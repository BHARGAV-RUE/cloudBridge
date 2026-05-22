package com.bhargav.cloudbridge.service.token;

import com.bhargav.cloudbridge.entity.DownloadToken;
import com.bhargav.cloudbridge.entity.FileMetadata;

import java.util.List;

public interface TokenService {

    /**
     * Generate a secure download token for a file.
     * Saves to DB and returns the full download URL.
     *
     * @param file the uploaded file metadata
     * @return full download URL e.g. https://api.cloudbridge.com/api/files/download/{token}
     */
    String generateDownloadToken(FileMetadata file);

    /**
     * Validate a token from the download URL.
     * Checks: exists → active → not expired → file not deleted
     *
     * @param token raw UUID token from URL
     * @return DownloadToken if valid
     * @throws RuntimeException if invalid for any reason
     */
    DownloadToken validateToken(String token);

    /**
     * Deactivate all tokens for a file.
     * Called by scheduler when file expires or hits download limit.
     *
     * @param fileId the file whose tokens should be deactivated
     */
    void deactivateAllTokensForFile(String fileId);

    /**
     * Deactivate a single token after it's been used
     * (only if we want single-use tokens in future).
     * Currently not used — tokens stay active until file expires.
     */
    void deactivateToken(String tokenId);
}