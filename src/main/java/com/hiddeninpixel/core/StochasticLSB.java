package com.hiddeninpixel.core;

import java.awt.image.BufferedImage;
import java.util.Random;
import com.hiddeninpixel.service.SecurityProvider;
import com.hiddeninpixel.service.BitManipulator;
import com.hiddeninpixel.exception.*;

/**
 * STOCHASTIC LSB: Randomized pixel selection steganography.
 * 
 * VIVA NOTE: Unlike Sequential LSB, this algorithm avoids embedding data in a 
 * predictable linear order. It uses a Pseudo-Random Number Generator (PRNG) 
 * seeded with the user's password to select 'random' pixel coordinates.
 * 
 * ADVANTAGE: This defends against 'Chi-Square Analysis' (statistical steganalysis),
 * as the modifications are scattered across the image like natural noise.
 */
public class StochasticLSB implements StegoAlgorithm {
    
    private final BitManipulator bitManipulator;
    private final SecurityProvider securityProvider;
    private static final int HEADER_SIZE = 32;
    
    public StochasticLSB() {
        this.bitManipulator = new BitManipulator();
        this.securityProvider = new SecurityProvider();
    }
    
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("Stochastic LSB requires a password seed");
        }
        
        if (payload.length > getCapacity(cover)) {
            throw new InsufficientCapacityException("Payload exceeds randomized capacity");
        }
        
        BufferedImage stego = deepCopy(cover);
        long seed = securityProvider.passwordToSeed(key);
        Random prng = new Random(seed);
        
        int totalBits = (payload.length * 8) + HEADER_SIZE;
        int[] bitPositions = generateUniquePositions(prng, totalBits, cover);
        
        int posIndex = 0;
        
        // Embed length header
        int length = payload.length;
        for (int i = 0; i < 32; i++) {
            int bit = (length >> (31 - i)) & 1;
            embedBitAtPosition(stego, bitPositions[posIndex++], bit);
        }
        
        // Embed payload
        for (byte b : payload) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                embedBitAtPosition(stego, bitPositions[posIndex++], bit);
            }
        }
        
        return stego;
    }
    
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("Stochastic LSB requires the original password");
        }
        
        long seed = securityProvider.passwordToSeed(key);
        Random prng = new Random(seed);
        
        // Generate same positions for header extraction
        int[] headerPositions = new int[HEADER_SIZE];
        int maxPos = stego.getWidth() * stego.getHeight() * 3;
        for (int i = 0; i < HEADER_SIZE; i++) {
            headerPositions[i] = prng.nextInt(maxPos);
        }
        
        // Extract length
        int length = 0;
        for (int i = 0; i < 32; i++) {
            length = (length << 1) | extractBitAtPosition(stego, headerPositions[i]);
        }
        
        if (length <= 0 || length > getCapacity(stego)) {
            throw new InvalidKeyException("Invalid length or wrong password");
        }
        
        // Generate positions for payload
        int totalBits = length * 8;
        int[] bitPositions = generateUniquePositions(new Random(seed), totalBits + HEADER_SIZE, stego);
        
        byte[] payload = new byte[length];
        for (int i = 0; i < length; i++) {
            byte b = 0;
            for (int j = 7; j >= 0; j--) {
                int posIndex = HEADER_SIZE + (i * 8) + (7 - j);
                b |= (extractBitAtPosition(stego, bitPositions[posIndex]) << j);
            }
            payload[i] = b;
        }
        
        return payload;
    }
    
    @Override
    public int getCapacity(BufferedImage image) {
        return (image.getWidth() * image.getHeight() * 3 - HEADER_SIZE) / 8;
    }
    
    private int[] generateUniquePositions(Random prng, int count, BufferedImage img) {
        int maxPos = img.getWidth() * img.getHeight() * 3;
        int[] positions = new int[count];
        
        for (int i = 0; i < count; i++) {
            positions[i] = prng.nextInt(maxPos);
        }
        
        return positions;
    }
    
    private void embedBitAtPosition(BufferedImage img, int bitPos, int bit) {
        int pixelIndex = bitPos / 3;
        int channel = bitPos % 3;
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();
        
        int rgb = img.getRGB(x, y);
        int[] channels = bitManipulator.splitRGB(rgb);
        channels[channel] = bitManipulator.embedLSB(channels[channel], bit);
        img.setRGB(x, y, bitManipulator.mergeRGB(channels));
    }
    
    private int extractBitAtPosition(BufferedImage img, int bitPos) {
        int pixelIndex = bitPos / 3;
        int channel = bitPos % 3;
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();
        
        int rgb = img.getRGB(x, y);
        int[] channels = bitManipulator.splitRGB(rgb);
        return bitManipulator.extractLSB(channels[channel]);
    }
    
    private BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
            source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }
}
