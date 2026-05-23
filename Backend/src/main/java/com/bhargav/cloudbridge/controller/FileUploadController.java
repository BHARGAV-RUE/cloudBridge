package com.bhargav.cloudbridge.controller;

import com.bhargav.cloudbridge.dto.request.FileUploadRequest;
import com.bhargav.cloudbridge.dto.response.ApiResponse;
import com.bhargav.cloudbridge.dto.response.FileUploadResponse;
import com.bhargav.cloudbridge.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * POST /api/files/upload
     *
     * Accepts multipart/form-data:
     *   - file: the binary file
     *   - senderEmail: optional
     *   - receiverEmail: required
     *   - password: optional
     *   - compressionFormat: ZIP | SEVEN_Z | TAR | GZIP | BZIP2 | XZ
     *   - encryptionAlgorithm: AES_GCM | AES_CBC (default AES_GCM)
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") @Valid FileUploadRequest request) {

        log.info("[UploadController] Upload request received | file: {} | size: {}",
                file.getOriginalFilename(), file.getSize());

        FileUploadResponse response = fileUploadService.uploadFile(file, request);

        return ResponseEntity.ok(
                ApiResponse.success("File uploaded successfully", response)
        );
    }
}