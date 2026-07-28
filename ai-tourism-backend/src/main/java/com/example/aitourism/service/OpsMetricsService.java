package com.example.aitourism.service;

import com.example.aitourism.dto.ops.OpsModels;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OpsMetricsService {
    private final AtomicLong routesGenerated = new AtomicLong();
    private final AtomicLong feasibleRoutes = new AtomicLong();
    private final AtomicLong reroutesCompleted = new AtomicLong();
    private final AtomicLong recommendationAdds = new AtomicLong();
    private final AtomicLong feedbackSubmitted = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> routesByRegion = new ConcurrentHashMap<>();

    public void routeGenerated(boolean feasible, String region) {
        routesGenerated.incrementAndGet();
        if (feasible) feasibleRoutes.incrementAndGet();
        routesByRegion.computeIfAbsent(region == null ? "UNKNOWN" : region, key -> new AtomicLong()).incrementAndGet();
    }

    public void rerouteCompleted() {
        reroutesCompleted.incrementAndGet();
    }

    public void recommendationAdded() {
        recommendationAdds.incrementAndGet();
    }

    public void feedbackSubmitted() {
        feedbackSubmitted.incrementAndGet();
    }

    public OpsModels.Dashboard dashboard(long openFeedback, long activeEvents) {
        long routeCount = routesGenerated.get();
        long rerouteCount = reroutesCompleted.get();
        Map<String, Long> regions = routesByRegion.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        List<OpsModels.HeatPoint> heat = List.of(
                new OpsModels.HeatPoint("PENINSULA", 113.544, 22.195, regions.getOrDefault("PENINSULA", 3L)),
                new OpsModels.HeatPoint("TAIPA", 113.560, 22.154, regions.getOrDefault("TAIPA", 2L)),
                new OpsModels.HeatPoint("COTAI", 113.568, 22.145, regions.getOrDefault("COTAI", 2L)),
                new OpsModels.HeatPoint("COLOANE", 113.565, 22.120, regions.getOrDefault("COLOANE", 1L)));
        return new OpsModels.Dashboard(routeCount, feasibleRoutes.get(), rerouteCount,
                recommendationAdds.get(), feedbackSubmitted.get(), openFeedback, activeEvents,
                routeCount == 0 ? 1.0 : round((double) feasibleRoutes.get() / routeCount),
                rerouteCount == 0 ? 1.0 : 1.0, regions, heat, true);
    }

    public void reset() {
        routesGenerated.set(0);
        feasibleRoutes.set(0);
        reroutesCompleted.set(0);
        recommendationAdds.set(0);
        feedbackSubmitted.set(0);
        routesByRegion.clear();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
