package com.bhargav.cloudbridge.controller;

import com.bhargav.cloudbridge.dto.request.*;
import com.bhargav.cloudbridge.dto.response.*;
import com.bhargav.cloudbridge.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── AUTH ───────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @RequestBody @Valid AdminLoginRequest request) {

        AdminLoginResponse response = adminService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response));
    }

    // ── FILE MANAGEMENT ────────────────────────────────────────────────────────

    @GetMapping("/files")
    public ResponseEntity<ApiResponse<Page<AdminFileResponse>>> getAllFiles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<AdminFileResponse> files = adminService
                .getAllFiles(search, provider, isDeleted, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Files retrieved", files));
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<AdminFileResponse>> getFileById(
            @PathVariable String fileId) {

        AdminFileResponse file = adminService.getFileById(fileId);
        return ResponseEntity.ok(
                ApiResponse.success("File retrieved", file));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String fileId) {

        adminService.deleteFile(fileId);
        return ResponseEntity.ok(
                ApiResponse.success("File deleted successfully"));
    }

    @PatchMapping("/files/{fileId}/expiry")
    public ResponseEntity<ApiResponse<Void>> updateExpiry(
            @PathVariable String fileId,
            @RequestBody @Valid UpdateExpiryRequest request) {

        adminService.updateExpiry(fileId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Expiry date updated"));
    }

    @PatchMapping("/files/{fileId}/download-limit")
    public ResponseEntity<ApiResponse<Void>> updateDownloadLimit(
            @PathVariable String fileId,
            @RequestBody @Valid UpdateDownloadLimitRequest request) {

        adminService.updateDownloadLimit(fileId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Download limit updated"));
    }

    @PostMapping("/files/{fileId}/resend-email")
    public ResponseEntity<ApiResponse<Void>> resendEmail(
            @PathVariable String fileId) {

        adminService.resendEmail(fileId);
        return ResponseEntity.ok(
                ApiResponse.success("Email resent successfully"));
    }

    // ── STORAGE MANAGEMENT ─────────────────────────────────────────────────────

    @GetMapping("/storage/providers")
    public ResponseEntity<ApiResponse<List<AdminProviderResponse>>>
    getAllProviders() {

        List<AdminProviderResponse> providers = adminService.getAllProviders();
        return ResponseEntity.ok(
                ApiResponse.success("Providers retrieved", providers));
    }

    @PatchMapping("/storage/{provider}/limit")
    public ResponseEntity<ApiResponse<Void>> updateStorageLimit(
            @PathVariable String provider,
            @RequestBody @Valid UpdateStorageLimitRequest request) {

        adminService.updateStorageLimit(provider, request);
        return ResponseEntity.ok(
                ApiResponse.success("Storage limit updated"));
    }

    @PatchMapping("/storage/{provider}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleProvider(
            @PathVariable String provider,
            @RequestBody @Valid ToggleProviderRequest request) {

        adminService.toggleProvider(provider, request);
        String status = request.getEnabled() ? "enabled" : "disabled";
        return ResponseEntity.ok(
                ApiResponse.success("Provider " + status + " successfully"));
    }

    @PostMapping("/storage/providers")
    public ResponseEntity<ApiResponse<AdminProviderResponse>> addDynamicProvider(
            @RequestBody @Valid AddDynamicProviderRequest request) {

        AdminProviderResponse response = adminService
                .addDynamicProvider(request);
        return ResponseEntity.ok(
                ApiResponse.success("Provider added successfully", response));
    }

    @PatchMapping("/storage/providers/{providerKey}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleDynamicProvider(
            @PathVariable String providerKey,
            @RequestBody @Valid ToggleProviderRequest request) {

        adminService.toggleDynamicProvider(providerKey, request);
        String status = request.getEnabled() ? "enabled" : "disabled";
        return ResponseEntity.ok(
                ApiResponse.success("Provider " + status));
    }

    @DeleteMapping("/storage/providers/{providerKey}")
    public ResponseEntity<ApiResponse<Void>> deleteDynamicProvider(
            @PathVariable String providerKey) {

        adminService.deleteDynamicProvider(providerKey);
        return ResponseEntity.ok(
                ApiResponse.success("Provider deleted successfully"));
    }

    // ── ANALYTICS ──────────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics() {

        AdminAnalyticsResponse analytics = adminService.getAnalytics();
        return ResponseEntity.ok(
                ApiResponse.success("Analytics retrieved", analytics));
    }

    @GetMapping("/analytics/emails/senders")
    public ResponseEntity<ApiResponse<List<String>>> getSenderEmails() {

        List<String> emails = adminService.getAllSenderEmails();
        return ResponseEntity.ok(
                ApiResponse.success("Sender emails retrieved", emails));
    }

    @GetMapping("/analytics/emails/downloaders")
    public ResponseEntity<ApiResponse<List<String>>> getDownloaderEmails() {

        List<String> emails = adminService.getAllDownloaderEmails();
        return ResponseEntity.ok(
                ApiResponse.success("Downloader emails retrieved", emails));
    }

    // ── SYSTEM ─────────────────────────────────────────────────────────────────

    @PostMapping("/system/cleanup")
    public ResponseEntity<ApiResponse<Void>> triggerCleanup() {

        adminService.triggerManualCleanup();
        return ResponseEntity.ok(
                ApiResponse.success("Cleanup triggered successfully"));
    }

    @PostMapping("/system/recalibrate")
    public ResponseEntity<ApiResponse<Void>> triggerRecalibration() {

        adminService.triggerCapacityRecalibration();
        return ResponseEntity.ok(
                ApiResponse.success("Recalibration triggered successfully"));
    }

    @GetMapping("/system/health")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>>
    checkHealth() {

        Map<String, Boolean> health = adminService.checkProvidersHealth();
        return ResponseEntity.ok(
                ApiResponse.success("Health check complete", health));
    }
}