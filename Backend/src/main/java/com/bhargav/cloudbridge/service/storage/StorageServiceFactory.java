package com.bhargav.cloudbridge.service.storage;

import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;
import com.bhargav.cloudbridge.entity.DynamicCloudProvider;
import com.bhargav.cloudbridge.exception.StorageFullException;
import com.bhargav.cloudbridge.repository.CloudStorageCapacityRepository;
import com.bhargav.cloudbridge.repository.DynamicCloudProviderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StorageServiceFactory {

    private final Map<String, StorageService> registry;
    private final CloudStorageCapacityRepository capacityRepository;
    private final DynamicCloudProviderRepository dynamicProviderRepository;

    public StorageServiceFactory(
            List<StorageService> allProviders,
            CloudStorageCapacityRepository capacityRepository,
            DynamicCloudProviderRepository dynamicProviderRepository) {

        this.capacityRepository = capacityRepository;
        this.dynamicProviderRepository = dynamicProviderRepository;

        this.registry = allProviders.stream()
                .collect(Collectors.toMap(
                        StorageService::getProviderName,
                        Function.identity()
                ));

        log.info("[StorageFactory] Registered providers: {}", registry.keySet());
    }

    // ── GET BY NAME ────────────────────────────────────────────────────────────

    public StorageService getProvider(String providerName) {
        StorageService service = registry.get(providerName);
        if (service == null) {
            throw new IllegalArgumentException(
                    "[StorageFactory] Unknown provider: '" + providerName + "'" +
                            " | Registered: " + registry.keySet()
            );
        }
        return service;
    }

    // ── INTELLIGENT SELECTOR ───────────────────────────────────────────────────

    /**
     * Selects best provider by:
     * 1. Static providers that are enabled + have capacity
     * 2. Dynamic providers that are enabled + have capacity
     * All sorted by most available space
     * Throws StorageFullException if none qualify
     */
    public StorageService selectPrimaryProvider(long requiredBytes) {

        // Check static providers (AWS, Azure, OCI, Google Drive)
        List<CloudStorageCapacity> staticCandidates = capacityRepository
                .findPrimaryProvidersSortedByAvailableSpace()
                .stream()
                .filter(c -> c.getIsEnabled())
                .filter(c -> c.hasCapacity(requiredBytes))
                .toList();

        // Check dynamic providers
        List<DynamicCloudProvider> dynamicCandidates = dynamicProviderRepository
                .findEnabledProvidersSortedByAvailableSpace()
                .stream()
                .filter(d -> d.hasCapacity(requiredBytes))
                .toList();

        // Prefer static with most space
        if (!staticCandidates.isEmpty()) {
            CloudStorageCapacity best = staticCandidates.get(0);
            log.info("[StorageFactory] Selected static provider: {} | Available: {}",
                    best.getProvider(), best.getAvailableBytes());
            return getProvider(best.getProvider().name());
        }

        // Fall back to dynamic with most space
        if (!dynamicCandidates.isEmpty()) {
            DynamicCloudProvider best = dynamicCandidates.get(0);
            log.info("[StorageFactory] Selected dynamic provider: {} | Available: {}",
                    best.getProviderKey(), best.getAvailableBytes());
            return getProvider(best.getProviderKey());
        }

        throw new StorageFullException(requiredBytes);
    }

    public List<String> getAvailableProviders() {
        return List.copyOf(registry.keySet());
    }

    /**
     * Register a dynamic provider at runtime.
     * Called by AdminDashboardService after saving to DB.
     */
    public void registerDynamicProvider(String providerKey,
                                        StorageService storageService) {
        registry.put(providerKey, storageService);
        log.info("[StorageFactory] Dynamic provider registered: {}", providerKey);
    }

    /**
     * Unregister a dynamic provider.
     * Called when admin deletes a provider.
     */
    public void unregisterProvider(String providerKey) {
        registry.remove(providerKey);
        log.info("[StorageFactory] Provider unregistered: {}", providerKey);
    }
}