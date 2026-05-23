package com.bhargav.cloudbridge.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminProviderResponse {

    private String providerKey;
    private String displayName;
    private String providerType;    // STATIC or DYNAMIC
    private Boolean isEnabled;
    private Long totalLimitBytes;
    private Long usedBytes;
    private Long availableBytes;
    private Double usagePercentage;
    private String formattedTotal;
    private String formattedUsed;
    private String formattedAvailable;
    private Boolean isDynamic;      // true = added by admin, false = built-in
}