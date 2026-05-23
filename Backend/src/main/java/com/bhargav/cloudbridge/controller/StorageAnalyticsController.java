package com.bhargav.cloudbridge.controller;

import com.bhargav.cloudbridge.dto.response.ApiResponse;
import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;
import com.bhargav.cloudbridge.service.storage.StorageCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class StorageAnalyticsController {

    private final StorageCapacityService storageCapacityService;

    /**
     * GET /api/analytics/storage
     *
     * Returns used/total/available bytes for all providers.
     * Used by admin dashboard or frontend storage indicator.
     */
    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<List<CloudStorageCapacity>>> getAllStorageStats() {

        log.info("[AnalyticsController] Storage stats requested");

        List<CloudStorageCapacity> capacities =
                storageCapacityService.getAllCapacities();

        return ResponseEntity.ok(
                ApiResponse.success("Storage stats retrieved", capacities)
        );
    }

    /**
     * GET /api/analytics/storage/{provider}
     *
     * Returns stats for a single provider.
     * e.g. GET /api/analytics/storage/AWS_S3
     */
    @GetMapping("/storage/{provider}")
    public ResponseEntity<ApiResponse<CloudStorageCapacity>> getProviderStats(
            @PathVariable String provider) {

        log.info("[AnalyticsController] Stats requested for provider: {}", provider);

        CloudProvider cloudProvider = CloudProvider.valueOf(provider.toUpperCase());
        CloudStorageCapacity capacity =
                storageCapacityService.getCapacity(cloudProvider);

        return ResponseEntity.ok(
                ApiResponse.success("Provider stats retrieved", capacity)
        );
    }

    /**
     * POST /api/analytics/storage/recalculate
     *
     * Triggers recalculation of all provider used_bytes
     * from actual DB records. Safety net endpoint.
     */
    @PostMapping("/storage/recalculate")
    public ResponseEntity<ApiResponse<Void>> recalculateAllStorage() {

        log.info("[AnalyticsController] Recalculation triggered");

        storageCapacityService.recalculateAllProviders();

        return ResponseEntity.ok(
                ApiResponse.success("Storage recalculated for all providers")
        );
    }
}