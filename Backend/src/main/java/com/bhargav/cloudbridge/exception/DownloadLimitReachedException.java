package com.bhargav.cloudbridge.exception;

import org.springframework.http.HttpStatus;

public class DownloadLimitReachedException extends CloudBridgeException {

    public DownloadLimitReachedException() {
        super(
                "This file has reached its maximum download limit",
                HttpStatus.FORBIDDEN,
                "DOWNLOAD_LIMIT_REACHED"
        );
    }
}