package com.bhargav.cloudbridge.entity;

public enum DynamicProviderType {
    S3_COMPATIBLE,    // Wasabi, Cloudflare R2, MinIO, Backblaze B2
    AZURE_COMPATIBLE, // Azure Blob variants
    GOOGLE_COMPATIBLE // Google Cloud Storage
}