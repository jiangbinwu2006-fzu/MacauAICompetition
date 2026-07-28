package com.example.aitourism.dto.preferences;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

@Data
public class VisitorPreferencesRequest {

    @NotEmpty(message = "请至少选择一个兴趣")
    @Size(max = 8, message = "兴趣最多选择 8 项")
    private List<String> interests;

    @NotBlank
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "出发时间格式必须为 HH:mm")
    private String departureTime;

    @NotBlank
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式必须为 HH:mm")
    private String latestEndTime;

    @NotNull
    @Min(value = 500, message = "步行上限不能少于 500 米")
    @Max(value = 20000, message = "步行上限不能超过 20000 米")
    private Integer maxWalkingMeters;

    @NotNull
    @Size(max = 8, message = "必去点最多选择 8 个")
    private List<Long> mustVisitPoiIds;

    @NotBlank
    private String transportPreference;

    @NotBlank
    private String language;

    @NotNull
    @Size(max = 3, message = "无障碍选项最多选择 3 项")
    private List<String> accessibilityNeeds;

    @DecimalMin(value = "113.4", message = "位置经度必须在澳门范围内")
    @DecimalMax(value = "113.7", message = "位置经度必须在澳门范围内")
    private BigDecimal currentLongitude;

    @DecimalMin(value = "22.0", message = "位置纬度必须在澳门范围内")
    @DecimalMax(value = "22.3", message = "位置纬度必须在澳门范围内")
    private BigDecimal currentLatitude;

    @Size(max = 100, message = "位置名称不能超过 100 个字符")
    private String currentLocationName;

    @Pattern(regexp = "^(GPS|MANUAL)$", message = "不支持的位置来源")
    private String locationSource;
}
