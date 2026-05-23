package com.bhargav.cloudbridge.service.admin;

import com.bhargav.cloudbridge.dto.request.*;
import com.bhargav.cloudbridge.dto.response.*;
import com.bhargav.cloudbridge.entity.CloudProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdminService {

    // ── AUTH ───────────────────────────────────────────────────────────────────
    AdminLoginResponse login(AdminLoginRequest request);

    // ── FILE MANAGEMENT ────────────────────────────────────────────────────────
    Page<AdminFileResponse> getAllFiles(String search,
                                        String provider,
                                        Boolean isDeleted,
                                        Pageable pageable);

    AdminFileResponse getFileById(String fileId);

    void deleteFile(String fileId);

    void updateExpiry(String fileId, UpdateExpiryRequest request);

    void updateDownloadLimit(String fileId, UpdateDownloadLimitRequest request);

    void resendEmail(String fileId);

    // ── STORAGE MANAGEMENT ─────────────────────────────────────────────────────
    List<AdminProviderResponse> getAllProviders();

    void updateStorageLimit(String provider, UpdateStorageLimitRequest request);

    void toggleProvider(String provider, ToggleProviderRequest request);

    AdminProviderResponse addDynamicProvider(AddDynamicProviderRequest request);

    void toggleDynamicProvider(String providerKey, ToggleProviderRequest request);

    void deleteDynamicProvider(String providerKey);

    // ── ANALYTICS ──────────────────────────────────────────────────────────────
    AdminAnalyticsResponse getAnalytics();

    List<String> getAllSenderEmails();

    List<String> getAllDownloaderEmails();

    // ── SYSTEM ─────────────────────────────────────────────────────────────────
    void triggerManualCleanup();

    void triggerCapacityRecalibration();

    Map<String, Boolean> checkProvidersHealth();
}