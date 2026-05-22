package com.bhargav.cloudbridge.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FileInfoResponse {

    // Shown on the download page before user downloads
    private String fileId;
    private String originalName;
    private Long originalSize;
    private String formattedSize;       // e.g. "24.5 MB"

    // Download status info
    private LocalDateTime expiryDate;
    private Integer downloadsRemaining;
    private boolean passwordRequired;
    private boolean isExpired;
    private boolean limitReached;

    // Provider info — optional, shown for transparency
    private String cloudProvider;
}