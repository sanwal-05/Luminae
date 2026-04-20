package com.hiddeninpixel.core;

import java.awt.image.BufferedImage;
import com.hiddeninpixel.service.BitManipulator;
import com.hiddeninpixel.exception.*;

/**
 * SEQUENTIAL LSB: The most basic steganography technique.
 * 
 * VIVA NOTE: This algorithm hides data by replacing the Least Significant Bit (LSB)
 * of each color channel (Red, Green, Blue) sequentially. Since the LSB contributes
 * only 1 unit to a 255-unit range, the change is invisible to the human eye.
 * 
 * BITMASKING: We use (rgb & 0xFFFFFFFE) | bit to replace the 0th bit without 
 * affecting higher bits.
 */
public class SequentialLSB implements StegoAlgorithm {
    
    // SRP Principle: Outsourcing low-level bit logic to BitManipulator
    private final BitManipulator bitManipulator;
    private static final int HEADER_SIZE = 32; // 4 bytes for payload length
    
    public SequentialLSB() {
        this.bitManipulator = new BitManipulator();
    }
    
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        if (payload.length > getCapacity(cover)) {
            throw new InsufficientCapacityException(
                "Payload size: " + payload.length + " bytes exceeds capacity: " + getCapacity(cover)
            );
        }
        
        BufferedImage stego = deepCopy(cover);
        int bitIndex = 0;
        
        // Embed payload length (32-bit header)
        int length = payload.length;
        for (int i = 0; i < 32; i++) {
            int bit = (length >> (31 - i)) & 1;
            embedBit(stego, bitIndex++, bit);
        }
        
        // Embed payload bits sequentially
        for (byte b : payload) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                embedBit(stego, bitIndex++, bit);
            }
        }
        
        return stego;
    }
    
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        int bitIndex = 0;
        
        // Extract length header
        int length = 0;
        for (int i = 0; i < 32; i++) {
            length = (length << 1) | extractBit(stego, bitIndex++);
        }
        
        if (length <= 0 || length > getCapacity(stego)) {
            throw new InvalidKeyException("Corrupted header or wrong key");
        }
        
        // Extract payload
        byte[] payload = new byte[length];
        for (int i = 0; i < length; i++) {
            byte b = 0;
            for (int j = 7; j >= 0; j--) {
                b |= (extractBit(stego, bitIndex++) << j);
            }
            payload[i] = b;
        }
        
        return payload;
    }
    
    @Override
    public int getCapacity(BufferedImage image) {
        int totalBits = image.getWidth() * image.getHeight() * 3; // RGB channels
        return (totalBits - HEADER_SIZE) / 8; // Convert to bytes, reserve header
    }
    
    private void embedBit(BufferedImage img, int bitIndex, int bit) {
        int pixelIndex = bitIndex / 3;
        int channel = bitIndex % 3;
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();
        
        int rgb = img.getRGB(x, y);
        int[] channels = bitManipulator.splitRGB(rgb);
        channels[channel] = bitManipulator.embedLSB(channels[channel], bit);
        img.setRGB(x, y, bitManipulator.mergeRGB(channels));
    }
    
    private int extractBit(BufferedImage img, int bitIndex) {
        int pixelIndex = bitIndex / 3;
        int channel = bitIndex % 3;
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
