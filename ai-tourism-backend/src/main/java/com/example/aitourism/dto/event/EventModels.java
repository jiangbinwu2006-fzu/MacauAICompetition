package com.example.aitourism.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class EventModels {
    private EventModels() {
    }

    public record UpsertRequest(
            @NotBlank String type,
            @NotBlank String severity,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 500) String description,
            @NotBlank String region,
            @NotEmpty List<String> affectedPoiCodes,
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T.*Z$") String startsAt,
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T.*Z$") String endsAt,
            boolean simulated) {
    }

    public record Response(
            String eventId, int version, String type, String severity, String title,
            String description, String region, List<String> affectedPoiCodes,
            Instant startsAt, Instant endsAt, String status, boolean simulated,
            Instant createdAt, Instant updatedAt) {
    }
}
