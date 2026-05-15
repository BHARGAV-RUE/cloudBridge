package com.bhargav.cloudbridge.repository;

import com.bhargav.cloudbridge.entity.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DownloadTokenRepository
        extends JpaRepository<DownloadToken, String> {

    Optional<DownloadToken> findBySecureTokenAndIsActiveTrue(String token);

    List<DownloadToken> findByFile_Id(String fileId);
}