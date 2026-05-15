package com.bhargav.cloudbridge.implementation.storage;

import com.bhargav.cloudbridge.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class AWSS3StorageServiceImpl implements StorageService{

    public final S3Client s3Client;

    @Value("${aws.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFile(String fileKey,
                             InputStream inputStream,
                             long fileSize,
                             String  contentType){
        try {
            log.info("[AWS S3] Uploading -> bucket: {} | key: {}", bucketName, fileKey);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromInputStream(inputStream, fileSize));

            log.info("[AWS S3] Upload Complete: {}", fileKey);
            return fileKey;
        }
        catch (S3Exception e){
            log.error("[AWS S3] Upload failed | key {} | reason: {}", fileKey, e.getMessage());
            throw new RuntimeException("AWS upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath){
        try {
            log.info("[AWS S3] Downloading -> key: {}", storagePath);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            return s3Client.getObject(request);
        }
        catch (S3Exception e){
            log.error("[AWS S3] Download failed key: {} | reason: {}", storagePath, e.getMessage());
            throw new RuntimeException("AWS S3 download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String storagePath){
        try {
            log.info("[AWS S3] deleting -> key: {}", storagePath);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build());

            log.info("[AWS S3] delete complete: {}", storagePath);
        }
        catch (S3Exception e){
            log.error("[AWS S3] Delete failed | key: {} | reason: {}", storagePath, e.getMessage());
            throw new RuntimeException("AWS S3 delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String storagePath){
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build());
            return true;
        }
        catch (NoSuchKeyException e){
            return false;
        }
        catch (S3Exception e){
            log.warn("[AWS S3] Existence check failed | key: {} | reason: {}", storagePath, e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName(){
        return "AWS_S3";
    }
}