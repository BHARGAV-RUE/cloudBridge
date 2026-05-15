package com.bhargav.cloudbridge.repository;

import com.bhargav.cloudbridge.entity.FileMetadata;
import com.bhargav.cloudbridge.entity.CompressionFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FileMetadataRepository
        extends JpaRepository<FileMetadata, String> {

    @Query("""
        SELECT f FROM FileMetadata f
        WHERE f.expiryDate < :now
        AND f.isDeleted = false
    """)
    List<FileMetadata> findExpiredFiles(@Param("now") LocalDateTime now);

    @Query("""
        SELECT f FROM FileMetadata f
        WHERE f.downloadCount >= f.downloadLimit
        AND f.isDeleted = false
    """)
    List<FileMetadata> findLimitReachedFiles();

    List<FileMetadata> findByCloudProviderAndIsDeletedFalse(String cloudProvider);

    List<FileMetadata> findByIsDeletedFalse();

    @Query("SELECT DISTINCT f.senderEmail FROM FileMetadata f " +
            "WHERE f.senderEmail IS NOT NULL")
    List<String> findAllSenderEmails();
}