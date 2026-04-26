package com.hiddeninpixel.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * SRP: Cryptographic operations (PBKDF2, seeding).
 */
public class SecurityProvider {
    
    private static final byte[] SALT = "HiddenInPixel2025".getBytes();
    private static final int ITERATIONS = 10000;
    
    /**
     * Derives deterministic keystream from password using PBKDF2.
     */
    public byte[] deriveKeyStream(String password, int length) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            
            byte[] keyStream = new byte[length];
            System.arraycopy(hash, 0, keyStream, 0, Math.min(hash.length, length));
            return keyStream;
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 failed", e);
        }
    }
    
    /**
     * Converts password to deterministic seed for PRNG.
     */
    public long passwordToSeed(String password) {
        byte[] hash = deriveKeyStream(password, 8);
        long seed = 0;
        for (int i = 0; i < 8; i++) {
            seed = (seed << 8) | (hash[i] & 0xFF);
        }
        return seed;
    }
}
