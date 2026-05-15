package com.bhargav.cloudbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "file_key", nullable = false, unique = true)
    private String fileKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "original_size")
    private Long originalSize;

    @Column(name = "compressed_name")
    private String compressedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "compression_format")
    private CompressionFormat compressionFormat;

    @Column(name = "compressed_size")
    private Long compressedSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "cloud_provider", nullable = false)
    private CloudProvider cloudProvider;

    @Column(name = "encryption_iv")
    private String encryptionIv;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "download_limit", nullable = false)
    private Integer downloadLimit;

    @Builder.Default
    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Builder.Default
    @Column(name = "unique_download_count", nullable = false)
    private Integer uniqueDownloadCount = 0;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;



    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        if(this.downloadCount == null) this.downloadCount = 0;
        if(this.isDeleted == null) this.isDeleted = false;
        if (this.expiryDate == null) this.expiryDate = LocalDateTime.now().plusDays(8);
        if (this.uniqueDownloadCount == null) this.uniqueDownloadCount = 0;
    }

}