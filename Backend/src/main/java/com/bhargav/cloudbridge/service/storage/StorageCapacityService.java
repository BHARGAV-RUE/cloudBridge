package com.bhargav.cloudbridge.service.storage;

import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;

import java.util.List;

public interface StorageCapacityService {

    /**
     * Add file size to provider's used_bytes after successful upload.
     *
     * @param provider     the provider that received the file
     * @param fileSizeBytes size of the uploaded file in bytes
     */
    void incrementUsage(CloudProvider provider, long fileSizeBytes);

    /**
     * Subtract file size from provider's used_bytes after file deletion.
     *
     * @param provider     the provider the file was deleted from
     * @param fileSizeBytes size of the deleted file in bytes
     */
    void decrementUsage(CloudProvider provider, long fileSizeBytes);

    /**
     * Get capacity record for a specific provider.
     *
     * @param provider the cloud provider
     * @return CloudStorageCapacity with used/total/available bytes
     */
    CloudStorageCapacity getCapacity(CloudProvider provider);

    /**
     * Get capacity records for all providers — used by analytics endpoint.
     *
     * @return list of all provider capacity records
     */
    List<CloudStorageCapacity> getAllCapacities();

    /**
     * Recalculate used_bytes for a provider from actual DB records.
     * Safety net — call if numbers drift out of sync.
     *
     * @param provider the provider to recalculate
     */
    void recalculateUsage(CloudProvider provider);

    /**
     * Recalculate used_bytes for ALL providers at once.
     * Can be called from a weekly maintenance scheduler.
     */
    void recalculateAllProviders();
}