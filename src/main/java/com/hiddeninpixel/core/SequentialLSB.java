// SequentialLSB is the simplest steganography algorithm in this project.
//
// HOW IT WORKS:
// Every pixel in a PNG image has three color channels: Red, Green, Blue.
// Each channel is a number from 0 to 255, stored in 8 bits of memory.
// The "Least Significant Bit" (LSB) is the last bit (rightmost) in that 8-bit number.
// Changing this bit only changes the color value by 1 (out of 255), which is invisible.
//
// We go through pixels one by one, left to right, top to bottom (sequentially).
// For each pixel, we replace the LSB of R, then G, then B with one bit of our secret message.
//
// HEADER: Before the message, we embed a 32-bit number that tells the decoder how many
// bytes to read. Without this, the decoder would not know where the message ends.
package com.hiddeninpixel.core;

// BufferedImage is how Java stores an image in memory.
import java.awt.image.BufferedImage;

// BitManipulator handles all the low-level bit operations for us.
import com.hiddeninpixel.service.BitManipulator;

// Import all custom exception types from this package.
import com.hiddeninpixel.exception.*;

public class SequentialLSB implements StegoAlgorithm {

    // We delegate all bit-level work to BitManipulator so this class stays focused on the algorithm.
    // "final" means this reference is set once in the constructor and never reassigned.
    private final BitManipulator bitManipulator;

    // The header is 32 bits (4 bytes) and stores the length of the payload.
    // We reserve this space at the start of the image before writing the actual message.
    private static final int HEADER_SIZE = 32;

    // Constructor: create our BitManipulator helper.
    public SequentialLSB() {
        this.bitManipulator = new BitManipulator();
    }

    // Hides the payload bytes inside the cover image using sequential LSB substitution.
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        // Check if the message fits inside the image.
        // Trying to hide more data than the image can hold would corrupt it.
        if (payload.length > getCapacity(cover)) {
            throw new InsufficientCapacityException(
                "Payload size: " + payload.length + " bytes exceeds capacity: " + getCapacity(cover)
            );
        }

        // Make a deep copy of the image so we never modify the original.
        // We write the hidden bits into this copy.
        BufferedImage stego = deepCopy(cover);

        // "bitIndex" tracks which bit position we are writing to next.
        // The image is treated as one long stream of bits: R0, G0, B0, R1, G1, B1, ...
        int bitIndex = 0;

        // STEP 1: Embed the header (the length of the payload as a 32-bit integer).
        // This is the first thing the decoder will read, so it knows how many bits follow.
        int length = payload.length;
        for (int i = 0; i < 32; i++) {
            // Extract bit i from the length integer, starting from the most significant bit.
            // (31 - i) counts from 31 down to 0 so we write the most important bit first.
            int bit = (length >> (31 - i)) & 1;

            // Write this one bit into the image at the current position.
            embedBit(stego, bitIndex++, bit);
        }

        // STEP 2: Embed every byte of the actual message, bit by bit.
        for (byte b : payload) {
            // A byte has 8 bits. We write them from the most significant (bit 7) down to 0.
            for (int i = 7; i >= 0; i--) {
                // Extract bit i from this byte.
                int bit = (b >> i) & 1;

                // Write this bit into the next available position in the image.
                embedBit(stego, bitIndex++, bit);
            }
        }

        // Return the finished stego image. It looks the same as the original.
        return stego;
    }

    // Reads the hidden message back out of a stego image.
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        // We read bits starting from position 0, same order as hiding.
        int bitIndex = 0;

        // STEP 1: Read the 32-bit header to find out how long the message is.
        int length = 0;
        for (int i = 0; i < 32; i++) {
            // Shift the accumulated length left to make room, then OR in the next bit.
            length = (length << 1) | extractBit(stego, bitIndex++);
        }

        // If the length is nonsensical (zero, negative, or impossibly large), the image
        // probably has no hidden data or was encoded by a different tool.
        if (length <= 0 || length > getCapacity(stego)) {
            throw new InvalidKeyException("Corrupted header or wrong key");
        }

        // STEP 2: Read exactly "length" bytes of payload.
        byte[] payload = new byte[length];
        for (int i = 0; i < length; i++) {
            byte b = 0;
            // Rebuild each byte from 8 individual bits.
            for (int j = 7; j >= 0; j--) {
                // Shift the bit into the correct position and OR it into the byte.
                b |= (extractBit(stego, bitIndex++) << j);
            }
            payload[i] = b;
        }

        return payload;
    }

    // Returns how many bytes of message can fit in the given image.
    // Total available bits = pixels * 3 channels. We subtract the 32-bit header.
    // Then divide by 8 to convert bits to bytes.
    @Override
    public int getCapacity(BufferedImage image) {
        int totalBits = image.getWidth() * image.getHeight() * 3; // 3 channels per pixel
        return (totalBits - HEADER_SIZE) / 8;
    }

    // Writes one single bit into a specific position in the image.
    // bitIndex is a flat index into the stream of channel LSBs across all pixels.
    private void embedBit(BufferedImage img, int bitIndex, int bit) {
        // Each pixel contributes 3 bit slots (R, G, B). Find which pixel and which channel.
        int pixelIndex = bitIndex / 3;
        int channel = bitIndex % 3; // 0 = Red, 1 = Green, 2 = Blue

        // Convert the flat pixel index into 2D coordinates (x, y).
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();

        // Read the current pixel value.
        int rgb = img.getRGB(x, y);

        // Split into three separate channel values.
        int[] channels = bitManipulator.splitRGB(rgb);

        // Replace the LSB of the chosen channel with our bit.
        channels[channel] = bitManipulator.embedLSB(channels[channel], bit);

        // Pack the three channels back into one integer and write it back to the image.
        img.setRGB(x, y, bitManipulator.mergeRGB(channels));
    }

    // Reads one single bit from a specific position in the image.
    // Mirrors the logic in embedBit.
    private int extractBit(BufferedImage img, int bitIndex) {
        int pixelIndex = bitIndex / 3;
        int channel = bitIndex % 3;
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();

        int rgb = img.getRGB(x, y);
        int[] channels = bitManipulator.splitRGB(rgb);

        // Read and return the LSB of the chosen channel.
        return bitManipulator.extractLSB(channels[channel]);
    }

    // Creates a brand new copy of a BufferedImage so we do not modify the original.
    // "Deep copy" means the new image has its own pixel data, not a reference to the old one.
    private BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
            source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        // Draw the source image onto the new blank canvas to copy all pixels.
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }
}
