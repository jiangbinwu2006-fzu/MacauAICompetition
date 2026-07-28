package com.example.aitourism.service.impl;

import com.example.aitourism.dto.feedback.FeedbackModels;
import com.example.aitourism.service.FeedbackService;
import com.example.aitourism.service.OpsMetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeedbackServiceImpl implements FeedbackService {
    private static final List<String> CATEGORIES = List.of("DATA_ERROR", "ROUTE_ISSUE", "ACCESSIBILITY", "HELP");
    private final Map<String, FeedbackModels.Response> feedback = new ConcurrentHashMap<>();
    private final OpsMetricsService metrics;

    public FeedbackServiceImpl(OpsMetricsService metrics) {
        this.metrics = metrics;
    }

    @Override
    public FeedbackModels.Response create(FeedbackModels.CreateRequest request) {
        String category = request.category().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("不支持的反馈分类");
        Instant now = Instant.now();
        FeedbackModels.Response item = new FeedbackModels.Response(
                "FB-" + UUID.randomUUID().toString().substring(0, 8), category, request.content(), "OPEN",
                request.tripId(), request.tripVersion(), request.poiCode(), request.eventId(), null, now, now);
        feedback.put(item.feedbackId(), item);
        metrics.feedbackSubmitted();
        return item;
    }

    @Override
    public List<FeedbackModels.Response> list() {
        return feedback.values().stream().sorted(Comparator.comparing(FeedbackModels.Response::createdAt).reversed()).toList();
    }

    @Override
    public FeedbackModels.Response update(String id, FeedbackModels.UpdateRequest request) {
        FeedbackModels.Response current = feedback.get(id);
        if (current == null) throw new IllegalArgumentException("反馈工单不存在");
        String status = request.status().toUpperCase(Locale.ROOT);
        if (!List.of("OPEN", "PROCESSING", "CLOSED").contains(status)) throw new IllegalArgumentException("不支持的工单状态");
        FeedbackModels.Response updated = new FeedbackModels.Response(current.feedbackId(), current.category(),
                current.content(), status, current.tripId(), current.tripVersion(), current.poiCode(), current.eventId(),
                request.resolution(), current.createdAt(), Instant.now());
        feedback.put(id, updated);
        return updated;
    }

    @Override
    public long openCount() {
        return feedback.values().stream().filter(item -> !"CLOSED".equals(item.status())).count();
    }

    @Override
    public void reset() {
        feedback.clear();
    }
}
