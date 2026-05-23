package com.bhargav.cloudbridge.dto.request;

import com.bhargav.cloudbridge.entity.DynamicProviderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddDynamicProviderRequest {

    // Unique key — used in factory registry
    // e.g. "WASABI_S3", "CLOUDFLARE_R2"
    @NotBlank(message = "Provider key is required")
    private String providerKey;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Provider type is required")
    private DynamicProviderType providerType;

    // Credentials — stored encrypted
    @NotBlank(message = "Access key is required")
    private String accessKey;

    @NotBlank(message = "Secret key is required")
    private String secretKey;

    @NotBlank(message = "Bucket name is required")
    private String bucketName;

    // Required for S3_COMPATIBLE
    private String endpointUrl;

    private String region;

    @NotNull(message = "Storage limit is required")
    @Min(value = 1, message = "Storage limit must be at least 1 byte")
    private Long totalLimitBytes;
}