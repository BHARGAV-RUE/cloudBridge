package com.bhargav.cloudbridge.entity;

public enum DownloadStatus {
    SUCCESS,
    FAILED_WRONG_PASSWORD,
    FAILED_EXPIRED,
    FAILED_LIMIT_REACHED,
    FAILED_INVALID_TOKEN
}
