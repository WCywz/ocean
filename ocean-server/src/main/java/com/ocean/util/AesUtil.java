package com.ocean.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String encrypt(String plainText, String secret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec keySpec = new SecretKeySpec(
                    deriveKey(secret), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipherText, String secret) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(
                    deriveKey(secret), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            byte[] key = new byte[16];
            System.arraycopy(hash, 0, key, 0, 16);
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static String mask(String value) {
        if (value == null || value.length() < 6) return "****";
        if (value.length() <= 8) return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        return value.substring(0, 4) + "****" + value.substring(value.length() - 2);
    }
}
