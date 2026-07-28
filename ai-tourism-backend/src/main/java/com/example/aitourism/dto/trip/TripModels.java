package com.example.aitourism.dto.trip;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class TripModels {
    private TripModels() {
    }

    public record CreateRequest(
            Long startPoiId,
            @DecimalMin("113.4") @DecimalMax("113.7") BigDecimal startLongitude,
            @DecimalMin("22.0") @DecimalMax("22.3") BigDecimal startLatitude,
            @Size(max = 80) String startName,
            Map<Long, String> requiredArrivalTimes,
            Boolean simulateMapFailure) {
    }

    public record Stop(
            Long poiId, String poiCode, String name, String category, String region,
            BigDecimal longitude, BigDecimal latitude, String arrivalTime, String departureTime,
            int stayMinutes, int safetyBufferMinutes, boolean mustVisit, String status,
            String sourceOrganization, String sourceUrl, LocalDate sourcePublishedAt, LocalDate validUntil) {
    }

    public record Leg(
            int sequence, String fromName, String toName, String mode,
            int distanceMeters, int walkingMeters, int durationMinutes,
            BigDecimal estimatedCostMop, String instruction, boolean staticFallback) {
    }

    public record TransportOption(
            String mode, int durationMinutes, int walkingMeters,
            int effortScore, BigDecimal estimatedCostMop, boolean feasible, List<String> conflicts) {
    }

    public record Recommendation(
            Long poiId, String poiCode, String name, String category,
            BigDecimal longitude, BigDecimal latitude, String reason,
            int detourMeters, int detourMinutes, String openingHours,
            String accessibilityLevel, boolean naturalMerchant, double score) {
    }

    public record Comparison(
            int durationDeltaMinutes, int walkingDeltaMeters, int detourMeters,
            Map<String, String> arrivalChanges) {
    }

    public record Response(
            String tripId, int version, String status, boolean feasible, List<String> conflicts, List<String> warnings,
            String departureTime, String estimatedEndTime, int totalDurationMinutes,
            int totalWalkingMeters, int totalDistanceMeters, int safetyBufferMinutes,
            List<Stop> stops, List<Leg> legs, List<TransportOption> transportOptions,
            List<Recommendation> recommendations, Comparison comparison,
            boolean staticFallback, String fallbackMessage, String createdAt) {
    }
}
