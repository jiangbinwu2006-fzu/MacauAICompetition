package com.example.aitourism.service;

import com.example.aitourism.dto.trip.TripModels;

public interface TripService {
    TripModels.Response create(TripModels.CreateRequest request);
    TripModels.Response createDemo(String preset);
    TripModels.Response current();
    TripModels.Response addRecommendation(String tripId, String poiCode);
    TripModels.Response ignoreRecommendation(String tripId, String poiCode);
    TripModels.Response reroute(String tripId, String mode);
    TripModels.Response undo(String tripId);
    TripModels.Response restore(String tripId);
    void resetCurrent();
}
