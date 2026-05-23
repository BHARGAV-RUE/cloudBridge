package com.bhargav.cloudbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_cloud_providers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DynamicCloudProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Unique identifier used in factory registry
    // e.g. "WASABI_S3", "CLOUDFLARE_R2"
    @Column(name = "provider_key", nullable = false, unique = true)
    private String providerKey;

    // Human readable
    @Column(name = "display_name", nullable = false)
    private String displayName;

    // S3_COMPATIBLE, AZURE_COMPATIBLE, CUSTOM
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private DynamicProviderType providerType;

    // Encrypted JSON blob of credentials
    // e.g. { "endpoint": "...", "accessKey": "...", "secretKey": "..." }
    // Stored encrypted using master key — never plaintext
    @Column(name = "credentials_encrypted",
            nullable = false, columnDefinition = "TEXT")
    private String credentialsEncrypted;

    @Column(name = "region")
    private String region;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(name = "total_limit_bytes", nullable = false)
    private Long totalLimitBytes;

    @Column(name = "used_bytes", nullable = false)
    private Long usedBytes = 0L;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "added_by")
    private String addedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        if (this.usedBytes == null) this.usedBytes = 0L;
        if (this.isEnabled == null) this.isEnabled = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    @Transient
    public long getAvailableBytes() {
        return totalLimitBytes - usedBytes;
    }

    @Transient
    public boolean hasCapacity(long requiredBytes) {
        return isEnabled && getAvailableBytes() >= requiredBytes;
    }
}