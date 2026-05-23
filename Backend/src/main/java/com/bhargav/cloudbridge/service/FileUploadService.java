package com.bhargav.cloudbridge.service;

import com.bhargav.cloudbridge.dto.request.FileUploadRequest;
import com.bhargav.cloudbridge.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * Full upload pipeline:
     * validate → compress → encrypt → select provider
     * → upload → save metadata → generate token → send emails
     *
     * @param file    the multipart file from frontend
     * @param request sender/receiver details, compression format, password
     * @return FileUploadResponse with download URL and file details
     */
    FileUploadResponse uploadFile(MultipartFile file,
                                  FileUploadRequest request);
}