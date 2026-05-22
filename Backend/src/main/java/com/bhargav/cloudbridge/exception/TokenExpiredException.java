package com.bhargav.cloudbridge.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends CloudBridgeException {

    public TokenExpiredException() {
        super(
                "Download link has expired or is invalid",
                HttpStatus.GONE,
                "TOKEN_EXPIRED"
        );
    }

    public TokenExpiredException(String token) {
        super(
                "Download link is invalid or has expired: " + token,
                HttpStatus.GONE,
                "TOKEN_EXPIRED"
        );
    }
}