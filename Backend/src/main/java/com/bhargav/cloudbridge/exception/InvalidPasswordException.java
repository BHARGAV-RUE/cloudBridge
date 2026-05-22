package com.bhargav.cloudbridge.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends CloudBridgeException {

    public InvalidPasswordException() {
        super(
                "Incorrect password for this file",
                HttpStatus.UNAUTHORIZED,
                "INVALID_PASSWORD"
        );
    }
}