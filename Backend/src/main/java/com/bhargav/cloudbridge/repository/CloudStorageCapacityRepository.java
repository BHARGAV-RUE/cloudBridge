package com.bhargav.cloudbridge.repository;

import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CloudStorageCapacityRepository
        extends JpaRepository<CloudStorageCapacity, Long> {

    Optional<CloudStorageCapacity> findByProvider(CloudProvider provider);

    @Query("SELECT c FROM CloudStorageCapacity c " +
            "ORDER BY (c.totalLimitBytes - c.usedBytes) DESC")
    List<CloudStorageCapacity> findPrimaryProvidersSortedByAvailableSpace();
}