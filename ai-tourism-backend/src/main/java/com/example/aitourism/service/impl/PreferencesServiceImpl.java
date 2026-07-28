package com.example.aitourism.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.aitourism.dto.preferences.VisitorPreferencesRequest;
import com.example.aitourism.dto.preferences.VisitorPreferencesResponse;
import com.example.aitourism.service.PreferencesService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PreferencesServiceImpl implements PreferencesService {

    private static final String SESSION_KEY = "visitor_preferences";
    private static final Set<String> INTERESTS = Set.of(
            "ATTRACTION", "CULTURE", "FOOD", "NATURE", "RETAIL", "PUBLIC_SERVICE");
    private static final Set<String> TRANSPORT = Set.of("WALK", "PUBLIC_TRANSIT", "MIXED");
    private static final Set<String> LANGUAGES = Set.of("zh-Hans", "zh-Hant", "en", "pt");
    private static final Set<String> ACCESSIBILITY = Set.of("STEP_FREE", "LOW_WALKING", "QUIET_ROUTE");

    @Override
    public VisitorPreferencesResponse getCurrent() {
        Object stored = StpUtil.getSession().get(SESSION_KEY);
        if (stored instanceof VisitorPreferencesResponse preferences) {
            return copy(preferences);
        }
        return defaults();
    }

    @Override
    public VisitorPreferencesResponse save(VisitorPreferencesRequest request) {
        validate(request);
        VisitorPreferencesResponse response = new VisitorPreferencesResponse(
                new ArrayList<>(request.getInterests()),
                request.getDepartureTime(),
                request.getLatestEndTime(),
                request.getMaxWalkingMeters(),
                new ArrayList<>(request.getMustVisitPoiIds()),
                request.getTransportPreference(),
                request.getLanguage(),
                new ArrayList<>(request.getAccessibilityNeeds()),
                request.getCurrentLongitude(),
                request.getCurrentLatitude(),
                request.getCurrentLocationName(),
                request.getLocationSource(),
                true,
                System.currentTimeMillis());
        StpUtil.getSession().set(SESSION_KEY, response);
        return copy(response);
    }

    @Override
    public VisitorPreferencesResponse reset() {
        StpUtil.getSession().delete(SESSION_KEY);
        return defaults();
    }

    private void validate(VisitorPreferencesRequest request) {
        boolean hasLongitude = request.getCurrentLongitude() != null;
        boolean hasLatitude = request.getCurrentLatitude() != null;
        if (hasLongitude != hasLatitude) {
            throw new IllegalArgumentException("当前位置必须同时包含经度和纬度");
        }
        if (hasLongitude && (request.getLocationSource() == null || request.getLocationSource().isBlank())) {
            throw new IllegalArgumentException("设置当前位置时必须提供位置来源");
        }
        if (!hasLongitude && request.getLocationSource() != null) {
            throw new IllegalArgumentException("未设置坐标时不能提供位置来源");
        }
        if (!INTERESTS.containsAll(request.getInterests())) {
            throw new IllegalArgumentException("包含不支持的兴趣选项");
        }
        if (!TRANSPORT.contains(request.getTransportPreference())) {
            throw new IllegalArgumentException("不支持的交通偏好");
        }
        if (!LANGUAGES.contains(request.getLanguage())) {
            throw new IllegalArgumentException("不支持的语言");
        }
        if (!ACCESSIBILITY.containsAll(request.getAccessibilityNeeds())) {
            throw new IllegalArgumentException("包含不支持的无障碍选项");
        }
        if (!LocalTime.parse(request.getDepartureTime()).isBefore(LocalTime.parse(request.getLatestEndTime()))) {
            throw new IllegalArgumentException("最晚结束时间必须晚于出发时间");
        }
    }

    private VisitorPreferencesResponse defaults() {
        return new VisitorPreferencesResponse(
                new ArrayList<>(List.of("CULTURE", "FOOD")),
                "09:00", "14:00", 5000,
                new ArrayList<>(), "MIXED", "zh-Hans",
                new ArrayList<>(), null, null, null, null, false, null);
    }

    private VisitorPreferencesResponse copy(VisitorPreferencesResponse source) {
        return new VisitorPreferencesResponse(
                new ArrayList<>(source.getInterests()),
                source.getDepartureTime(),
                source.getLatestEndTime(),
                source.getMaxWalkingMeters(),
                new ArrayList<>(source.getMustVisitPoiIds()),
                source.getTransportPreference(),
                source.getLanguage(),
                new ArrayList<>(source.getAccessibilityNeeds()),
                source.getCurrentLongitude(),
                source.getCurrentLatitude(),
                source.getCurrentLocationName(),
                source.getLocationSource(),
                source.isSaved(), source.getUpdatedAt());
    }
}
