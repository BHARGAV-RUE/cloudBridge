package com.bhargav.cloudbridge.dto.response;

import com.bhargav.cloudbridge.entity.DownloadStatus;
import com.bhargav.cloudbridge.entity.FileMetadata;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DownloadValidationResult {

    // Is download allowed to proceed?
    private boolean valid;

    // If not valid — why?
    private DownloadStatus failureReason;

    // If valid — the file to serve
    private FileMetadata file;

    // Convenience message for frontend
    private String message;

    // ── STATIC FACTORIES ───────────────────────────────────────────────────────

    public static DownloadValidationResult success(FileMetadata file) {
        return DownloadValidationResult.builder()
                .valid(true)
                .file(file)
                .message("Download authorized")
                .build();
    }

    public static DownloadValidationResult failure(DownloadStatus reason,
                                                   String message) {
        return DownloadValidationResult.builder()
                .valid(false)
                .failureReason(reason)
                .message(message)
                .build();
    }
}