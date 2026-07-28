package com.example.aitourism.service;

import com.example.aitourism.dto.feedback.FeedbackModels;

import java.util.List;

public interface FeedbackService {
    FeedbackModels.Response create(FeedbackModels.CreateRequest request);
    List<FeedbackModels.Response> list();
    FeedbackModels.Response update(String id, FeedbackModels.UpdateRequest request);
    long openCount();
    void reset();
}
