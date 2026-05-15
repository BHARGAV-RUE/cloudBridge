package com.bhargav.cloudbridge.service.storage;

import java.io.InputStream;
import java.util.random.RandomGenerator;

public interface StorageService {

    String uploadFile(String fileKey, InputStream inputStream, long fileSize, String contentType);

    InputStream downloadFile(String storagePath);

    void deleteFile(String storagePath);

    boolean fileExists(String storagePath);

    String getProviderName();
}



