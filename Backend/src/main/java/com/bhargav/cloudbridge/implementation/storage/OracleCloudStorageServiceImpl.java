package com.bhargav.cloudbridge.implementation.storage;

import com.bhargav.cloudbridge.service.storage.StorageService;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OracleCloudStorageServiceImpl implements StorageService {

    // Injected from OracleCloudConfig @Bean
    private final ObjectStorageClient objectStorageClient;

    @Value("${oracle.namespace}")
    private String namespace;

    @Value("${oracle.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFile(String fileKey,
                             InputStream inputStream,
                             long fileSize,
                             String contentType) {
        try {
            log.info("[OCI] Uploading → bucket: {} | key: {}", bucketName, fileKey);

            PutObjectRequest request = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(fileKey)
                    .contentLength(fileSize)
                    .contentType(contentType)
                    .putObjectBody(inputStream)
                    .build();

            objectStorageClient.putObject(request);

            log.info("[OCI] Upload complete: {}", fileKey);
            return fileKey;

        } catch (Exception e) {
            log.error("[OCI] Upload failed | key: {} | reason: {}",
                    fileKey, e.getMessage());
            throw new RuntimeException("Oracle Cloud upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        try {
            log.info("[OCI] Downloading → key: {}", storagePath);

            GetObjectRequest request = GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(storagePath)
                    .build();

            GetObjectResponse response = objectStorageClient.getObject(request);
            return response.getInputStream();

        } catch (Exception e) {
            log.error("[OCI] Download failed | key: {} | reason: {}",
                    storagePath, e.getMessage());
            throw new RuntimeException("Oracle Cloud download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            log.info("[OCI] Deleting → key: {}", storagePath);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(storagePath)
                    .build();

            objectStorageClient.deleteObject(request);
            log.info("[OCI] Delete complete: {}", storagePath);

        } catch (Exception e) {
            log.error("[OCI] Delete failed | key: {} | reason: {}",
                    storagePath, e.getMessage());
            throw new RuntimeException("Oracle Cloud delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(storagePath)
                    .build();

            objectStorageClient.headObject(request);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "ORACLE_CLOUD";
    }
}