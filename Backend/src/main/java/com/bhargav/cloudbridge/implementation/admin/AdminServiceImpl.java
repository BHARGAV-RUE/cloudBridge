package com.bhargav.cloudbridge.implementation.admin;

import com.bhargav.cloudbridge.dto.request.*;
import com.bhargav.cloudbridge.dto.response.*;
import com.bhargav.cloudbridge.entity.*;
import com.bhargav.cloudbridge.exception.CloudBridgeException;
import com.bhargav.cloudbridge.exception.FileNotFoundException;
import com.bhargav.cloudbridge.repository.*;
import com.bhargav.cloudbridge.security.JwtService;
import com.bhargav.cloudbridge.service.admin.AdminService;
import com.bhargav.cloudbridge.service.email.EmailService;
import com.bhargav.cloudbridge.service.scheduler.FileCleanupScheduler;
import com.bhargav.cloudbridge.service.storage.StorageCapacityService;
import com.bhargav.cloudbridge.service.storage.StorageService;
import com.bhargav.cloudbridge.service.storage.StorageServiceFactory;
import com.bhargav.cloudbridge.service.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final DownloadLogRepository downloadLogRepository;
    private final CloudStorageCapacityRepository capacityRepository;
    private final DynamicCloudProviderRepository dynamicProviderRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final StorageServiceFactory storageServiceFactory;
    private final StorageCapacityService storageCapacityService;
    private final FileCleanupScheduler fileCleanupScheduler;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ── AUTH ───────────────────────────────────────────────────────────────────

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new CloudBridgeException(
                        "Invalid username or password",
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS"
                ));

        if (!admin.getIsActive()) {
            throw new CloudBridgeException(
                    "Admin account is disabled",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_DISABLED"
            );
        }

        if (!passwordEncoder.matches(request.getPassword(),
                admin.getPasswordHash())) {
            throw new CloudBridgeException(
                    "Invalid username or password",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }

        // Update last login
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        String token = jwtService.generateToken(admin.getUsername());
        Date expiry  = jwtService.extractExpiry(token);

        log.info("[AdminService] Admin logged in: {}", admin.getUsername());

        return AdminLoginResponse.builder()
                .token(token)
                .username(admin.getUsername())
                .expiresAt(expiry.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime())
                .build();
    }

    // ── FILE MANAGEMENT ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFileResponse> getAllFiles(String search,
                                               String provider,
                                               Boolean isDeleted,
                                               Pageable pageable) {
        // Fetch all then filter — replace with Specification for production
        Page<FileMetadata> files = fileMetadataRepository.findAll(pageable);

        return files.map(this::toAdminFileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFileResponse getFileById(String fileId) {
        FileMetadata file = findFileOrThrow(fileId);
        return toAdminFileResponse(file);
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        FileMetadata file = findFileOrThrow(fileId);

        if (file.getIsDeleted()) {
            throw new CloudBridgeException(
                    "File is already deleted",
                    HttpStatus.BAD_REQUEST,
                    "ALREADY_DELETED"
            );
        }

        // Delete from cloud
        try {
            StorageService storageService = storageServiceFactory
                    .getProvider(file.getCloudProvider().name()); //
            if (storageService.fileExists(file.getStoragePath())) {
                storageService.deleteFile(file.getStoragePath());
            }
        } catch (Exception e) {
            log.error("[AdminService] Cloud delete failed for: {} | {}",
                    fileId, e.getMessage());
        }

        // Decrement capacity
        try {
            CloudProvider provider = file.getCloudProvider(); //

            long sizeToFree = file.getCompressedSize() != null
                    ? file.getCompressedSize()
                    : file.getOriginalSize() != null
                    ? file.getOriginalSize()
                    : 0L; // ✅ null-safe cascade, no getFileSize()

            storageCapacityService.decrementUsage(provider, sizeToFree);
        } catch (Exception e) {
            log.error("[AdminService] Capacity decrement failed: {}", e.getMessage());
        }

        // Deactivate tokens
        tokenService.deactivateAllTokensForFile(fileId);

        // Soft delete
        file.setIsDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileMetadataRepository.save(file);

        log.info("[AdminService] File deleted by admin: {}", fileId);
    }

    @Override
    @Transactional
    public void updateExpiry(String fileId, UpdateExpiryRequest request) {
        FileMetadata file = findFileOrThrow(fileId);
        file.setExpiryDate(request.getExpiryDate());
        fileMetadataRepository.save(file);
        log.info("[AdminService] Expiry updated for: {} → {}",
                fileId, request.getExpiryDate());
    }

    @Override
    @Transactional
    public void updateDownloadLimit(String fileId,
                                    UpdateDownloadLimitRequest request) {
        FileMetadata file = findFileOrThrow(fileId);
        file.setDownloadLimit(request.getDownloadLimit());
        fileMetadataRepository.save(file);
        log.info("[AdminService] Download limit updated for: {} → {}",
                fileId, request.getDownloadLimit());
    }

    @Override
    public void resendEmail(String fileId) {
        FileMetadata file = findFileOrThrow(fileId);

        if (file.getIsDeleted()) {
            throw new CloudBridgeException(
                    "Cannot resend email for deleted file",
                    HttpStatus.BAD_REQUEST,
                    "FILE_DELETED"
            );
        }

        // Regenerate token + send email
        String downloadUrl = tokenService.generateDownloadToken(file);
        emailService.sendDownloadLinkToReceiver(file, downloadUrl,
                file.getPasswordHash() != null);

        log.info("[AdminService] Email resent for file: {} → {}",
                fileId, file.getReceiverEmail());
    }

    // ── STORAGE MANAGEMENT ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdminProviderResponse> getAllProviders() {
        List<AdminProviderResponse> result = new ArrayList<>();

        // Static providers
        capacityRepository.findAll().forEach(c ->
                result.add(toAdminProviderResponse(c)));

        // Dynamic providers
        dynamicProviderRepository.findAll().forEach(d ->
                result.add(toAdminProviderResponseDynamic(d)));

        return result;
    }

    @Override
    @Transactional
    public void updateStorageLimit(String provider,
                                   UpdateStorageLimitRequest request) {
        CloudProvider cloudProvider = CloudProvider.valueOf(
                provider.toUpperCase());

        CloudStorageCapacity capacity = capacityRepository
                .findByProvider(cloudProvider)
                .orElseThrow(() -> new CloudBridgeException(
                        "Provider not found: " + provider,
                        HttpStatus.NOT_FOUND,
                        "PROVIDER_NOT_FOUND"
                ));

        capacity.setTotalLimitBytes(request.getLimitBytes());
        capacityRepository.save(capacity);

        log.info("[AdminService] Storage limit updated | provider: {} → {} bytes",
                provider, request.getLimitBytes());
    }

    @Override
    @Transactional
    public void toggleProvider(String provider, ToggleProviderRequest request) {
        CloudProvider cloudProvider = CloudProvider.valueOf(
                provider.toUpperCase());

        CloudStorageCapacity capacity = capacityRepository
                .findByProvider(cloudProvider)
                .orElseThrow(() -> new CloudBridgeException(
                        "Provider not found: " + provider,
                        HttpStatus.NOT_FOUND,
                        "PROVIDER_NOT_FOUND"
                ));

        capacity.setIsEnabled(request.getEnabled());
        capacityRepository.save(capacity);

        log.info("[AdminService] Provider {} | enabled: {} | reason: {}",
                provider, request.getEnabled(), request.getReason());
    }

    @Override
    @Transactional
    public AdminProviderResponse addDynamicProvider(
            AddDynamicProviderRequest request) {

        // Check duplicate
        if (dynamicProviderRepository
                .findByProviderKey(request.getProviderKey()).isPresent()) {
            throw new CloudBridgeException(
                    "Provider key already exists: " + request.getProviderKey(),
                    HttpStatus.CONFLICT,
                    "PROVIDER_EXISTS"
            );
        }

        // Build and register S3-compatible client at runtime
        StorageService dynamicService = buildS3CompatibleService(request);
        storageServiceFactory.registerDynamicProvider(
                request.getProviderKey(), dynamicService);

        // Save to DB
        DynamicCloudProvider dynamic = DynamicCloudProvider.builder()
                .providerKey(request.getProviderKey())
                .displayName(request.getDisplayName())
                .providerType(request.getProviderType())
                .credentialsEncrypted(
                        encryptCredentials(request.getAccessKey(),
                                request.getSecretKey()))
                .region(request.getRegion())
                .bucketName(request.getBucketName())
                .endpointUrl(request.getEndpointUrl())
                .totalLimitBytes(request.getTotalLimitBytes())
                .usedBytes(0L)
                .isEnabled(true)
                .build();

        dynamicProviderRepository.save(dynamic);

        log.info("[AdminService] Dynamic provider added: {}",
                request.getProviderKey());

        return toAdminProviderResponseDynamic(dynamic);
    }

    @Override
    @Transactional
    public void toggleDynamicProvider(String providerKey,
                                      ToggleProviderRequest request) {
        DynamicCloudProvider provider = dynamicProviderRepository
                .findByProviderKey(providerKey)
                .orElseThrow(() -> new CloudBridgeException(
                        "Dynamic provider not found: " + providerKey,
                        HttpStatus.NOT_FOUND,
                        "PROVIDER_NOT_FOUND"
                ));

        provider.setIsEnabled(request.getEnabled());
        dynamicProviderRepository.save(provider);

        log.info("[AdminService] Dynamic provider {} | enabled: {}",
                providerKey, request.getEnabled());
    }

    @Override
    @Transactional
    public void deleteDynamicProvider(String providerKey) {
        DynamicCloudProvider provider = dynamicProviderRepository
                .findByProviderKey(providerKey)
                .orElseThrow(() -> new CloudBridgeException(
                        "Dynamic provider not found: " + providerKey,
                        HttpStatus.NOT_FOUND,
                        "PROVIDER_NOT_FOUND"
                ));

        dynamicProviderRepository.delete(provider);
        storageServiceFactory.unregisterProvider(providerKey);

        log.info("[AdminService] Dynamic provider deleted: {}", providerKey);
    }

    // ── ANALYTICS ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        long totalFiles   = fileMetadataRepository.count();
        long activeFiles  = fileMetadataRepository.findByIsDeletedFalse().size();
        long deletedFiles = totalFiles - activeFiles;

        long totalDownloads = downloadLogRepository.count();
        long uniqueDownloaders = downloadLogRepository
                .findAllUniqueDownloaderEmails().size();
        long uniqueSenders = fileMetadataRepository
                .findAllSenderEmails().size();

        LocalDateTime now = LocalDateTime.now();

        long expiringIn24h = fileMetadataRepository
                .findExpiredFiles(now.plusHours(24)).size();
        long expiringIn7d = fileMetadataRepository
                .findExpiredFiles(now.plusDays(7)).size();

        List<String> topSenders = fileMetadataRepository
                .findAllSenderEmails()
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        List<String> topDownloaders = downloadLogRepository
                .findAllUniqueDownloaderEmails()
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        return AdminAnalyticsResponse.builder()
                .totalFilesUploaded(totalFiles)
                .totalActiveFiles(activeFiles)
                .totalDeletedFiles(deletedFiles)
                .totalDownloads(totalDownloads)
                .totalUniqueDownloaders(uniqueDownloaders)
                .totalUniqueSenders(uniqueSenders)
                .filesExpiringInNext24Hours(expiringIn24h)
                .filesExpiringInNext7Days(expiringIn7d)
                .topSenderEmails(topSenders)
                .topDownloaderEmails(topDownloaders)
                .build();
    }

    @Override
    public List<String> getAllSenderEmails() {
        return fileMetadataRepository.findAllSenderEmails();
    }

    @Override
    public List<String> getAllDownloaderEmails() {
        return downloadLogRepository.findAllUniqueDownloaderEmails();
    }

    // ── SYSTEM ─────────────────────────────────────────────────────────────────

    @Override
    public void triggerManualCleanup() {
        log.info("[AdminService] Manual cleanup triggered by admin");
        fileCleanupScheduler.runNightlyCleanup();
    }

    @Override
    public void triggerCapacityRecalibration() {
        log.info("[AdminService] Manual recalibration triggered by admin");
        storageCapacityService.recalculateAllProviders();
    }

    @Override
    public Map<String, Boolean> checkProvidersHealth() {
        Map<String, Boolean> health = new LinkedHashMap<>();

        // Check static providers
        for (CloudProvider provider : CloudProvider.values()) {
            try {
                StorageService service = storageServiceFactory
                        .getProvider(provider.name());
                // Lightweight existence check on a dummy key
                service.fileExists("health-check-probe");
                health.put(provider.name(), true);
            } catch (Exception e) {
                health.put(provider.name(), false);
            }
        }

        // Check dynamic providers
        dynamicProviderRepository.findByIsEnabledTrue().forEach(d -> {
            try {
                StorageService service = storageServiceFactory
                        .getProvider(d.getProviderKey());
                service.fileExists("health-check-probe");
                health.put(d.getProviderKey(), true);
            } catch (Exception e) {
                health.put(d.getProviderKey(), false);
            }
        });

        return health;
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private FileMetadata findFileOrThrow(String fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    private AdminFileResponse toAdminFileResponse(FileMetadata f) {
        boolean expired     = f.getExpiryDate().isBefore(LocalDateTime.now());
        boolean limitReached = f.getDownloadCount() >= f.getDownloadLimit();
        int remaining       = Math.max(0,
                f.getDownloadLimit() - f.getDownloadCount());

        return AdminFileResponse.builder()
                .fileId(f.getId())
                .fileKey(f.getFileKey())
                .originalName(f.getOriginalName())
                .originalSize(f.getOriginalSize())
                .compressedSize(f.getCompressedSize())
                .formattedOriginalSize(formatSize(f.getOriginalSize()))
                .formattedCompressedSize(formatSize(f.getCompressedSize()))
                .compressionSavings(calcSavings(f.getOriginalSize(),
                        f.getCompressedSize()))
                .compressionFormat(f.getCompressionFormat())
                .encryptionAlgorithm(f.getEncryptionAlgorithm())
                .cloudProvider(f.getCloudProvider().name()) //
                .storagePath(f.getStoragePath())
                .senderEmail(f.getSenderEmail())
                .receiverEmail(f.getReceiverEmail())
                .passwordProtected(f.getPasswordHash() != null)
                .downloadCount(f.getDownloadCount())
                .uniqueDownloadCount(f.getUniqueDownloadCount())
                .downloadLimit(f.getDownloadLimit())
                .downloadsRemaining(remaining)
                .expiryDate(f.getExpiryDate())
                .createdAt(f.getCreatedAt())
                .deletedAt(f.getDeletedAt())
                .isDeleted(f.getIsDeleted())
                .isExpired(expired)
                .limitReached(limitReached)
                .build();
    }

    private AdminProviderResponse toAdminProviderResponse(
            CloudStorageCapacity c) {
        double usagePct = c.getTotalLimitBytes() > 0
                ? (c.getUsedBytes() * 100.0) / c.getTotalLimitBytes()
                : 0;

        return AdminProviderResponse.builder()
                .providerKey(c.getProvider().name())
                .displayName(c.getDisplayName() != null
                        ? c.getDisplayName() : c.getProvider().name())
                .providerType("STATIC")
                .isEnabled(c.getIsEnabled())
                .totalLimitBytes(c.getTotalLimitBytes())
                .usedBytes(c.getUsedBytes())
                .availableBytes(c.getAvailableBytes())
                .usagePercentage(Math.round(usagePct * 10.0) / 10.0)
                .formattedTotal(formatSize(c.getTotalLimitBytes()))
                .formattedUsed(formatSize(c.getUsedBytes()))
                .formattedAvailable(formatSize(c.getAvailableBytes()))
                .isDynamic(false)
                .build();
    }

    private AdminProviderResponse toAdminProviderResponseDynamic(
            DynamicCloudProvider d) {
        double usagePct = d.getTotalLimitBytes() > 0
                ? (d.getUsedBytes() * 100.0) / d.getTotalLimitBytes()
                : 0;

        return AdminProviderResponse.builder()
                .providerKey(d.getProviderKey())
                .displayName(d.getDisplayName())
                .providerType(d.getProviderType().name())
                .isEnabled(d.getIsEnabled())
                .totalLimitBytes(d.getTotalLimitBytes())
                .usedBytes(d.getUsedBytes())
                .availableBytes(d.getAvailableBytes())
                .usagePercentage(Math.round(usagePct * 10.0) / 10.0)
                .formattedTotal(formatSize(d.getTotalLimitBytes()))
                .formattedUsed(formatSize(d.getUsedBytes()))
                .formattedAvailable(formatSize(d.getAvailableBytes()))
                .isDynamic(true)
                .build();
    }

    private StorageService buildS3CompatibleService(
            AddDynamicProviderRequest request) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                request.getAccessKey(), request.getSecretKey());

        S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(request.getEndpointUrl()))
                .region(Region.of(request.getRegion() != null
                        ? request.getRegion() : "us-east-1"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();

        String providerKey  = request.getProviderKey();
        String bucketName   = request.getBucketName();

        // Return anonymous StorageService backed by this S3 client
        return new StorageService() {
            @Override
            public String uploadFile(String fileKey, java.io.InputStream in,
                                     long fileSize, String contentType) {
                s3Client.putObject(
                        software.amazon.awssdk.services.s3.model
                                .PutObjectRequest.builder()
                                .bucket(bucketName).key(fileKey)
                                .contentType(contentType)
                                .contentLength(fileSize).build(),
                        software.amazon.awssdk.core.sync.RequestBody
                                .fromInputStream(in, fileSize));
                return fileKey;
            }

            @Override
            public java.io.InputStream downloadFile(String storagePath) {
                return s3Client.getObject(
                        software.amazon.awssdk.services.s3.model
                                .GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(storagePath).build());
            }

            @Override
            public void deleteFile(String storagePath) {
                s3Client.deleteObject(
                        software.amazon.awssdk.services.s3.model
                                .DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(storagePath).build());
            }

            @Override
            public boolean fileExists(String storagePath) {
                try {
                    s3Client.headObject(
                            software.amazon.awssdk.services.s3.model
                                    .HeadObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(storagePath).build());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public String getProviderName() {
                return providerKey;
            }
        };
    }

    private String encryptCredentials(String accessKey, String secretKey) {
        // Simple encoding for now — replace with AES encryption in production
        return Base64.getEncoder().encodeToString(
                (accessKey + ":" + secretKey).getBytes());
    }

    private String formatSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String calcSavings(Long original, Long compressed) {
        if (original == null || compressed == null || original == 0)
            return "N/A";
        double saved = ((double)(original - compressed) / original) * 100;
        return saved <= 0 ? "No reduction"
                : String.format("Saved %.1f%%", saved);
    }
}