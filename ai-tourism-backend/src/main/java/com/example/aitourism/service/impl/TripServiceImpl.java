package com.example.aitourism.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.aitourism.dto.event.EventModels;
import com.example.aitourism.dto.preferences.VisitorPreferencesResponse;
import com.example.aitourism.dto.trip.TripModels;
import com.example.aitourism.entity.CatalogPoi;
import com.example.aitourism.service.CatalogService;
import com.example.aitourism.service.EventService;
import com.example.aitourism.service.OpsMetricsService;
import com.example.aitourism.service.PreferencesService;
import com.example.aitourism.service.TripService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TripServiceImpl implements TripService {
    private static final String SESSION_KEY = "macau.current.trip";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final CatalogService catalogService;
    private final PreferencesService preferencesService;
    private final EventService eventService;
    private final OpsMetricsService metrics;

    public TripServiceImpl(CatalogService catalogService, PreferencesService preferencesService,
                           EventService eventService, OpsMetricsService metrics) {
        this.catalogService = catalogService;
        this.preferencesService = preferencesService;
        this.eventService = eventService;
        this.metrics = metrics;
    }

    @Override
    public TripModels.Response create(TripModels.CreateRequest request) {
        VisitorPreferencesResponse preferences = preferencesService.getCurrent();
        SessionTrip trip = new SessionTrip("TRIP-" + UUID.randomUUID().toString().substring(0, 8),
                request == null ? new TripModels.CreateRequest(null, null, null, null, Map.of(), false) : request,
                preferences);
        TripModels.Response response = solve(trip, null, "INITIAL", null, false);
        trip.current = response;
        trip.original = response;
        StpUtil.getSession().set(SESSION_KEY, trip);
        String region = response.stops().isEmpty() ? "UNKNOWN" : response.stops().get(0).region();
        metrics.routeGenerated(response.feasible(), region);
        return response;
    }

    @Override
    public TripModels.Response current() {
        SessionTrip trip = sessionTrip();
        return trip == null ? null : trip.current;
    }

    @Override
    public TripModels.Response addRecommendation(String tripId, String poiCode) {
        SessionTrip trip = require(tripId);
        CatalogPoi recommendation = catalogService.findActiveByCode(poiCode)
                .orElseThrow(() -> new IllegalArgumentException("推荐点位不存在或已过期"));
        List<Long> ids = trip.current.stops().stream().map(TripModels.Stop::poiId).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (!ids.contains(recommendation.getId())) ids.add(recommendation.getId());
        pushHistory(trip);
        TripModels.Response updated = solve(trip, ids, "RECOMMENDATION_ADDED", trip.current, false);
        if (!updated.feasible()) {
            trip.history.pop();
            throw new IllegalArgumentException(String.join("；", updated.conflicts()));
        }
        trip.current = updated;
        metrics.recommendationAdded();
        return updated;
    }

    @Override
    public TripModels.Response ignoreRecommendation(String tripId, String poiCode) {
        SessionTrip trip = require(tripId);
        trip.ignoredPoiCodes.add(poiCode);
        trip.current = copyWithRecommendations(trip.current, trip.current.recommendations().stream()
                .filter(item -> !item.poiCode().equalsIgnoreCase(poiCode)).toList());
        return trip.current;
    }

    @Override
    public TripModels.Response reroute(String tripId, String mode) {
        SessionTrip trip = require(tripId);
        trip.preferences = preferencesService.getCurrent();
        String normalized = mode == null ? "LOCAL" : mode.toUpperCase(Locale.ROOT);
        if (!List.of("LOCAL", "GLOBAL").contains(normalized)) throw new IllegalArgumentException("改线模式必须为 LOCAL 或 GLOBAL");
        Set<String> blocked = activeBlockedCodes();
        boolean currentTripAffected = trip.current.stops().stream().anyMatch(stop -> blocked.contains(stop.poiCode()));
        if (!currentTripAffected) throw new IllegalArgumentException("当前行程没有受生效事件影响，无需重新改线");
        List<Long> desired;
        if ("GLOBAL".equals(normalized)) {
            desired = null;
        } else {
            List<CatalogPoi> active = catalogService.listActiveEntities();
            Map<Long, CatalogPoi> activeById = active.stream().collect(java.util.stream.Collectors.toMap(CatalogPoi::getId, poi -> poi));
            Set<Long> used = new HashSet<>();
            Set<Long> reserved = trip.current.stops().stream()
                    .filter(stop -> !blocked.contains(stop.poiCode()))
                    .map(TripModels.Stop::poiId).collect(java.util.stream.Collectors.toSet());
            desired = new ArrayList<>();
            for (int index = 0; index < trip.current.stops().size(); index++) {
                TripModels.Stop stop = trip.current.stops().get(index);
                if (!blocked.contains(stop.poiCode())) {
                    desired.add(stop.poiId());
                    used.add(stop.poiId());
                    continue;
                }
                CatalogPoi previous = desired.isEmpty() ? null : activeById.get(desired.get(desired.size() - 1));
                TripModels.Stop next = nextUnaffectedStop(trip.current.stops(), index + 1, blocked);
                Comparator<CatalogPoi> replacementOrder = Comparator
                        .comparingDouble((CatalogPoi poi) -> replacementScore(trip, previous, next, stop, poi))
                        .thenComparing(CatalogPoi::getPoiCode);
                Optional<CatalogPoi> replacement = active.stream()
                        .filter(poi -> poi.getCategory().equals(stop.category()))
                        .filter(poi -> !blocked.contains(poi.getPoiCode()) && !used.contains(poi.getId()) && !reserved.contains(poi.getId()))
                        .filter(poi -> accessible(poi, trip.preferences))
                        .min(replacementOrder);
                if (replacement.isEmpty()) {
                    replacement = active.stream()
                            .filter(poi -> trip.preferences.getInterests().contains(poi.getCategory()))
                            .filter(poi -> !blocked.contains(poi.getPoiCode()) && !used.contains(poi.getId()) && !reserved.contains(poi.getId()))
                            .filter(poi -> accessible(poi, trip.preferences))
                            .min(replacementOrder);
                }
                replacement.ifPresent(poi -> {
                    desired.add(poi.getId());
                    used.add(poi.getId());
                });
            }
        }
        pushHistory(trip);
        TripModels.Response updated = solve(trip, desired, normalized + "_REROUTE", trip.current, true);
        if (!updated.feasible()) {
            trip.history.pop();
            throw new IllegalArgumentException("改线未能满足硬约束：" + String.join("；", updated.conflicts()));
        }
        trip.current = updated;
        metrics.rerouteCompleted();
        return updated;
    }

    @Override
    public TripModels.Response undo(String tripId) {
        SessionTrip trip = require(tripId);
        if (trip.history.isEmpty()) throw new IllegalArgumentException("没有可撤回的行程版本");
        TripModels.Response previous = trip.history.pop();
        trip.current = withVersion(previous, trip.current.version() + 1, "UNDO_RESTORED");
        return trip.current;
    }

    @Override
    public TripModels.Response restore(String tripId) {
        SessionTrip trip = require(tripId);
        pushHistory(trip);
        trip.current = withVersion(trip.original, trip.current.version() + 1, "ORIGINAL_RESTORED");
        return trip.current;
    }

    @Override
    public void resetCurrent() {
        StpUtil.getSession().delete(SESSION_KEY);
    }

    private TripModels.Response solve(SessionTrip trip, List<Long> desiredIds, String status,
                                      TripModels.Response previous, boolean excludeEvents) {
        List<CatalogPoi> all = catalogService.listActiveEntities();
        Map<Long, CatalogPoi> byId = all.stream().collect(java.util.stream.Collectors.toMap(CatalogPoi::getId, poi -> poi));
        Set<String> blocked = excludeEvents ? activeBlockedCodes() : Set.of();
        List<String> conflicts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<CatalogPoi> selected = desiredIds == null
                ? initialSelection(trip, all, blocked)
                : desiredIds.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .filter(poi -> !blocked.contains(poi.getPoiCode())).distinct().toList();

        for (Long mustId : trip.preferences.getMustVisitPoiIds()) {
            CatalogPoi must = byId.get(mustId);
            if (must == null) conflicts.add("必去点 " + mustId + " 已过期或不可用");
            else if (blocked.contains(must.getPoiCode())) warnings.add("必去点 " + name(must, trip.preferences.getLanguage()) + " 受当前事件影响，已为本次安全改线临时移除");
            else if (selected.stream().noneMatch(poi -> poi.getId().equals(mustId))) conflicts.add("缺少必去点 " + name(must, trip.preferences.getLanguage()));
        }
        if (selected.isEmpty()) conflicts.add("没有满足条件的有效点位");

        List<CatalogPoi> ordered = desiredIds == null ? nearestOrder(trip, selected) : new ArrayList<>(selected);
        List<String> calculationConflicts = new ArrayList<>();
        RouteCalculation calculation = calculate(trip, ordered, trip.preferences.getTransportPreference(), calculationConflicts);
        if (desiredIds == null) {
            int minimumStops = Math.max(1, trip.preferences.getMustVisitPoiIds().size());
            while (!calculationConflicts.isEmpty() && ordered.size() > minimumStops) {
                int removable = lastOptionalStop(ordered, trip.preferences.getMustVisitPoiIds());
                if (removable < 0) break;
                ordered.remove(removable);
                calculationConflicts.clear();
                calculation = calculate(trip, ordered, trip.preferences.getTransportPreference(), calculationConflicts);
            }
        }
        conflicts.addAll(calculationConflicts);
        List<TripModels.TransportOption> options = List.of("WALK", "PUBLIC_TRANSIT", "MIXED").stream()
                .map(mode -> transportOption(trip, ordered, mode)).toList();
        boolean feasible = conflicts.isEmpty();
        int version = previous == null ? 1 : previous.version() + 1;
        TripModels.Comparison comparison = previous == null ? null : compare(previous, calculation);
        List<TripModels.Recommendation> recommendations = feasible
                ? recommendations(trip, all, ordered, calculation, blocked)
                : List.of();
        boolean fallback = Boolean.TRUE.equals(trip.request.simulateMapFailure());
        return new TripModels.Response(trip.tripId, version, feasible ? status : "CONFLICT", feasible,
                List.copyOf(conflicts), List.copyOf(warnings), trip.preferences.getDepartureTime(), calculation.end.format(TIME),
                calculation.durationMinutes, calculation.walkingMeters, calculation.distanceMeters,
                calculation.bufferMinutes, calculation.stops, calculation.legs, options, recommendations,
                comparison, fallback, fallback ? "地图服务不可用，已切换为静态距离和纯文字路线" : null,
                Instant.now().toString());
    }

    private List<CatalogPoi> initialSelection(SessionTrip trip, List<CatalogPoi> all, Set<String> blocked) {
        LinkedHashSet<CatalogPoi> selected = new LinkedHashSet<>();
        int targetCount = targetStopCount(trip.preferences);
        CatalogPoi start = startPoi(trip, all);
        if (start != null && !blocked.contains(start.getPoiCode())) selected.add(start);
        for (Long id : trip.preferences.getMustVisitPoiIds()) {
            all.stream().filter(poi -> poi.getId().equals(id)).findFirst()
                    .filter(poi -> !blocked.contains(poi.getPoiCode())).ifPresent(selected::add);
        }
        String region = start == null ? null : start.getRegion();
        all.stream()
                .filter(poi -> !blocked.contains(poi.getPoiCode()))
                .filter(poi -> region == null || region.equals(poi.getRegion()))
                .filter(poi -> trip.preferences.getInterests().contains(poi.getCategory()))
                .filter(poi -> accessible(poi, trip.preferences))
                .sorted(Comparator.comparingDouble(poi -> start == null ? 0 : distance(start, poi)))
                .limit(Math.max(8, targetCount * 2L)).forEach(poi -> { if (selected.size() < targetCount) selected.add(poi); });
        all.stream().filter(poi -> !blocked.contains(poi.getPoiCode())).filter(poi -> accessible(poi, trip.preferences))
                .sorted(Comparator.comparingDouble(poi -> start == null ? 0 : distance(start, poi)))
                .forEach(poi -> { if (selected.size() < targetCount) selected.add(poi); });
        return new ArrayList<>(selected);
    }

    private List<CatalogPoi> nearestOrder(SessionTrip trip, List<CatalogPoi> selected) {
        if (selected.size() < 2) return new ArrayList<>(selected);
        List<CatalogPoi> remaining = new ArrayList<>(selected);
        CatalogPoi current = startPoi(trip, selected);
        if (current == null || !remaining.remove(current)) current = remaining.remove(0);
        List<CatalogPoi> ordered = new ArrayList<>();
        ordered.add(current);
        while (!remaining.isEmpty()) {
            CatalogPoi from = current;
            current = remaining.stream().min(Comparator.comparingDouble(poi -> distance(from, poi))).orElseThrow();
            remaining.remove(current);
            ordered.add(current);
        }
        return ordered;
    }

    private int targetStopCount(VisitorPreferencesResponse preferences) {
        long windowMinutes = java.time.Duration.between(
                LocalTime.parse(preferences.getDepartureTime()),
                LocalTime.parse(preferences.getLatestEndTime())).toMinutes();
        int timeBased = (int) Math.max(1, windowMinutes / 70);
        int required = preferences.getMustVisitPoiIds().size();
        return Math.min(8, Math.max(required, timeBased));
    }

    private int lastOptionalStop(List<CatalogPoi> ordered, List<Long> mustVisitIds) {
        for (int index = ordered.size() - 1; index >= 0; index--) {
            if (!mustVisitIds.contains(ordered.get(index).getId())) return index;
        }
        return -1;
    }

    private TripModels.Stop nextUnaffectedStop(List<TripModels.Stop> stops, int fromIndex, Set<String> blocked) {
        for (int index = fromIndex; index < stops.size(); index++) {
            if (!blocked.contains(stops.get(index).poiCode())) return stops.get(index);
        }
        return null;
    }

    private double replacementScore(SessionTrip trip, CatalogPoi previous, TripModels.Stop next,
                                    TripModels.Stop replaced, CatalogPoi candidate) {
        double previousLat = previous == null ? startLatitude(trip, List.of()) : previous.getLatitude().doubleValue();
        double previousLng = previous == null ? startLongitude(trip, List.of()) : previous.getLongitude().doubleValue();
        double score = distance(previousLat, previousLng,
                candidate.getLatitude().doubleValue(), candidate.getLongitude().doubleValue());
        if (next != null) {
            score += distance(candidate.getLatitude().doubleValue(), candidate.getLongitude().doubleValue(),
                    next.latitude().doubleValue(), next.longitude().doubleValue());
        }
        if (!candidate.getRegion().equals(replaced.region())) score += 1500;
        if (!trip.preferences.getInterests().contains(candidate.getCategory())) score += 3000;
        return score;
    }

    private RouteCalculation calculate(SessionTrip trip, List<CatalogPoi> ordered, String mode, List<String> conflicts) {
        LocalTime departure = LocalTime.parse(trip.preferences.getDepartureTime());
        LocalTime latest = LocalTime.parse(trip.preferences.getLatestEndTime());
        LocalTime cursor = departure;
        double currentLat = startLatitude(trip, ordered);
        double currentLng = startLongitude(trip, ordered);
        String currentName = startName(trip);
        int totalWalking = 0;
        int totalDistance = 0;
        int totalBuffer = 0;
        List<TripModels.Stop> stops = new ArrayList<>();
        List<TripModels.Leg> legs = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            CatalogPoi poi = ordered.get(index);
            int meters = (int) Math.round(distance(currentLat, currentLng, poi.getLatitude().doubleValue(), poi.getLongitude().doubleValue()));
            LegMetric metric = legMetric(meters, mode);
            cursor = cursor.plusMinutes(metric.minutes);
            String required = trip.request.requiredArrivalTimes() == null ? null : trip.request.requiredArrivalTimes().get(poi.getId());
            if (required != null && !required.isBlank()) {
                LocalTime requiredTime = LocalTime.parse(required);
                if (cursor.isAfter(requiredTime)) conflicts.add(name(poi, trip.preferences.getLanguage()) + " 无法在 " + required + " 前到达");
                else cursor = requiredTime;
            }
            int stay = stayMinutes(poi);
            int buffer = 10;
            LocalTime arrival = cursor;
            cursor = cursor.plusMinutes(stay + buffer);
            boolean must = trip.preferences.getMustVisitPoiIds().contains(poi.getId());
            if (!accessible(poi, trip.preferences) && must) conflicts.add("必去点 " + name(poi, trip.preferences.getLanguage()) + " 不满足无障碍要求");
            stops.add(new TripModels.Stop(poi.getId(), poi.getPoiCode(), name(poi, trip.preferences.getLanguage()),
                    poi.getCategory(), poi.getRegion(), poi.getLongitude(), poi.getLatitude(), arrival.format(TIME),
                    cursor.format(TIME), stay, buffer, must, "SCHEDULED", poi.getSourceOrganization(),
                    poi.getSourceUrl(), poi.getSourcePublishedAt(), poi.getValidUntil()));
            legs.add(new TripModels.Leg(index + 1, currentName, name(poi, trip.preferences.getLanguage()),
                    metric.mode, meters, metric.walking, metric.minutes, metric.cost,
                    instruction(metric.mode, name(poi, trip.preferences.getLanguage())),
                    Boolean.TRUE.equals(trip.request.simulateMapFailure())));
            totalWalking += metric.walking;
            totalDistance += meters;
            totalBuffer += buffer;
            currentLat = poi.getLatitude().doubleValue();
            currentLng = poi.getLongitude().doubleValue();
            currentName = name(poi, trip.preferences.getLanguage());
        }
        if (totalWalking > trip.preferences.getMaxWalkingMeters()) {
            conflicts.add("预计步行 " + totalWalking + " 米，超过上限 " + trip.preferences.getMaxWalkingMeters() + " 米");
        }
        if (cursor.isAfter(latest)) conflicts.add("预计结束时间 " + cursor.format(TIME) + " 晚于最晚结束时间 " + latest.format(TIME));
        int duration = (int) java.time.Duration.between(departure, cursor).toMinutes();
        return new RouteCalculation(stops, legs, cursor, duration, totalWalking, totalDistance, totalBuffer);
    }

    private TripModels.TransportOption transportOption(SessionTrip trip, List<CatalogPoi> ordered, String mode) {
        List<String> conflicts = new ArrayList<>();
        RouteCalculation calculation = calculate(trip, ordered, mode, conflicts);
        BigDecimal cost = calculation.legs.stream().map(TripModels.Leg::estimatedCostMop).reduce(BigDecimal.ZERO, BigDecimal::add);
        int effort = Math.min(100, (int) Math.round(calculation.walkingMeters / 80.0));
        return new TripModels.TransportOption(mode, calculation.durationMinutes, calculation.walkingMeters,
                effort, cost, conflicts.isEmpty(), List.copyOf(conflicts));
    }

    private List<TripModels.Recommendation> recommendations(SessionTrip trip, List<CatalogPoi> all,
                                                            List<CatalogPoi> selected, RouteCalculation calculation,
                                                            Set<String> blocked) {
        Set<Long> used = selected.stream().map(CatalogPoi::getId).collect(java.util.stream.Collectors.toSet());
        LocalTime latest = LocalTime.parse(trip.preferences.getLatestEndTime());
        return all.stream()
                .filter(poi -> !used.contains(poi.getId()) && !trip.ignoredPoiCodes.contains(poi.getPoiCode()))
                .filter(poi -> !blocked.contains(poi.getPoiCode()))
                .filter(poi -> accessible(poi, trip.preferences))
                .map(poi -> recommendation(trip, poi, selected))
                .filter(item -> calculation.walkingMeters + item.detourMeters() <= trip.preferences.getMaxWalkingMeters())
                .filter(item -> !calculation.end.plusMinutes(item.detourMinutes()).isAfter(latest))
                .sorted(Comparator.comparingDouble(TripModels.Recommendation::score).reversed())
                .limit(5).toList();
    }

    private TripModels.Recommendation recommendation(SessionTrip trip, CatalogPoi poi, List<CatalogPoi> selected) {
        double nearest = selected.stream().mapToDouble(stop -> distance(stop, poi)).min().orElse(0);
        int detour = (int) Math.round(nearest * 2);
        int minutes = Math.max(5, detour / 70) + stayMinutes(poi) + 10;
        boolean interest = trip.preferences.getInterests().contains(poi.getCategory());
        double score = (interest ? 40 : 10) + ("FULL".equals(poi.getAccessibilityLevel()) ? 12 : 5) + Math.max(0, 45 - detour / 100.0);
        String reason = (interest ? "符合兴趣；" : "沿途补给；") + "新增绕行约 " + detour + " 米，不破坏当前硬约束";
        return new TripModels.Recommendation(poi.getId(), poi.getPoiCode(), name(poi, trip.preferences.getLanguage()),
                poi.getCategory(), poi.getLongitude(), poi.getLatitude(), reason, detour, minutes,
                poi.getOpeningHours(), poi.getAccessibilityLevel(), Boolean.TRUE.equals(poi.getNaturalMerchant()),
                Math.round(score * 10.0) / 10.0);
    }

    private TripModels.Comparison compare(TripModels.Response previous, RouteCalculation updated) {
        Map<String, String> oldArrivals = previous.stops().stream().collect(java.util.stream.Collectors.toMap(
                TripModels.Stop::poiCode, TripModels.Stop::arrivalTime, (first, ignored) -> first));
        Map<String, String> changes = new LinkedHashMap<>();
        for (TripModels.Stop stop : updated.stops) {
            String old = oldArrivals.get(stop.poiCode());
            if (old != null && !old.equals(stop.arrivalTime())) changes.put(stop.poiCode(), old + " -> " + stop.arrivalTime());
        }
        return new TripModels.Comparison(updated.durationMinutes - previous.totalDurationMinutes(),
                updated.walkingMeters - previous.totalWalkingMeters(),
                Math.max(0, updated.distanceMeters - previous.totalDistanceMeters()), changes);
    }

    private CatalogPoi startPoi(SessionTrip trip, List<CatalogPoi> all) {
        if (trip.request.startPoiId() != null) return all.stream().filter(poi -> poi.getId().equals(trip.request.startPoiId())).findFirst().orElse(null);
        if (trip.preferences.getCurrentLatitude() != null && trip.preferences.getCurrentLongitude() != null) {
            double latitude = trip.preferences.getCurrentLatitude().doubleValue();
            double longitude = trip.preferences.getCurrentLongitude().doubleValue();
            Comparator<CatalogPoi> proximity = Comparator.comparingDouble(poi -> distance(latitude, longitude,
                    poi.getLatitude().doubleValue(), poi.getLongitude().doubleValue()));
            Optional<CatalogPoi> relevant = all.stream()
                    .filter(poi -> trip.preferences.getInterests().contains(poi.getCategory()))
                    .filter(poi -> accessible(poi, trip.preferences))
                    .min(proximity);
            return relevant.orElseGet(() -> all.stream().min(proximity).orElse(null));
        }
        return all.stream().filter(poi -> "P002".equals(poi.getPoiCode())).findFirst().orElse(all.isEmpty() ? null : all.get(0));
    }

    private double startLatitude(SessionTrip trip, List<CatalogPoi> ordered) {
        if (trip.request.startLatitude() != null) return trip.request.startLatitude().doubleValue();
        if (trip.preferences.getCurrentLatitude() != null) return trip.preferences.getCurrentLatitude().doubleValue();
        return ordered.isEmpty() ? 22.1935 : ordered.get(0).getLatitude().doubleValue();
    }

    private double startLongitude(SessionTrip trip, List<CatalogPoi> ordered) {
        if (trip.request.startLongitude() != null) return trip.request.startLongitude().doubleValue();
        if (trip.preferences.getCurrentLongitude() != null) return trip.preferences.getCurrentLongitude().doubleValue();
        return ordered.isEmpty() ? 113.5399 : ordered.get(0).getLongitude().doubleValue();
    }

    private String startName(SessionTrip trip) {
        if (trip.request.startName() != null && !trip.request.startName().isBlank()) return trip.request.startName();
        if (trip.preferences.getCurrentLocationName() != null && !trip.preferences.getCurrentLocationName().isBlank()) {
            return trip.preferences.getCurrentLocationName();
        }
        return "行程起点";
    }

    private boolean accessible(CatalogPoi poi, VisitorPreferencesResponse preferences) {
        if (preferences.getAccessibilityNeeds().contains("STEP_FREE")) return "FULL".equals(poi.getAccessibilityLevel());
        if (preferences.getAccessibilityNeeds().contains("LOW_WALKING")) return !"LIMITED".equals(poi.getAccessibilityLevel());
        return true;
    }

    private int stayMinutes(CatalogPoi poi) {
        return switch (poi.getCategory()) {
            case "FOOD" -> 35;
            case "PUBLIC_SERVICE", "TRANSPORT" -> 20;
            default -> 45;
        };
    }

    private LegMetric legMetric(int distance, String requestedMode) {
        String mode = requestedMode == null ? "MIXED" : requestedMode;
        if ("WALK".equals(mode) || ("MIXED".equals(mode) && distance <= 900)) {
            return new LegMetric("WALK", distance, Math.max(1, (int) Math.ceil(distance / 72.0)), BigDecimal.ZERO);
        }
        int walking = Math.min(distance, 280);
        int minutes = 8 + Math.max(3, (int) Math.ceil(distance / 260.0)) + (int) Math.ceil(walking / 72.0);
        return new LegMetric("PUBLIC_TRANSIT", walking, minutes, new BigDecimal("6.00"));
    }

    private String instruction(String mode, String destination) {
        return "WALK".equals(mode) ? "步行前往 " + destination : "步行接驳并乘坐公共交通前往 " + destination;
    }

    private double distance(CatalogPoi a, CatalogPoi b) {
        return distance(a.getLatitude().doubleValue(), a.getLongitude().doubleValue(), b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double earth = 6371000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2) + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String name(CatalogPoi poi, String language) {
        return switch (language == null ? "zh-Hans" : language) {
            case "zh-Hant" -> fallback(poi.getNameZhHant(), poi.getNameZhHans());
            case "en" -> fallback(poi.getNameEn(), poi.getNameZhHant(), poi.getNameZhHans());
            case "pt" -> fallback(poi.getNamePt(), poi.getNameZhHant(), poi.getNameZhHans());
            default -> poi.getNameZhHans();
        };
    }

    private String fallback(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private Set<String> activeBlockedCodes() {
        return eventService.listActive().stream().flatMap(event -> event.affectedPoiCodes().stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    private SessionTrip sessionTrip() {
        Object value = StpUtil.getSession().get(SESSION_KEY);
        return value instanceof SessionTrip trip ? trip : null;
    }

    private SessionTrip require(String tripId) {
        SessionTrip trip = sessionTrip();
        if (trip == null || !trip.tripId.equals(tripId)) throw new IllegalArgumentException("当前会话中没有该行程");
        return trip;
    }

    private void pushHistory(SessionTrip trip) {
        trip.history.push(trip.current);
        while (trip.history.size() > 10) trip.history.removeLast();
    }

    private TripModels.Response copyWithRecommendations(TripModels.Response source, List<TripModels.Recommendation> recommendations) {
        return new TripModels.Response(source.tripId(), source.version(), source.status(), source.feasible(), source.conflicts(), source.warnings(),
                source.departureTime(), source.estimatedEndTime(), source.totalDurationMinutes(), source.totalWalkingMeters(),
                source.totalDistanceMeters(), source.safetyBufferMinutes(), source.stops(), source.legs(),
                source.transportOptions(), recommendations, source.comparison(), source.staticFallback(),
                source.fallbackMessage(), source.createdAt());
    }

    private TripModels.Response withVersion(TripModels.Response source, int version, String status) {
        return new TripModels.Response(source.tripId(), version, status, source.feasible(), source.conflicts(), source.warnings(),
                source.departureTime(), source.estimatedEndTime(), source.totalDurationMinutes(), source.totalWalkingMeters(),
                source.totalDistanceMeters(), source.safetyBufferMinutes(), source.stops(), source.legs(), source.transportOptions(),
                source.recommendations(), source.comparison(), source.staticFallback(), source.fallbackMessage(), Instant.now().toString());
    }

    private record LegMetric(String mode, int walking, int minutes, BigDecimal cost) {
    }

    private record RouteCalculation(List<TripModels.Stop> stops, List<TripModels.Leg> legs, LocalTime end,
                                    int durationMinutes, int walkingMeters, int distanceMeters, int bufferMinutes) {
    }

    private static final class SessionTrip {
        private final String tripId;
        private final TripModels.CreateRequest request;
        private VisitorPreferencesResponse preferences;
        private final Deque<TripModels.Response> history = new ArrayDeque<>();
        private final Set<String> ignoredPoiCodes = new HashSet<>();
        private TripModels.Response original;
        private TripModels.Response current;

        private SessionTrip(String tripId, TripModels.CreateRequest request, VisitorPreferencesResponse preferences) {
            this.tripId = tripId;
            this.request = request;
            this.preferences = preferences;
        }
    }
}
