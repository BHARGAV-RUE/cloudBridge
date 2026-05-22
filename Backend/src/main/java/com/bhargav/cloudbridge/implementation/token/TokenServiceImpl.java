package com.bhargav.cloudbridge.implementation.token;

import com.bhargav.cloudbridge.entity.DownloadToken;
import com.bhargav.cloudbridge.entity.FileMetadata;
import com.bhargav.cloudbridge.exception.DownloadLimitReachedException;
import com.bhargav.cloudbridge.exception.FileNotFoundException;
import com.bhargav.cloudbridge.exception.TokenExpiredException;
import com.bhargav.cloudbridge.repository.DownloadTokenRepository;
import com.bhargav.cloudbridge.service.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final DownloadTokenRepository tokenRepository;

    // Base URL of your backend — used to build the download link
    // e.g. https://api.cloudbridge.com  or  http://localhost:8080
    @Value("${cloudbridge.base-url}")
    private String baseUrl;

    // How many days until token expires — matches file expiry
    @Value("${cloudbridge.file.expiry-days}")
    private int expiryDays;

    // ── GENERATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public String generateDownloadToken(FileMetadata file) {

        // Generate cryptographically random UUID
        String rawToken = UUID.randomUUID().toString().replace("-", "");

        DownloadToken token = DownloadToken.builder()
                .file(file)
                .secureToken(rawToken)
                .expiryTime(LocalDateTime.now().plusDays(expiryDays))
                .isActive(true)
                .build();

        tokenRepository.save(token);

        String downloadUrl = buildDownloadUrl(rawToken);
        log.info("[TokenService] Token generated for file: {} | URL: {}",
                file.getId(), downloadUrl);

        return downloadUrl;
    }

    // ── VALIDATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DownloadToken validateToken(String token) {

        // Step 1 — does token exist and is it active?
        DownloadToken downloadToken = tokenRepository
                .findBySecureTokenAndIsActiveTrue(token)
                .orElseThrow(() -> {
                    log.warn("[TokenService] Invalid or inactive token: {}", token);
                    throw new TokenExpiredException(token);   // invalid or inactive

                });

        // Step 2 — is it expired?
        if (downloadToken.isExpired()) {
            log.warn("[TokenService] Token expired: {}", token);
            throw new TokenExpiredException();        // expired
        }

        // Step 3 — is the file soft-deleted?
        if (downloadToken.getFile().getIsDeleted()) {
            log.warn("[TokenService] File is deleted for token: {}", token);
            throw new FileNotFoundException();        // file deleted
        }

        // Step 4 — has download limit been reached?
        FileMetadata file = downloadToken.getFile();
        if (file.getDownloadCount() >= file.getDownloadLimit()) {
            log.warn("[TokenService] Download limit reached for file: {}",
                    file.getId());
            throw new DownloadLimitReachedException(); // limit hit
        }

        log.info("[TokenService] Token valid for file: {}", file.getId());
        return downloadToken;
    }

    // ── DEACTIVATE ALL ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivateAllTokensForFile(String fileId) {

        List<DownloadToken> tokens = tokenRepository.findByFile_Id(fileId);

        tokens.forEach(t -> t.setIsActive(false));
        tokenRepository.saveAll(tokens);

        log.info("[TokenService] Deactivated {} token(s) for file: {}",
                tokens.size(), fileId);
    }

    // ── DEACTIVATE SINGLE ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivateToken(String tokenId) {

        tokenRepository.findById(tokenId).ifPresent(t -> {
            t.setIsActive(false);
            tokenRepository.save(t);
            log.info("[TokenService] Deactivated token: {}", tokenId);
        });
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private String buildDownloadUrl(String rawToken) {
        // Format: {baseUrl}/api/files/download/{token}
        return baseUrl + "/api/files/download/" + rawToken;
    }
}