package com.example.aitourism.service.impl;

import com.example.aitourism.dto.event.EventModels;
import com.example.aitourism.service.EventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventServiceImpl implements EventService {
    private static final List<String> TYPES = List.of("ROAD_CLOSURE", "HEAVY_RAIN", "VENUE_CLOSED");
    private static final List<String> SEVERITIES = List.of("INFO", "MODERATE", "HIGH");
    private final Map<String, EventModels.Response> events = new ConcurrentHashMap<>();
    private final Sinks.Many<EventModels.Response> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public EventModels.Response create(EventModels.UpsertRequest request) {
        validate(request);
        Instant now = Instant.now();
        EventModels.Response event = toResponse("EVT-" + UUID.randomUUID().toString().substring(0, 8), 1,
                request, "ACTIVE", now, now);
        events.put(event.eventId(), event);
        sink.tryEmitNext(event);
        return event;
    }

    @Override
    public EventModels.Response update(String eventId, EventModels.UpsertRequest request) {
        validate(request);
        EventModels.Response current = require(eventId);
        EventModels.Response updated = toResponse(eventId, current.version() + 1, request,
                current.status(), current.createdAt(), Instant.now());
        events.put(eventId, updated);
        sink.tryEmitNext(updated);
        return updated;
    }

    @Override
    public EventModels.Response changeStatus(String eventId, String status) {
        EventModels.Response current = require(eventId);
        String normalized = status.toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "CANCELLED", "EXPIRED").contains(normalized)) {
            throw new IllegalArgumentException("不支持的事件状态");
        }
        EventModels.Response updated = new EventModels.Response(current.eventId(), current.version() + 1,
                current.type(), current.severity(), current.title(), current.description(), current.region(),
                current.affectedPoiCodes(), current.startsAt(), current.endsAt(), normalized,
                current.simulated(), current.createdAt(), Instant.now());
        events.put(eventId, updated);
        sink.tryEmitNext(updated);
        return updated;
    }

    @Override
    public List<EventModels.Response> listAll() {
        expirePastEvents();
        return events.values().stream().sorted(Comparator.comparing(EventModels.Response::createdAt).reversed()).toList();
    }

    @Override
    public List<EventModels.Response> listActive() {
        expirePastEvents();
        Instant now = Instant.now();
        return events.values().stream()
                .filter(event -> "ACTIVE".equals(event.status()))
                .filter(event -> !event.startsAt().isAfter(now) && event.endsAt().isAfter(now))
                .sorted(Comparator.comparing(EventModels.Response::severity).reversed())
                .toList();
    }

    @Override
    public Flux<EventModels.Response> stream() {
        return sink.asFlux();
    }

    @Override
    public void reset() {
        new ArrayList<>(events.keySet()).forEach(id -> changeStatus(id, "CANCELLED"));
        events.clear();
    }

    private EventModels.Response toResponse(String id, int version, EventModels.UpsertRequest request,
                                            String status, Instant createdAt, Instant updatedAt) {
        Instant starts = request.startsAt() == null ? Instant.now() : Instant.parse(request.startsAt());
        Instant ends = request.endsAt() == null ? starts.plus(4, ChronoUnit.HOURS) : Instant.parse(request.endsAt());
        if (!ends.isAfter(starts)) throw new IllegalArgumentException("事件结束时间必须晚于开始时间");
        return new EventModels.Response(id, version, request.type().toUpperCase(Locale.ROOT),
                request.severity().toUpperCase(Locale.ROOT), request.title(), request.description(),
                request.region().toUpperCase(Locale.ROOT), List.copyOf(request.affectedPoiCodes()),
                starts, ends, status, request.simulated(), createdAt, updatedAt);
    }

    private void validate(EventModels.UpsertRequest request) {
        if (!TYPES.contains(request.type().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("不支持的事件类型");
        if (!SEVERITIES.contains(request.severity().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("不支持的事件等级");
    }

    private EventModels.Response require(String id) {
        EventModels.Response event = events.get(id);
        if (event == null) throw new IllegalArgumentException("事件不存在");
        return event;
    }

    private void expirePastEvents() {
        Instant now = Instant.now();
        events.replaceAll((id, event) -> "ACTIVE".equals(event.status()) && !event.endsAt().isAfter(now)
                ? new EventModels.Response(event.eventId(), event.version() + 1, event.type(), event.severity(),
                event.title(), event.description(), event.region(), event.affectedPoiCodes(), event.startsAt(),
                event.endsAt(), "EXPIRED", event.simulated(), event.createdAt(), now)
                : event);
    }
}
