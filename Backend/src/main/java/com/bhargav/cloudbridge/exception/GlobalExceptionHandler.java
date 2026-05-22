package com.bhargav.cloudbridge.exception;

import com.bhargav.cloudbridge.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── CLOUDBRIDGE BASE EXCEPTIONS ────────────────────────────────────────────

    @ExceptionHandler(CloudBridgeException.class)
    public ResponseEntity<ApiResponse<Void>> handleCloudBridgeException(
            CloudBridgeException ex) {

        log.error("[Exception] {} | code: {} | message: {}",
                ex.getClass().getSimpleName(),
                ex.getErrorCode(),
                ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── FILE NOT FOUND ─────────────────────────────────────────────────────────

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileNotFoundException(
            FileNotFoundException ex) {

        log.warn("[Exception] FileNotFound: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── TOKEN EXPIRED ──────────────────────────────────────────────────────────

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpiredException(
            TokenExpiredException ex) {

        log.warn("[Exception] TokenExpired: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── STORAGE FULL ───────────────────────────────────────────────────────────

    @ExceptionHandler(StorageFullException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageFullException(
            StorageFullException ex) {

        log.error("[Exception] StorageFull: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INSUFFICIENT_STORAGE)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── INVALID PASSWORD ───────────────────────────────────────────────────────

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPasswordException(
            InvalidPasswordException ex) {

        log.warn("[Exception] InvalidPassword: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── DOWNLOAD LIMIT REACHED ─────────────────────────────────────────────────

    @ExceptionHandler(DownloadLimitReachedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDownloadLimitReachedException(
            DownloadLimitReachedException ex) {

        log.warn("[Exception] DownloadLimitReached: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── VALIDATION ERRORS (@Valid) ─────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Collect all field errors into one readable message
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[Exception] Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Validation failed: " + errors));
    }

    // ── FILE TOO LARGE ─────────────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {

        log.warn("[Exception] File too large: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.failure(
                        "File size exceeds maximum allowed limit of 5GB"));
    }

    // ── ILLEGAL ARGUMENT ───────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("[Exception] IllegalArgument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── CATCH ALL ──────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {

        log.error("[Exception] Unhandled exception: {} | message: {}",
                ex.getClass().getSimpleName(),
                ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        "Something went wrong. Please try again later."));
    }
}