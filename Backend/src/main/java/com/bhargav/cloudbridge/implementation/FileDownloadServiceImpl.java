package com.bhargav.cloudbridge.implementation;

import com.bhargav.cloudbridge.dto.request.FileDownloadRequest;
import com.bhargav.cloudbridge.dto.response.FileInfoResponse;
import com.bhargav.cloudbridge.entity.*;
import com.bhargav.cloudbridge.exception.FileNotFoundException;
import com.bhargav.cloudbridge.exception.InvalidPasswordException;
import com.bhargav.cloudbridge.exception.DownloadLimitReachedException;
import com.bhargav.cloudbridge.repository.DownloadLogRepository;
import com.bhargav.cloudbridge.repository.FileMetadataRepository;
import com.bhargav.cloudbridge.service.FileDownloadService;
import com.bhargav.cloudbridge.service.compression.CompressionService;
import com.bhargav.cloudbridge.service.encryption.EncryptionService;
import com.bhargav.cloudbridge.service.storage.StorageService;
import com.bhargav.cloudbridge.service.storage.StorageServiceFactory;
import com.bhargav.cloudbridge.service.storage.StorageCapacityService;
import com.bhargav.cloudbridge.service.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private final TokenService tokenService;
    private final StorageServiceFactory storageServiceFactory;
    private final StorageCapacityService storageCapacityService;
    private final CompressionService compressionService;
    private final EncryptionService encryptionService;
    private final FileMetadataRepository fileMetadataRepository;
    private final DownloadLogRepository downloadLogRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int BUFFER_SIZE = 8192; // 8KB stream buffer

    // ── GET FILE INFO ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public FileInfoResponse getFileInfo(String token) {

        // Validate token — throws TokenExpiredException if invalid
        DownloadToken downloadToken = tokenService.validateToken(token);
        FileMetadata file = downloadToken.getFile();

        boolean isExpired = file.getExpiryDate()
                .isBefore(java.time.LocalDateTime.now());

        boolean limitReached = file.getDownloadCount()
                >= file.getDownloadLimit();

        int downloadsRemaining = Math.max(0,
                file.getDownloadLimit() - file.getDownloadCount());

        return FileInfoResponse.builder()
                .fileId(file.getId())
                .originalName(file.getOriginalName())
                .originalSize(file.getOriginalSize())
                .formattedSize(formatFileSize(file.getOriginalSize()))
                .expiryDate(file.getExpiryDate())
                .downloadsRemaining(downloadsRemaining)
                .passwordRequired(file.getPasswordHash() != null)
                .isExpired(isExpired)
                .limitReached(limitReached)
                .cloudProvider(file.getCloudProvider().name())
                .build();
    }

    // ── DOWNLOAD PIPELINE ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void downloadFile(String token,
                             FileDownloadRequest request,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {

        // Step 1 — Validate token
        DownloadToken downloadToken = tokenService.validateToken(token);
        FileMetadata file = downloadToken.getFile();

        log.info("[DownloadService] Download request | file: {} | receiver: {}",
                file.getId(), request.getReceiverEmail());

        // Step 2 — Check password if file is protected
        validatePassword(file, request.getPassword());

        // Step 3 — Check download limit one more time (race condition safety)
        if (file.getDownloadCount() >= file.getDownloadLimit()) {
            logDownload(file, request, httpRequest,
                    DownloadStatus.FAILED_LIMIT_REACHED);
            throw new DownloadLimitReachedException();
        }

        try {
            // Step 4 — Fetch from cloud
            InputStream encryptedStream = fetchFromCloud(file);
            log.info("[DownloadService] Fetched from cloud: {}",
                    file.getCloudProvider());

            // Step 5 — Decrypt
            InputStream decryptedStream = encryptionService.decrypt(
                    encryptedStream,
                    file.getEncryptionAlgorithm(),
                    file.getEncryptionIv(),
                    request.getPassword()  // null if no password
            );
            log.info("[DownloadService] Decryption complete");

            // Step 6 — Decompress
            InputStream rawStream = compressionService.decompress(
                    decryptedStream,
                    file.getCompressionFormat()
            );
            log.info("[DownloadService] Decompression complete");

            // Step 7 — Set response headers for file download
            setDownloadHeaders(httpResponse, file);

            // Step 8 — Stream to user
            streamToResponse(rawStream, httpResponse);
            log.info("[DownloadService] File streamed to user: {}",
                    request.getReceiverEmail());

            // Step 9 — Update download counts
            updateDownloadCounts(file, request.getReceiverEmail());

            // Step 10 — Log successful download
            logDownload(file, request, httpRequest, DownloadStatus.SUCCESS);

        } catch (InvalidPasswordException | DownloadLimitReachedException e) {
            throw e;

        } catch (Exception e) {
            log.error("[DownloadService] Download failed | reason: {}",
                    e.getMessage(), e);

            logDownload(file, request, httpRequest, DownloadStatus.FAILED_EXPIRED);

            throw new com.bhargav.cloudbridge.exception.CloudBridgeException(
                    "Download failed: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // ── STEP METHODS ───────────────────────────────────────────────────────────

    private void validatePassword(FileMetadata file, String inputPassword) {
        // File has no password — skip check
        if (file.getPasswordHash() == null || file.getPasswordHash().isBlank()) {
            return;
        }

        // File has password but none provided
        if (inputPassword == null || inputPassword.isBlank()) {
            throw new InvalidPasswordException();
        }

        // Password provided but wrong
        if (!passwordEncoder.matches(inputPassword, file.getPasswordHash())) {
            log.warn("[DownloadService] Wrong password for file: {}", file.getId());
            throw new InvalidPasswordException();
        }

        log.info("[DownloadService] Password verified for file: {}", file.getId());
    }

    private InputStream fetchFromCloud(FileMetadata file) {
        StorageService storageService = storageServiceFactory
                .getProvider(file.getCloudProvider().name());

        if (!storageService.fileExists(file.getStoragePath())) {
            log.error("[DownloadService] File not found in cloud: {}",
                    file.getStoragePath());
            throw new FileNotFoundException(file.getId());
        }

        return storageService.downloadFile(file.getStoragePath());
    }

    private void setDownloadHeaders(HttpServletResponse httpResponse,
                                    FileMetadata file) {
        httpResponse.setContentType("application/octet-stream");
        httpResponse.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + file.getOriginalName() + "\""
        );
        if (file.getOriginalSize() != null) {
            httpResponse.setContentLengthLong(file.getOriginalSize());
        }
        // Allow frontend to read headers
        httpResponse.setHeader("Access-Control-Expose-Headers",
                "Content-Disposition");
    }

    private void streamToResponse(InputStream inputStream,
                                  HttpServletResponse httpResponse) throws Exception {
        try (InputStream in = inputStream;
             OutputStream out = httpResponse.getOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        }
    }

    private void updateDownloadCounts(FileMetadata file, String receiverEmail) {

        // Increment total download count
        file.setDownloadCount(file.getDownloadCount() + 1);

        // Check if this email is downloading for the first time
        boolean isUnique = !downloadLogRepository
                .existsByFile_IdAndReceiverEmail(file.getId(), receiverEmail);

        if (isUnique) {
            file.setUniqueDownloadCount(file.getUniqueDownloadCount() + 1);
            log.info("[DownloadService] New unique downloader: {}", receiverEmail);
        }

        fileMetadataRepository.save(file);

        log.info("[DownloadService] Download count updated | total: {} | unique: {}",
                file.getDownloadCount(), file.getUniqueDownloadCount());
    }

    private void logDownload(FileMetadata file,
                             FileDownloadRequest request,
                             HttpServletRequest httpRequest,
                             DownloadStatus status) {
        try {
            boolean isUnique = !downloadLogRepository
                    .existsByFile_IdAndReceiverEmail(
                            file.getId(),
                            request.getReceiverEmail()
                    );

            DownloadLog downloadLog = DownloadLog.builder()
                    .file(file)
                    .receiverEmail(request.getReceiverEmail())
                    .isUniqueDownloader(isUnique)
                    .ipAddress(extractIpAddress(httpRequest))
                    .deviceInfo(extractDeviceInfo(httpRequest))
                    .downloadStatus(status)
                    .build();

            downloadLogRepository.save(downloadLog);

        } catch (Exception e) {
            // Log failure should never break the download flow
            log.error("[DownloadService] Failed to save download log: {}",
                    e.getMessage());
        }
    }

    // ── UTILITY ────────────────────────────────────────────────────────────────

    private String extractIpAddress(HttpServletRequest request) {
        // Check for proxy headers first
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs — take the first
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddr();
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) return "Unknown";
        // Truncate to avoid storing huge UA strings in DB
        return userAgent.length() > 255
                ? userAgent.substring(0, 255)
                : userAgent;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}