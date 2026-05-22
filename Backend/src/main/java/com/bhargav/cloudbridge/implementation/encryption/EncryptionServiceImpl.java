package com.bhargav.cloudbridge.implementation.encryption;

import com.bhargav.cloudbridge.service.encryption.EncryptionResult;
import com.bhargav.cloudbridge.service.encryption.EncryptionService;
import com.bhargav.cloudbridge.util.EncryptionAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.HexFormat;

@Slf4j
@Service
public class EncryptionServiceImpl implements EncryptionService {

    @Value("${cloudbridge.encryption.master-key}")
    private String masterKeyHex;

    private static final int GCM_TAG_LENGTH    = 128;
    private static final int GCM_IV_LENGTH     = 12;
    private static final int CBC_IV_LENGTH     = 16;
    private static final int PBKDF2_ITERATIONS = 310_000;
    private static final String PBKDF2_ALGO    = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();
    private final HexFormat hex = HexFormat.of();

    // ── PUBLIC API ─────────────────────────────────────────────────────────────

    @Override
    public EncryptionResult encrypt(InputStream inputStream,
                                    EncryptionAlgorithm algorithm,
                                    String password) throws Exception {

        byte[] iv        = generateIv(algorithm);
        SecretKey key    = buildKey(password, iv, algorithm);
        byte[] plain     = inputStream.readAllBytes();

        byte[] encrypted = switch (algorithm) {
            case AES_GCM -> encryptGcm(plain, key, iv);
            case AES_CBC -> encryptCbc(plain, key, iv);
        };

        log.info("[Encryption] Encrypted {} bytes using {}", plain.length, algorithm);

        return EncryptionResult.builder()
                .encryptedStream(new ByteArrayInputStream(encrypted))
                .encryptedSize(encrypted.length)
                .ivHex(hex.formatHex(iv))
                .algorithm(algorithm)
                .build();
    }

    @Override
    public InputStream decrypt(InputStream encryptedStream,
                               EncryptionAlgorithm algorithm,
                               String ivHex,
                               String password) throws Exception {

        byte[] iv        = hex.parseHex(ivHex);
        SecretKey key    = buildKey(password, iv, algorithm);
        byte[] encrypted = encryptedStream.readAllBytes();

        byte[] decrypted = switch (algorithm) {
            case AES_GCM -> decryptGcm(encrypted, key, iv);
            case AES_CBC -> decryptCbc(encrypted, key, iv);
        };

        log.info("[Encryption] Decrypted {} bytes using {}", decrypted.length, algorithm);
        return new ByteArrayInputStream(decrypted);
    }

    // ── KEY BUILDING ───────────────────────────────────────────────────────────

    private SecretKey buildKey(String password,
                               byte[] iv,
                               EncryptionAlgorithm algorithm) throws Exception {
        if (password != null && !password.isBlank()) {
            return deriveKeyFromPassword(password, iv);
        }
        return masterKey();
    }

    private SecretKey masterKey() {
        byte[] keyBytes = hex.parseHex(masterKeyHex);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private SecretKey deriveKeyFromPassword(String password,
                                            byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return new SecretKeySpec(keyBytes, "AES");
    }

    // ── IV GENERATION ──────────────────────────────────────────────────────────

    private byte[] generateIv(EncryptionAlgorithm algorithm) {
        int length = (algorithm == EncryptionAlgorithm.AES_GCM)
                ? GCM_IV_LENGTH : CBC_IV_LENGTH;
        byte[] iv = new byte[length];
        secureRandom.nextBytes(iv);
        return iv;
    }

    // ── AES-GCM ────────────────────────────────────────────────────────────────

    private byte[] encryptGcm(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    private byte[] decryptGcm(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    // ── AES-CBC ────────────────────────────────────────────────────────────────

    private byte[] encryptCbc(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    private byte[] decryptCbc(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }
}