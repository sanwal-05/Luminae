// MetadataChannel hides data by appending it to the END of the PNG file.
// This is completely different from the pixel-based LSB methods.
//
// HOW IT WORKS:
// A PNG file ends with a special 8-byte block called the "IEND" chunk.
// Standard image viewers stop reading the file at IEND.
// Anything written after IEND is invisible to normal software.
// We use this to our advantage: we copy the original image and then append
// our hidden data after the IEND marker.
//
// Our hidden block has this structure:
// [##LUN_BEGIN##] [your secret message bytes] [##LUN_END##]
// We use unique marker strings so we can find and read the data back.
package com.hiddeninpixel.core;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import com.hiddeninpixel.exception.*;

public class MetadataChannel implements StegoAlgorithm {

    // These strings mark the start and end of our hidden block.
    // They are unusual enough that a normal image file would never contain them.
    private static final String START_MARKER = "##LUN_BEGIN##";
    private static final String END_MARKER = "##LUN_END##";

    // This method is not used for MetadataChannel because we work with the file directly.
    // We return the cover image unchanged to satisfy the interface contract.
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        return cover;
    }

    // This method cannot be used for MetadataChannel because we need the actual file bytes,
    // not just the in-memory image. The UI calls "extractFromFile" directly instead.
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        throw new UnsupportedOperationException(
            "Metadata extraction requires binary file access - use extractFromFile(File)"
        );
    }

    // We set a practical 1 MB limit on how much data can be appended.
    // Technically there is no hard limit, but 1 MB keeps things reasonable.
    @Override
    public int getCapacity(BufferedImage image) {
        return 1024 * 1024; // 1 MB in bytes
    }

    // Hides data by appending it to the end of the image file on disk.
    // "inputFile" is the original image. "outputFile" is where the result is saved.
    public void embedIntoFile(File inputFile, byte[] payload, File outputFile) throws StegoException {
        try {
            // Step 1: Copy the original PNG file byte for byte to the output location.
            // The output file is now a valid PNG with the image unchanged.
            Files.copy(inputFile.toPath(), outputFile.toPath());

            // Step 2: Build the hidden block: start marker + payload + end marker.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(START_MARKER.getBytes());  // write ##LUN_BEGIN##
            bos.write(payload);                  // write the actual secret message bytes
            bos.write(END_MARKER.getBytes());    // write ##LUN_END##

            // Step 3: Append the hidden block to the end of the output file.
            // APPEND mode means we add after the existing content, not replace it.
            Files.write(outputFile.toPath(), bos.toByteArray(), StandardOpenOption.APPEND);

        } catch (IOException e) {
            throw new StegoException("Embedding failed: " + e.getMessage());
        }
    }

    // Reads the hidden data back out of a file by searching for our markers.
    public byte[] extractFromFile(File stegoFile) throws StegoException {
        try {
            // Read every single byte of the file into memory.
            byte[] fileContent = Files.readAllBytes(stegoFile.toPath());

            // Convert to a String using ISO-8859-1 (Latin-1) encoding.
            // ISO-8859-1 maps each byte value 0-255 to exactly one character,
            // making it safe to search for our markers without losing any bytes.
            String contentStr = new String(fileContent, "ISO-8859-1");

            // Find where our markers are in the file.
            int startIndex = contentStr.lastIndexOf(START_MARKER);
            int endIndex = contentStr.lastIndexOf(END_MARKER);

            // If either marker is missing or they are in the wrong order, no valid data exists.
            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                throw new InvalidKeyException("No hidden metadata found or markers corrupted.");
            }

            // The actual payload sits between the end of the start marker and the start of the end marker.
            int payloadStart = startIndex + START_MARKER.length();
            int payloadLength = endIndex - payloadStart;

            // Copy just the payload bytes into a new array.
            byte[] payload = new byte[payloadLength];
            System.arraycopy(fileContent, payloadStart, payload, 0, payloadLength);

            return payload;

        } catch (IOException e) {
            throw new StegoException("Extraction failed: " + e.getMessage());
        }
    }
}
