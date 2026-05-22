package com.bhargav.cloudbridge.service.encryption;

import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import java.io.InputStream;
public interface EncryptionService {

    EncryptionResult encrypt(InputStream inputStream,
                             EncryptionAlgorithm algorithm,
                             String password) throws Exception;

    InputStream decrypt(InputStream encryptedStream,
                        EncryptionAlgorithm algorithm,
                        String ivHex,
                        String password) throws Exception;
}
