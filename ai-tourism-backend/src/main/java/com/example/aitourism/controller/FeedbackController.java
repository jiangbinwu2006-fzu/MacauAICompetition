package com.example.aitourism.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.dto.feedback.FeedbackModels;
import com.example.aitourism.service.FeedbackService;
import com.example.aitourism.util.Constants;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@SaCheckLogin
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public BaseResponse<FeedbackModels.Response> create(@Valid @RequestBody FeedbackModels.CreateRequest request) {
        try {
            return BaseResponse.success(feedbackService.create(request));
        } catch (IllegalArgumentException exception) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, exception.getMessage());
        }
    }
}
