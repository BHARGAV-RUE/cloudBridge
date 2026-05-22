package com.bhargav.cloudbridge.service.email;

import com.bhargav.cloudbridge.entity.FileMetadata;

public interface EmailService {

    /**
     * Send download link to the receiver.
     * Called immediately after upload + token generation.
     *
     * @param file        uploaded file metadata
     * @param downloadUrl the secure download URL
     * @param hasPassword whether the file is password protected
     */
    void sendDownloadLinkToReceiver(FileMetadata file,
                                    String downloadUrl,
                                    boolean hasPassword);

    /**
     * Send upload confirmation to the sender.
     * Called immediately after upload + token generation.
     *
     * @param file        uploaded file metadata
     * @param downloadUrl the secure download URL
     */
    void sendUploadConfirmationToSender(FileMetadata file,
                                        String downloadUrl);
}