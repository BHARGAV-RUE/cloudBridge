package com.bhargav.cloudbridge.exception;

import org.springframework.http.HttpStatus;

public class FileNotFoundException extends CloudBridgeException {

    public FileNotFoundException(String fileId) {
        super(
                "File not found or has been deleted: " + fileId,
                HttpStatus.NOT_FOUND,
                "FILE_NOT_FOUND"
        );
    }

    public FileNotFoundException() {
        super(
                "File not found or has been deleted",
                HttpStatus.NOT_FOUND,
                "FILE_NOT_FOUND"
        );
    }
}