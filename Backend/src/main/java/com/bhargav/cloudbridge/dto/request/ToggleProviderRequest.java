package com.bhargav.cloudbridge.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToggleProviderRequest {

    @NotNull(message = "Enabled status is required")
    private Boolean enabled;

    // Optional reason — logged for audit
    private String reason;
}