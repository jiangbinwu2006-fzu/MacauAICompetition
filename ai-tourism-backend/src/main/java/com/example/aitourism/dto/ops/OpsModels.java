package com.example.aitourism.dto.ops;

import java.util.List;
import java.util.Map;

public final class OpsModels {
    private OpsModels() {
    }

    public record Dashboard(
            long routesGenerated, long feasibleRoutes, long reroutesCompleted,
            long recommendationAdds, long feedbackSubmitted, long openFeedback,
            long activeEvents, double onTimeRate, double rerouteSuccessRate,
            Map<String, Long> routesByRegion, List<HeatPoint> heatPoints,
            boolean simulatedData) {
    }

    public record HeatPoint(String region, double longitude, double latitude, long value) {
    }
}
