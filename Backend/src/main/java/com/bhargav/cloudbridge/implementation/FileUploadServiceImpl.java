package com.bhargav.cloudbridge.implementation;

import com.bhargav.cloudbridge.dto.request.FileUploadRequest;
import com.bhargav.cloudbridge.dto.response.FileUploadResponse;
import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;

import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import com.bhargav.cloudbridge.entity.FileMetadata;
import com.bhargav.cloudbridge.exception.CloudBridgeException;
import com.bhargav.cloudbridge.exception.StorageFullException;
import com.bhargav.cloudbridge.repository.FileMetadataRepository;
import com.bhargav.cloudbridge.service.FileUploadService;
import com.bhargav.cloudbridge.service.email.EmailService;
import com.bhargav.cloudbridge.service.compression.CompressionResult;
import com.bhargav.cloudbridge.service.compression.CompressionService;
import com.bhargav.cloudbridge.service.encryption.EncryptionResult;
import com.bhargav.cloudbridge.service.encryption.EncryptionService;
import com.bhargav.cloudbridge.service.storage.StorageCapacityService;
import com.bhargav.cloudbridge.service.storage.StorageService;
import com.bhargav.cloudbridge.service.storage.StorageServiceFactory;
import com.bhargav.cloudbridge.service.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final CompressionService compressionService;
    private final EncryptionService encryptionService;
    private final StorageServiceFactory storageServiceFactory;
    private final StorageCapacityService storageCapacityService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final FileMetadataRepository fileMetadataRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${cloudbridge.file.expiry-days}")
    private int expiryDays;

    @Value("${cloudbridge.file.max-downloads}")
    private int maxDownloads;

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB

    // ── MAIN PIPELINE ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file,
                                         FileUploadRequest request) {

        log.info("[UploadService] Starting upload | file: {} | receiver: {}",
                file.getOriginalFilename(), request.getReceiverEmail());

        // Step 1 — Validate
        validateFile(file);

        // Step 2 — Generate unique file key
        String fileKey = generateFileKey(file.getOriginalFilename());
        log.info("[UploadService] FileKey generated: {}", fileKey);

        try {
            // Step 3 — Compress
            CompressionResult compressed = compressFile(file, request);
            log.info("[UploadService] Compression complete | format: {} | size: {} bytes",
                    request.getCompressionFormat(),
                    compressed.getCompressedSize());

            // Step 4 — Encrypt
            EncryptionAlgorithm algorithm = request.getEncryptionAlgorithm() != null
                    ? request.getEncryptionAlgorithm()
                    : EncryptionAlgorithm.AES_GCM;

            EncryptionResult encrypted = encryptFile(
                    compressed.getCompressedStream(),
                    algorithm,
                    request.getPassword()
            );
            log.info("[UploadService] Encryption complete | algorithm: {} | iv: {}",
                    algorithm, encrypted.getIvHex());

            // Step 5 — Select best provider by available capacity
            StorageService storageService = selectBestProvider(
                    encrypted.getEncryptedSize());
            String providerName = storageService.getProviderName();
            log.info("[UploadService] Provider selected: {}", providerName);

            // Step 6 — Upload to cloud
            String storagePath = storageService.uploadFile(
                    fileKey,
                    encrypted.getEncryptedStream(),
                    encrypted.getEncryptedSize(),
                    "application/octet-stream"
            );
            log.info("[UploadService] Cloud upload complete | path: {}", storagePath);

            // Step 7 — Update provider capacity
            CloudProvider provider = CloudProvider.valueOf(providerName);
            storageCapacityService.incrementUsage(provider,
                    encrypted.getEncryptedSize());

            // Step 8 — Hash password if set
            String passwordHash = null;
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                passwordHash = passwordEncoder.encode(request.getPassword());
            }

            // Step 9 — Save metadata to PostgreSQL
            FileMetadata metadata = buildFileMetadata(
                    file, request, fileKey,
                    compressed, encrypted,
                    storagePath, providerName, passwordHash
            );
            fileMetadataRepository.save(metadata);
            log.info("[UploadService] Metadata saved | fileId: {}", metadata.getId());

            // Step 10 — Generate download token + URL
            String downloadUrl = tokenService.generateDownloadToken(metadata);
            log.info("[UploadService] Token generated | url: {}", downloadUrl);

            // Step 11 — Send emails
            boolean hasPassword = passwordHash != null;
            emailService.sendDownloadLinkToReceiver(metadata, downloadUrl, hasPassword);
            emailService.sendUploadConfirmationToSender(metadata, downloadUrl);

            // Step 12 — Build and return response
            return buildUploadResponse(metadata, downloadUrl, file.getSize());

        } catch (CloudBridgeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[UploadService] Upload failed | reason: {}", e.getMessage(), e);
            throw new CloudBridgeException(
                    "Upload failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // ── PROVIDER SELECTION ─────────────────────────────────────────────────────

    private StorageService selectBestProvider(long requiredBytes) {
        List<CloudStorageCapacity> capacities = storageCapacityService
                .getAllCapacities();

        return capacities.stream()
                .filter(c -> c.getIsEnabled() && c.hasCapacity(requiredBytes))
                .sorted(Comparator.comparingLong(
                        CloudStorageCapacity::getAvailableBytes).reversed())
                .map(c -> storageServiceFactory.getProvider(c.getProvider().name()))
                .findFirst()
                .orElseThrow(() -> new StorageFullException(requiredBytes));
    }

    // ── STEP METHODS ───────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CloudBridgeException(
                    "File is empty or missing",
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_FILE"
            );
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CloudBridgeException(
                    "File size exceeds 5GB limit",
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE"
            );
        }
        log.info("[UploadService] File validated | name: {} | size: {} bytes",
                file.getOriginalFilename(), file.getSize());
    }

    private CompressionResult compressFile(MultipartFile file,
                                           FileUploadRequest request) throws Exception {
        return compressionService.compress(
                file.getInputStream(),
                file.getOriginalFilename(),
                request.getCompressionFormat()
        );
    }

    private EncryptionResult encryptFile(InputStream compressedStream,
                                         EncryptionAlgorithm algorithm,
                                         String password) throws Exception {
        return encryptionService.encrypt(compressedStream, algorithm, password);
    }

    private String generateFileKey(String originalFilename) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String safeName = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        return "files/" + uuid + "/" + safeName;
    }

    // ── METADATA BUILDER ───────────────────────────────────────────────────────

    private FileMetadata buildFileMetadata(MultipartFile file,
                                           FileUploadRequest request,
                                           String fileKey,
                                           CompressionResult compressed,
                                           EncryptionResult encrypted,
                                           String storagePath,
                                           String providerName,
                                           String passwordHash) {
        return FileMetadata.builder()
                .fileKey(fileKey)
                .originalName(file.getOriginalFilename())
                .originalSize(file.getSize())
                .compressedName(compressed.getCompressedFileName())
                .compressionFormat(request.getCompressionFormat())
                .compressedSize(compressed.getCompressedSize())
                .encryptionIv(encrypted.getIvHex())
                .encryptionAlgorithm(encrypted.getAlgorithm())
                .cloudProvider(CloudProvider.valueOf(providerName))
                .storagePath(storagePath)
                .passwordHash(passwordHash)
                .downloadLimit(maxDownloads)
                .downloadCount(0)
                .uniqueDownloadCount(0)
                .senderEmail(request.getSenderEmail())
                .receiverEmail(request.getReceiverEmail())
                .isDeleted(false)
                .build();
        // expiryDate set by @PrePersist — always 8 days from now
    }

    // ── RESPONSE BUILDER ───────────────────────────────────────────────────────

    private FileUploadResponse buildUploadResponse(FileMetadata metadata,
                                                   String downloadUrl,
                                                   long originalSize) {
        String savings = calculateSavings(originalSize, metadata.getCompressedSize());

        return FileUploadResponse.builder()
                .fileId(metadata.getId())
                .originalName(metadata.getOriginalName())
                .originalSize(metadata.getOriginalSize())
                .compressedSize(metadata.getCompressedSize())
                .compressionSavings(savings)
                .cloudProvider(metadata.getCloudProvider().name())
                .compressionFormat(metadata.getCompressionFormat())
                .encryptionAlgorithm(metadata.getEncryptionAlgorithm())
                .downloadUrl(downloadUrl)
                .expiryDate(metadata.getExpiryDate())
                .maxDownloads(metadata.getDownloadLimit())
                .receiverEmail(metadata.getReceiverEmail())
                .emailSent(metadata.getReceiverEmail() != null)
                .build();
    }

    private String calculateSavings(long originalSize, Long compressedSize) {
        if (compressedSize == null || originalSize == 0) return "N/A";
        double saved = ((double)(originalSize - compressedSize) / originalSize) * 100;
        if (saved <= 0) return "No reduction";
        return String.format("Saved %.1f%%", saved);
    }
}