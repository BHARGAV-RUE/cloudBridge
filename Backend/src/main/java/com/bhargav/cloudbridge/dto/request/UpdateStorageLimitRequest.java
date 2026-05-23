package com.bhargav.cloudbridge.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStorageLimitRequest {

    @NotNull(message = "Limit is required")
    @Min(value = 1, message = "Limit must be at least 1 byte")
    private Long limitBytes;
}