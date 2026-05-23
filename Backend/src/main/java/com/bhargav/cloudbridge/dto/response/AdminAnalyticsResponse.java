package com.bhargav.cloudbridge.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AdminAnalyticsResponse {

    // Totals
    private long totalFilesUploaded;
    private long totalActiveFiles;
    private long totalDeletedFiles;
    private long totalDownloads;
    private long totalUniqueDownloaders;
    private long totalUniqueSenders;
    private long failedDownloadAttempts;

    // Expiry alerts
    private long filesExpiringInNext24Hours;
    private long filesExpiringInNext7Days;

    // Top lists
    private List<String> topSenderEmails;
    private List<String> topDownloaderEmails;
}