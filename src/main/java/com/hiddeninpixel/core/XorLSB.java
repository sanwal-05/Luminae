package com.hiddeninpixel.core;

import java.awt.image.BufferedImage;
import com.hiddeninpixel.service.SecurityProvider;
import com.hiddeninpixel.exception.*;

/**
 * XOR-LSB: Secure steganography using bitwise XOR.
 * 
 * VIVA NOTE: This class uses the 'Decorator' concept by wrapping 'SequentialLSB'
 * with an additional security layer. Before embedding, we perform a bitwise XOR 
 * between the payload and a key-stream derived from the password.
 * 
 * SECURITY: XOR is a symmetric operation: (A ^ B) ^ B = A. This allows us to
 * encrypt and decrypt using the exact same logic and key.
 */
public class XorLSB implements StegoAlgorithm {
    
    // Using Composition: Reusing SequentialLSB logic
    private final SequentialLSB baseLSB;
    private final SecurityProvider securityProvider;
    private static final byte[] PREAMBLE = {0x48, 0x49, 0x50}; // "HIP" verification
    
    public XorLSB() {
        this.baseLSB = new SequentialLSB();
        this.securityProvider = new SecurityProvider();
    }
    
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("XOR-LSB requires a non-empty key");
        }
        
        byte[] keyStream = securityProvider.deriveKeyStream(key, payload.length + PREAMBLE.length);
        byte[] combined = concatenate(PREAMBLE, payload);
        byte[] encrypted = xorBytes(combined, keyStream);
        
        return baseLSB.hide(cover, encrypted, null);
    }
    
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("XOR-LSB requires the original key");
        }
        
        byte[] encrypted = baseLSB.extract(stego, null);
        byte[] keyStream = securityProvider.deriveKeyStream(key, encrypted.length);
        byte[] decrypted = xorBytes(encrypted, keyStream);
        
        // Verify preamble
        for (int i = 0; i < PREAMBLE.length; i++) {
            if (decrypted[i] != PREAMBLE[i]) {
                throw new InvalidKeyException("Key verification failed - incorrect password");
            }
        }
        
        // Return payload without preamble
        byte[] payload = new byte[decrypted.length - PREAMBLE.length];
        System.arraycopy(decrypted, PREAMBLE.length, payload, 0, payload.length);
        return payload;
    }
    
    @Override
    public int getCapacity(BufferedImage image) {
        return baseLSB.getCapacity(image) - PREAMBLE.length;
    }
    
    private byte[] xorBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }
    
    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
