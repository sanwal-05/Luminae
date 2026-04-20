package com.hiddeninpixel.core;

import java.awt.image.BufferedImage;
import com.hiddeninpixel.exception.StegoException;

/**
 * STRATEGY PATTERN: This interface defines the contract for all steganography techniques.
 * 
 * VIVA NOTE: Adheres to the 'Open/Closed Principle' (SOLID).
 * We can add new algorithms (Sequential, XOR, Stochastic) by implementing this interface 
 * without modifying the existing User Interface (UI) code.
 */
public interface StegoAlgorithm {
    
    /**
     * Embeds secret data into carrier image.
     * @param cover Original PNG image
     * @param payload Secret message as byte array
     * @param key User password (nullable for Sequential)
     * @return Stego image with hidden data
     * @throws StegoException on capacity/format errors
     */
    BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException;
    
    /**
     * Extracts hidden data from stego image.
     * @param stego Image containing hidden data
     * @param key User password (must match embedding key)
     * @return Extracted payload
     * @throws StegoException on key mismatch or data corruption
     */
    byte[] extract(BufferedImage stego, String key) throws StegoException;
    
    /**
     * Calculates maximum payload capacity for given image.
     * @param image Carrier image
     * @return Capacity in bytes
     */
    int getCapacity(BufferedImage image);
}
