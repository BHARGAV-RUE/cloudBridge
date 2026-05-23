package com.bhargav.cloudbridge.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDownloadLimitRequest {

    @NotNull(message = "Download limit is required")
    @Min(value = 1, message = "Download limit must be at least 1")
    private Integer downloadLimit;
}