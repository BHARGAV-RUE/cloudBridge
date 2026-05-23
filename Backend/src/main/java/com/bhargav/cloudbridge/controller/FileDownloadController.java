package com.bhargav.cloudbridge.controller;

import com.bhargav.cloudbridge.dto.request.FileDownloadRequest;
import com.bhargav.cloudbridge.dto.response.ApiResponse;
import com.bhargav.cloudbridge.dto.response.FileInfoResponse;
import com.bhargav.cloudbridge.service.FileDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    /**
     * GET /api/files/info/{token}
     *
     * Returns public file info shown on download page.
     * Frontend calls this first to show file name,
     * size, expiry, and whether password is required.
     * No email or password needed for this endpoint.
     */
    @GetMapping("/info/{token}")
    public ResponseEntity<ApiResponse<FileInfoResponse>> getFileInfo(
            @PathVariable String token) {

        log.info("[DownloadController] File info request | token: {}",
                token.substring(0, 8) + "...");

        FileInfoResponse info = fileDownloadService.getFileInfo(token);

        return ResponseEntity.ok(
                ApiResponse.success("File info retrieved", info)
        );
    }

    /**
     * POST /api/files/download/{token}
     *
     * Full download pipeline.
     * Frontend sends receiver email + optional password.
     * Response streams the raw file as binary.
     *
     * Note: Returns void — file is written directly
     * to HttpServletResponse output stream.
     */
    @PostMapping("/download/{token}")
    public void downloadFile(
            @PathVariable String token,
            @RequestBody @Valid FileDownloadRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("[DownloadController] Download request | token: {} | email: {}",
                token.substring(0, 8) + "...",
                request.getReceiverEmail());

        fileDownloadService.downloadFile(
                token, request, httpRequest, httpResponse);
    }
}