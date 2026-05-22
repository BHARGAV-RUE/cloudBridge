package com.bhargav.cloudbridge.dto.request;

import com.bhargav.cloudbridge.entity.CompressionFormat;
import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadRequest {

    // Who is sending — optional, used for confirmation email
    @Email(message = "Sender email must be valid")
    private String senderEmail;

    // Who receives the download link — required
    @NotBlank(message = "Receiver email is required")
    @Email(message = "Receiver email must be valid")
    private String receiverEmail;

    // Optional password — if null, no password protection
    private String password;

    // Which compression format to use — required
    @NotNull(message = "Compression format is required")
    private CompressionFormat compressionFormat;

    // Which encryption algorithm — defaults to AES_GCM if not provided
    private EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithm.AES_GCM;
}