package com.example.aitourism.service;

import com.example.aitourism.dto.preferences.VisitorPreferencesRequest;
import com.example.aitourism.dto.preferences.VisitorPreferencesResponse;

public interface PreferencesService {
    VisitorPreferencesResponse getCurrent();
    VisitorPreferencesResponse save(VisitorPreferencesRequest request);
    VisitorPreferencesResponse reset();
}
