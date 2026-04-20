package com.hiddeninpixel.service;

/**
 * Encapsulates low-level bitwise operations.
 * SRP: Handles ONLY bit manipulation logic.
 */
public class BitManipulator {
    
    /**
     * Splits RGB integer into [R, G, B] array.
     */
    public int[] splitRGB(int rgb) {
        return new int[] {
            (rgb >> 16) & 0xFF,  // Red
            (rgb >> 8) & 0xFF,   // Green
            rgb & 0xFF           // Blue
        };
    }
    
    /**
     * Merges [R, G, B] into single RGB integer.
     */
    public int mergeRGB(int[] channels) {
        return (channels[0] << 16) | (channels[1] << 8) | channels[2];
    }
    
    /**
     * Embeds single bit into LSB of channel value.
     * Formula: (value & ~1) | bit
     */
    public int embedLSB(int channelValue, int bit) {
        return (channelValue & 0xFE) | (bit & 1);
    }
    
    /**
     * Extracts LSB from channel value.
     */
    public int extractLSB(int channelValue) {
        return channelValue & 1;
    }
}
