package com.bhargav.cloudbridge.exception;

import org.springframework.http.HttpStatus;

public class StorageFullException extends CloudBridgeException {

    public StorageFullException(long requiredBytes) {
        super(
                "No cloud provider has enough capacity for file size: "
                        + requiredBytes + " bytes",
                HttpStatus.INSUFFICIENT_STORAGE,
                "STORAGE_FULL"
        );
    }

    public StorageFullException() {
        super(
                "All cloud providers are at capacity",
                HttpStatus.INSUFFICIENT_STORAGE,
                "STORAGE_FULL"
        );
    }
}