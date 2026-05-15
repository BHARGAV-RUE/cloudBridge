package com.bhargav.cloudbridge.implementation.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobClient;
import com.bhargav.cloudbridge.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureBlobStorageServiceImpl implements StorageService {

    private final BlobContainerClient blobContainerClient;

    @Override
    public String uploadFile(String fileKey,
                             InputStream inputStream,
                             long fileSize,
                             String contentType){
        try{
            log.info("[Azure Blob] uploading -> key: {}", fileKey);

            BlobClient blobClient = blobContainerClient.getBlobClient(fileKey);
            blobClient.upload(inputStream, fileSize, true);

            log.info("[Azure Blob] upload complete: {}", fileKey);
            return fileKey;
        }
        catch (Exception e){
            log.error("[Azure Blob] upload failed | key: {} | reason: {}", fileKey, e.getMessage());
            throw new RuntimeException("Azure Blob upload failed: "+ e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath){
        try {
            log.info("[Azure Blob] Downloading → key: {}", storagePath);

            return blobContainerClient.getBlobClient(storagePath).openInputStream();

        } catch (Exception e) {
            log.error("[Azure Blob] Download failed | key: {} | reason: {}",
                    storagePath, e.getMessage());
            throw new RuntimeException("Azure Blob download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            log.info("[Azure Blob] Deleting → key: {}", storagePath);

            blobContainerClient.getBlobClient(storagePath).delete();
            log.info("[Azure Blob] Delete complete: {}", storagePath);

        } catch (Exception e) {
            log.error("[Azure Blob] Delete failed | key: {} | reason: {}",
                    storagePath, e.getMessage());
            throw new RuntimeException("Azure Blob delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String storagePath){
        try{
            return blobContainerClient.getBlobClient(storagePath).exists();
        }
        catch (Exception e){
            log.warn("[Azure Blob] Existence check failed | key: {}", storagePath);
            return false;
        }
    }

    @Override
    public String getProviderName(){
        return "AZURE_BLOB";
    }
}