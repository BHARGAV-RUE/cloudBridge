package com.bhargav.cloudbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_storage_capacity")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CloudStorageCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, unique = true)
    private CloudProvider provider;

    @Column(name = "total_limit_bytes", nullable = false)
    private Long totalLimitBytes;

    @Builder.Default
    @Column(name = "used_bytes", nullable = false)
    private Long usedBytes = 0L;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void onUpdate(){
        this.lastUpdated = LocalDateTime.now();
    }

    public long getAvailableBytes(){
        return totalLimitBytes - usedBytes;
    }

    public boolean hasCapacity(long requiredBytes){
        return getAvailableBytes() >= requiredBytes;
    }

}
