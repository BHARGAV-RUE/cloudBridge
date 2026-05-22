package com.bhargav.cloudbridge.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class CloudBridgeException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public CloudBridgeException(String message,
                                HttpStatus status,
                                String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public CloudBridgeException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "CLOUDBRIDGE_ERROR";
    }
}