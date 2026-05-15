package com.bhargav.cloudbridge.implementation.storage;

import com.bhargav.cloudbridge.service.storage.StorageService;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveStorageServiceImpl implements StorageService {

    // Injected from GoogleDriveConfig @Bean
    private final Drive driveClient;

    @Value("${google.drive.folder-id}")
    private String folderId;

    @Override
    public String uploadFile(String fileKey,
                             InputStream inputStream,
                             long fileSize,
                             String contentType) {
        try {
            log.info("[Google Drive] Uploading → name: {}", fileKey);

            File metadata = new File();
            metadata.setName(fileKey);
            metadata.setParents(List.of(folderId));

            InputStreamContent mediaContent =
                    new InputStreamContent(contentType, inputStream);

            File uploaded = driveClient.files()
                    .create(metadata, mediaContent)
                    .setFields("id, name")
                    .execute();

            // Google Drive returns file ID — this is the storagePath stored in DB
            log.info("[Google Drive] Upload complete | Drive ID: {}", uploaded.getId());
            return uploaded.getId();

        } catch (Exception e) {
            log.error("[Google Drive] Upload failed | name: {} | reason: {}",
                    fileKey, e.getMessage());
            throw new RuntimeException("Google Drive upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        try {
            // storagePath = Google Drive file ID
            log.info("[Google Drive] Downloading → Drive ID: {}", storagePath);

            return driveClient.files()
                    .get(storagePath)
                    .executeMediaAsInputStream();

        } catch (Exception e) {
            log.error("[Google Drive] Download failed | Drive ID: {} | reason: {}",
                    storagePath, e.getMessage());
            throw new RuntimeException("Google Drive download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            log.info("[Google Drive] Deleting → Drive ID: {}", storagePath);

            driveClient.files().delete(storagePath).execute();
            log.info("[Google Drive] Delete complete: {}", storagePath);

        } catch (Exception e) {
            log.error("[Google Drive] Delete failed | Drive ID: {} | reason: {}",
                    storagePath, e.getMessage());
            throw new RuntimeException("Google Drive delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            driveClient.files().get(storagePath).setFields("id").execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "GOOGLE_DRIVE";
    }
}