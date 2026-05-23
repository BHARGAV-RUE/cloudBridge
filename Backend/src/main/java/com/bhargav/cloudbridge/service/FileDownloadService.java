package com.bhargav.cloudbridge.service;

import com.bhargav.cloudbridge.dto.request.FileDownloadRequest;
import com.bhargav.cloudbridge.dto.response.FileInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface FileDownloadService {

    /**
     * Returns public file info shown on download page
     * before user enters email or password.
     * No authentication needed — just token in URL.
     *
     * @param token secure token from URL
     * @return FileInfoResponse with name, size, expiry, passwordRequired
     */
    FileInfoResponse getFileInfo(String token);

    /**
     * Full download pipeline:
     * validate → collect email → check password
     * → fetch from cloud → decrypt → decompress
     * → stream to user → log download
     *
     * @param token           secure token from URL
     * @param request         receiver email + optional password
     * @param httpRequest     for IP + device info extraction
     * @param httpResponse    for streaming file back to frontend
     */
    void downloadFile(String token,
                      FileDownloadRequest request,
                      HttpServletRequest httpRequest,
                      HttpServletResponse httpResponse);
}