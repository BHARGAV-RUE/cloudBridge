package com.bhargav.cloudbridge.repository;

import com.bhargav.cloudbridge.entity.DynamicCloudProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DynamicCloudProviderRepository
        extends JpaRepository<DynamicCloudProvider, String> {

    Optional<DynamicCloudProvider> findByProviderKey(String providerKey);

    // Only enabled providers for upload selection
    List<DynamicCloudProvider> findByIsEnabledTrue();

    // All providers for admin dashboard
    List<DynamicCloudProvider> findAll();

    // Enabled + has capacity — for intelligent selector
    @Query("SELECT d FROM DynamicCloudProvider d " +
            "WHERE d.isEnabled = true " +
            "AND (d.totalLimitBytes - d.usedBytes) > 0 " +
            "ORDER BY (d.totalLimitBytes - d.usedBytes) DESC")
    List<DynamicCloudProvider> findEnabledProvidersSortedByAvailableSpace();
}