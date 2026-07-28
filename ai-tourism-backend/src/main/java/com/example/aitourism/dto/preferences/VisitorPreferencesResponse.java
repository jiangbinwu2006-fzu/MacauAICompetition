package com.example.aitourism.dto.preferences;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorPreferencesResponse {
    private List<String> interests;
    private String departureTime;
    private String latestEndTime;
    private Integer maxWalkingMeters;
    private List<Long> mustVisitPoiIds;
    private String transportPreference;
    private String language;
    private List<String> accessibilityNeeds;
    private BigDecimal currentLongitude;
    private BigDecimal currentLatitude;
    private String currentLocationName;
    private String locationSource;
    private boolean saved;
    private Long updatedAt;
}
