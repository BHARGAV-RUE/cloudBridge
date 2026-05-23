package com.bhargav.cloudbridge.dto.response;

import com.bhargav.cloudbridge.entity.CompressionFormat;
import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminFileResponse {

    private String fileId;
    private String fileKey;
    private String originalName;
    private Long originalSize;
    private Long compressedSize;
    private String formattedOriginalSize;
    private String formattedCompressedSize;
    private String compressionSavings;
    private CompressionFormat compressionFormat;
    private EncryptionAlgorithm encryptionAlgorithm;
    private String cloudProvider;
    private String storagePath;
    private String senderEmail;
    private String receiverEmail;
    private boolean passwordProtected;
    private Integer downloadCount;
    private Integer uniqueDownloadCount;
    private Integer downloadLimit;
    private Integer downloadsRemaining;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
    private boolean isExpired;
    private boolean limitReached;
}