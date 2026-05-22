package com.bhargav.cloudbridge.dto.response;

import com.bhargav.cloudbridge.entity.CompressionFormat;
import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FileUploadResponse {

    // File identity
    private String fileId;
    private String originalName;

    // Size comparison — shows compression benefit
    private Long originalSize;
    private Long compressedSize;
    private String compressionSavings;  // e.g. "Saved 42.3%"

    // Storage info
    private String cloudProvider;
    private CompressionFormat compressionFormat;
    private EncryptionAlgorithm encryptionAlgorithm;

    // Download info
    private String downloadUrl;
    private LocalDateTime expiryDate;
    private Integer maxDownloads;

    // Email info
    private String receiverEmail;
    private boolean emailSent;
}