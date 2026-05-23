package com.bhargav.cloudbridge.service.scheduler;

import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.FileMetadata;
import com.bhargav.cloudbridge.repository.FileMetadataRepository;
import com.bhargav.cloudbridge.service.storage.StorageCapacityService;
import com.bhargav.cloudbridge.service.storage.StorageService;
import com.bhargav.cloudbridge.service.storage.StorageServiceFactory;
import com.bhargav.cloudbridge.service.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileMetadataRepository fileMetadataRepository;
    private final StorageServiceFactory storageServiceFactory;
    private final StorageCapacityService storageCapacityService;
    private final TokenService tokenService;

    // ── NIGHTLY CLEANUP ────────────────────────────────────────────────────────

    /**
     * Runs every night at midnight.
     * Cleans up expired files and limit-reached files.
     * cron = second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void runNightlyCleanup() {
        log.info("[Scheduler] ===== Nightly cleanup started: {} =====",
                LocalDateTime.now());

        // Collect both lists — may overlap so use combined set
        List<FileMetadata> expiredFiles = fileMetadataRepository
                .findExpiredFiles(LocalDateTime.now());

        List<FileMetadata> limitReachedFiles = fileMetadataRepository
                .findLimitReachedFiles();

        // Merge without duplicates
        List<FileMetadata> toClean = mergeLists(expiredFiles, limitReachedFiles);

        if (toClean.isEmpty()) {
            log.info("[Scheduler] No files to clean up tonight.");
            return;
        }

        log.info("[Scheduler] Files to clean: {} ({} expired, {} limit-reached)",
                toClean.size(), expiredFiles.size(), limitReachedFiles.size());

        int success = 0;
        int failed  = 0;

        for (FileMetadata file : toClean) {
            try {
                cleanupSingleFile(file);
                success++;
            } catch (Exception e) {
                log.error("[Scheduler] Failed to clean file: {} | reason: {}",
                        file.getId(), e.getMessage());
                failed++;
                // Continue with next file — don't stop entire batch
            }
        }

        log.info("[Scheduler] ===== Cleanup complete | success: {} | failed: {} =====",
                success, failed);
    }

    // ── WEEKLY RECALIBRATION ───────────────────────────────────────────────────

    /**
     * Runs every Sunday at 2AM.
     * Recalculates all provider used_bytes from actual DB records.
     * Safety net for any drift that accumulates over the week.
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void runWeeklyCapacityRecalibration() {
        log.info("[Scheduler] ===== Weekly capacity recalibration started =====");

        try {
            storageCapacityService.recalculateAllProviders();
            log.info("[Scheduler] ===== Recalibration complete =====");
        } catch (Exception e) {
            log.error("[Scheduler] Recalibration failed: {}", e.getMessage());
        }
    }

    // ── SINGLE FILE CLEANUP ────────────────────────────────────────────────────

    private void cleanupSingleFile(FileMetadata file) {
        log.info("[Scheduler] Cleaning file: {} | provider: {} | path: {}",
                file.getId(),
                file.getCloudProvider(),
                file.getStoragePath());

        // Step 1 — Delete from cloud provider
        deleteFromCloud(file);

        // Step 2 — Decrement provider capacity
        decrementCapacity(file);

        // Step 3 — Deactivate all download tokens
        tokenService.deactivateAllTokensForFile(file.getId());

        // Step 4 — Soft delete in DB
        // Metadata stays forever — hard delete never happens
        file.setIsDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileMetadataRepository.save(file);

        log.info("[Scheduler] File cleaned successfully: {}", file.getId());
    }

    // ── CLOUD DELETION ─────────────────────────────────────────────────────────

    private void deleteFromCloud(FileMetadata file) {
        try {
            StorageService storageService = storageServiceFactory
                    .getProvider(file.getCloudProvider().name());

            if (storageService.fileExists(file.getStoragePath())) {
                storageService.deleteFile(file.getStoragePath());
                log.info("[Scheduler] Deleted from {}: {}",
                        file.getCloudProvider(), file.getStoragePath());
            } else {
                // File already gone from cloud — log and continue
                log.warn("[Scheduler] File not found in cloud (already deleted?): {}",
                        file.getStoragePath());
            }

        } catch (Exception e) {
            log.error("[Scheduler] Cloud deletion failed | file: {} | reason: {}",
                    file.getId(), e.getMessage());
            // Don't re-throw — still soft delete in DB even if cloud fails
        }
    }

    // ── CAPACITY DECREMENT ─────────────────────────────────────────────────────

    private void decrementCapacity(FileMetadata file) {
        try {
            CloudProvider provider = file.getCloudProvider();

            long sizeToFree = file.getCompressedSize() != null
                    ? file.getCompressedSize()
                    : file.getOriginalSize() != null
                    ? file.getOriginalSize()
                    : 0L;

            storageCapacityService.decrementUsage(provider, sizeToFree);

        } catch (Exception e) {
            log.error("[Scheduler] Capacity decrement failed | file: {} | reason: {}",
                    file.getId(), e.getMessage());
        }
    }

    // ── UTILITY ────────────────────────────────────────────────────────────────

    /**
     * Merge two lists without duplicates by file ID.
     */
    private List<FileMetadata> mergeLists(List<FileMetadata> list1,
                                          List<FileMetadata> list2) {
        List<FileMetadata> merged = new ArrayList<>(list1);

        for (FileMetadata file : list2) {
            boolean alreadyPresent = merged.stream()
                    .anyMatch(f -> f.getId().equals(file.getId()));
            if (!alreadyPresent) {
                merged.add(file);
            }
        }

        return merged;
    }
}