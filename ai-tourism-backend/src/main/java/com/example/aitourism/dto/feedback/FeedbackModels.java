package com.example.aitourism.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class FeedbackModels {
    private FeedbackModels() {
    }

    public record CreateRequest(
            @NotBlank String category,
            @NotBlank @Size(max = 1000) String content,
            String tripId, Integer tripVersion, String poiCode, String eventId) {
    }

    public record UpdateRequest(@NotBlank String status, @Size(max = 1000) String resolution) {
    }

    public record Response(
            String feedbackId, String category, String content, String status,
            String tripId, Integer tripVersion, String poiCode, String eventId,
            String resolution, Instant createdAt, Instant updatedAt) {
    }
}
