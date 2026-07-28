package com.example.aitourism.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.dto.trip.TripModels;
import com.example.aitourism.service.TripService;
import com.example.aitourism.util.Constants;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@SaCheckLogin
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public BaseResponse<TripModels.Response> create(@Valid @RequestBody(required = false) TripModels.CreateRequest request) {
        return execute(() -> tripService.create(request));
    }

    @GetMapping("/current")
    public BaseResponse<TripModels.Response> current() {
        return BaseResponse.success(tripService.current());
    }

    @PostMapping("/{tripId}/recommendations/{poiCode}")
    public BaseResponse<TripModels.Response> addRecommendation(@PathVariable String tripId, @PathVariable String poiCode) {
        return execute(() -> tripService.addRecommendation(tripId, poiCode));
    }

    @DeleteMapping("/{tripId}/recommendations/{poiCode}")
    public BaseResponse<TripModels.Response> ignoreRecommendation(@PathVariable String tripId, @PathVariable String poiCode) {
        return execute(() -> tripService.ignoreRecommendation(tripId, poiCode));
    }

    @PostMapping("/{tripId}/reroute")
    public BaseResponse<TripModels.Response> reroute(@PathVariable String tripId,
                                                     @RequestParam(defaultValue = "LOCAL") String mode) {
        return execute(() -> tripService.reroute(tripId, mode));
    }

    @PostMapping("/{tripId}/undo")
    public BaseResponse<TripModels.Response> undo(@PathVariable String tripId) {
        return execute(() -> tripService.undo(tripId));
    }

    @PostMapping("/{tripId}/restore")
    public BaseResponse<TripModels.Response> restore(@PathVariable String tripId) {
        return execute(() -> tripService.restore(tripId));
    }

    @DeleteMapping("/current")
    public BaseResponse<Void> reset() {
        tripService.resetCurrent();
        return BaseResponse.success();
    }

    private BaseResponse<TripModels.Response> execute(Action action) {
        try {
            return BaseResponse.success(action.run());
        } catch (IllegalArgumentException exception) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface Action {
        TripModels.Response run();
    }
}
