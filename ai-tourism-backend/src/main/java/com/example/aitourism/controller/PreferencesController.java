package com.example.aitourism.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.dto.preferences.VisitorPreferencesRequest;
import com.example.aitourism.dto.preferences.VisitorPreferencesResponse;
import com.example.aitourism.service.PreferencesService;
import com.example.aitourism.util.Constants;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
@SaCheckLogin
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public BaseResponse<VisitorPreferencesResponse> getCurrent() {
        return BaseResponse.success(preferencesService.getCurrent());
    }

    @PutMapping
    public BaseResponse<VisitorPreferencesResponse> save(
            @Valid @RequestBody VisitorPreferencesRequest request) {
        try {
            return BaseResponse.success(preferencesService.save(request));
        } catch (IllegalArgumentException exception) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, exception.getMessage());
        }
    }

    @DeleteMapping
    public BaseResponse<VisitorPreferencesResponse> reset() {
        return BaseResponse.success(preferencesService.reset());
    }
}
