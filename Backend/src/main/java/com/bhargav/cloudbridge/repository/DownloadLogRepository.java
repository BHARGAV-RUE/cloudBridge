package com.bhargav.cloudbridge.repository;

import com.bhargav.cloudbridge.entity.DownloadLog;
import com.bhargav.cloudbridge.entity.DownloadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DownloadLogRepository
        extends JpaRepository<DownloadLog, String> {

    List<DownloadLog> findByFile_Id(String fileId);

    boolean existsByFile_IdAndReceiverEmail(String fileId, String receiverEmail);

    List<DownloadLog> findByFile_IdAndDownloadStatus(
            String fileId, DownloadStatus status);

    @Query("SELECT COUNT(DISTINCT d.receiverEmail) FROM DownloadLog d " +
            "WHERE d.file.id = :fileId AND d.downloadStatus = 'SUCCESS'")
    long countUniqueDownloadersByFileId(@Param("fileId") String fileId);

    @Query("SELECT DISTINCT d.receiverEmail FROM DownloadLog d " +
            "WHERE d.downloadStatus = 'SUCCESS'")
    List<String> findAllUniqueDownloaderEmails();
}