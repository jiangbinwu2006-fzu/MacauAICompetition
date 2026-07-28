package com.example.aitourism.controller;

import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.dto.event.EventModels;
import com.example.aitourism.dto.feedback.FeedbackModels;
import com.example.aitourism.dto.ops.OpsModels;
import com.example.aitourism.service.EventService;
import com.example.aitourism.service.FeedbackService;
import com.example.aitourism.service.OpsMetricsService;
import com.example.aitourism.service.TripService;
import com.example.aitourism.util.Constants;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ops")
public class OpsController {
    private final EventService eventService;
    private final FeedbackService feedbackService;
    private final OpsMetricsService metrics;
    private final TripService tripService;

    public OpsController(EventService eventService, FeedbackService feedbackService,
                         OpsMetricsService metrics, TripService tripService) {
        this.eventService = eventService;
        this.feedbackService = feedbackService;
        this.metrics = metrics;
        this.tripService = tripService;
    }

    @GetMapping("/dashboard")
    public BaseResponse<OpsModels.Dashboard> dashboard() {
        return BaseResponse.success(metrics.dashboard(feedbackService.openCount(), eventService.listActive().size()));
    }

    @GetMapping("/events")
    public BaseResponse<List<EventModels.Response>> events() {
        return BaseResponse.success(eventService.listAll());
    }

    @PostMapping("/events")
    public BaseResponse<EventModels.Response> createEvent(@Valid @RequestBody EventModels.UpsertRequest request) {
        return eventAction(() -> eventService.create(request));
    }

    @PutMapping("/events/{eventId}")
    public BaseResponse<EventModels.Response> updateEvent(@PathVariable String eventId,
                                                           @Valid @RequestBody EventModels.UpsertRequest request) {
        return eventAction(() -> eventService.update(eventId, request));
    }

    @PatchMapping("/events/{eventId}/status")
    public BaseResponse<EventModels.Response> eventStatus(@PathVariable String eventId, @RequestParam String status) {
        return eventAction(() -> eventService.changeStatus(eventId, status));
    }

    @GetMapping("/feedback")
    public BaseResponse<List<FeedbackModels.Response>> feedback() {
        return BaseResponse.success(feedbackService.list());
    }

    @PatchMapping("/feedback/{feedbackId}")
    public BaseResponse<FeedbackModels.Response> updateFeedback(@PathVariable String feedbackId,
                                                                 @Valid @RequestBody FeedbackModels.UpdateRequest request) {
        try {
            return BaseResponse.success(feedbackService.update(feedbackId, request));
        } catch (IllegalArgumentException exception) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping("/demo/road-closure")
    public BaseResponse<EventModels.Response> demoRoadClosure(@RequestParam(defaultValue = "P004") String poiCode) {
        Instant now = Instant.now();
        EventModels.UpsertRequest request = new EventModels.UpsertRequest("ROAD_CLOSURE", "MODERATE",
                "模拟封路：大炮台周边", "测试环境注入的封路事件，用于验证实时提醒和局部改线。",
                "PENINSULA", List.of(poiCode), now.minus(1, ChronoUnit.MINUTES).toString(),
                now.plus(2, ChronoUnit.HOURS).toString(), true);
        return BaseResponse.success(eventService.create(request));
    }

    @PostMapping("/demo/reset")
    public BaseResponse<Map<String, String>> resetDemo() {
        eventService.reset();
        feedbackService.reset();
        metrics.reset();
        try {
            tripService.resetCurrent();
        } catch (Exception ignored) {
            // Demo reset is also available before a visitor session exists.
        }
        return BaseResponse.success(Map.of("status", "reset"));
    }

    private BaseResponse<EventModels.Response> eventAction(EventAction action) {
        try {
            return BaseResponse.success(action.run());
        } catch (IllegalArgumentException exception) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface EventAction {
        EventModels.Response run();
    }
}
