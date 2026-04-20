package com.hiddeninpixel.core;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import com.hiddeninpixel.exception.*;

/**
 * METADATA STEGANOGRAPHY: End-of-File (EOF) Appending Method.
 * 
 * VIVA NOTE: This technique appends hidden data AFTER the PNG 'IEND' chunk.
 * Standard image viewers only process data up to the 'IEND' marker, making
 * the appended data invisible to the user but accessible to our software.
 * 
 * STRATEGY PATTERN: This class implements 'StegoAlgorithm', allowing the UI 
 * to switch to this metadata technique without changing any GUI logic.
 */
public class MetadataChannel implements StegoAlgorithm {
    
    // Unique marker to identify our hidden data block
    private static final String START_MARKER = "##HIP_BEGIN##";
    private static final String END_MARKER = "##HIP_END##";
    
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        // Pixel-based hide is not applicable here as we modify the file directly
        // return the image as-is to satisfy the interface
        return cover;
    }
    
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        throw new UnsupportedOperationException(
            "Metadata extraction requires binary file access - use extractFromFile(File)"
        );
    }
    
    @Override
    public int getCapacity(BufferedImage image) {
        // Technically limited only by storage, but we set a practical limit for this project
        return 1024 * 1024; // 1MB capacity
    }
    
    /**
     * Embeds data by appending it to the end of the PNG file.
     * VIVA NOTE: We use a Marker-Payload-Marker structure: [START_MARKER][PAYLOAD][END_MARKER]
     */
    public void embedIntoFile(File inputFile, byte[] payload, File outputFile) throws StegoException {
        try {
            // 1. Copy the original image content to the output file
            Files.copy(inputFile.toPath(), outputFile.toPath());
            
            // 2. Prepare the hidden block
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(START_MARKER.getBytes());
            bos.write(payload);
            bos.write(END_MARKER.getBytes());
            
            // 3. Append the hidden block to the end of the output file
            Files.write(outputFile.toPath(), bos.toByteArray(), StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            throw new StegoException("Deployment failed: " + e.getMessage());
        }
    }
    
    /**
     * Extracts hidden data from the end of a PNG file.
     * VIVA NOTE: This method searches for our markers starting from the end of the file.
     */
    public byte[] extractFromFile(File stegoFile) throws StegoException {
        try {
            byte[] fileContent = Files.readAllBytes(stegoFile.toPath());
            String contentStr = new String(fileContent, "ISO-8859-1"); // Use 1-to-1 mapping charset
            
            int startIndex = contentStr.lastIndexOf(START_MARKER);
            int endIndex = contentStr.lastIndexOf(END_MARKER);
            
            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                throw new InvalidKeyException("No hidden metadata found or markers corrupted.");
            }
            
            int payloadStart = startIndex + START_MARKER.length();
            int payloadLength = endIndex - payloadStart;
            
            byte[] payload = new byte[payloadLength];
            System.arraycopy(fileContent, payloadStart, payload, 0, payloadLength);
            
            return payload;
            
        } catch (IOException e) {
            throw new StegoException("Extraction failed: " + e.getMessage());
        }
    }
}
