package com.bhargav.cloudbridge.service.encryption;

import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import lombok.Builder;
import lombok.Getter;
import java.io.InputStream;

@Getter
@Builder
public class EncryptionResult {

    private final InputStream encryptedStream;
    private final long encryptedSize;
    private final String ivHex;
    private final EncryptionAlgorithm algorithm;
}
