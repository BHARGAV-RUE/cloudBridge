package com.bhargav.cloudbridge.implementation.storage;

import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;
import com.bhargav.cloudbridge.entity.FileMetadata;
import com.bhargav.cloudbridge.repository.CloudStorageCapacityRepository;
import com.bhargav.cloudbridge.repository.FileMetadataRepository;
import com.bhargav.cloudbridge.service.storage.StorageCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCapacityServiceImpl implements StorageCapacityService {

    private final CloudStorageCapacityRepository capacityRepository;
    private final FileMetadataRepository fileMetadataRepository;

    // ── INCREMENT ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void incrementUsage(CloudProvider provider, long fileSizeBytes) {

        CloudStorageCapacity capacity = findOrThrow(provider);

        long before = capacity.getUsedBytes();
        capacity.setUsedBytes(before + fileSizeBytes);
        capacityRepository.save(capacity);

        log.info("[CapacityService] {} usage: {} → {} bytes (limit: {})",
                provider,
                before,
                capacity.getUsedBytes(),
                capacity.getTotalLimitBytes());
    }

    // ── DECREMENT ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void decrementUsage(CloudProvider provider, long fileSizeBytes) {

        CloudStorageCapacity capacity = findOrThrow(provider);

        long before = capacity.getUsedBytes();

        // Guard against going negative — shouldn't happen but safety net
        long newUsage = Math.max(0, before - fileSizeBytes);
        capacity.setUsedBytes(newUsage);
        capacityRepository.save(capacity);

        log.info("[CapacityService] {} usage decremented: {} → {} bytes",
                provider, before, newUsage);
    }

    // ── GET SINGLE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CloudStorageCapacity getCapacity(CloudProvider provider) {
        return findOrThrow(provider);
    }

    // ── GET ALL ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CloudStorageCapacity> getAllCapacities() {
        List<CloudStorageCapacity> capacities = capacityRepository.findAll();

        capacities.forEach(c -> log.info(
                "[CapacityService] {} → used: {} / total: {} | free: {}",
                c.getProvider(),
                c.getUsedBytes(),
                c.getTotalLimitBytes(),
                c.getAvailableBytes()
        ));

        return capacities;
    }

    // ── RECALCULATE SINGLE ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void recalculateUsage(CloudProvider provider) {

        List<FileMetadata> files = fileMetadataRepository
                .findByCloudProviderAndIsDeletedFalse(provider);

        long actualUsage = files.stream()
                .mapToLong(f -> f.getCompressedSize() != null
                        ? f.getCompressedSize()
                        : f.getOriginalSize() != null
                        ? f.getOriginalSize()
                        : 0L)
                .sum();

        CloudStorageCapacity capacity = findOrThrow(provider);
        long before = capacity.getUsedBytes();
        capacity.setUsedBytes(actualUsage);
        capacityRepository.save(capacity);

        log.info("[CapacityService] Recalculated {} → was: {} | now: {} bytes",
                provider, before, actualUsage);
    }

    // ── RECALCULATE ALL ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void recalculateAllProviders() {
        log.info("[CapacityService] Recalculating all providers...");

        for (CloudProvider provider : CloudProvider.values()) {
            recalculateUsage(provider);
        }

        log.info("[CapacityService] Recalculation complete for all providers.");
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private CloudStorageCapacity findOrThrow(CloudProvider provider) {
        return capacityRepository.findByProvider(provider)
                .orElseThrow(() -> {
                    log.error("[CapacityService] No capacity record for: {}", provider);
                    return new RuntimeException(
                            "Capacity record not found for provider: " + provider);
                });
    }
}