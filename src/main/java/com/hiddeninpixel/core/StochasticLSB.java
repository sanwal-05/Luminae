// StochasticLSB hides data in randomly chosen pixels instead of going in order.
//
// HOW IT WORKS:
// "Stochastic" means random. Instead of embedding bits in pixels left to right,
// we use the user's password to create a Pseudo-Random Number Generator (PRNG).
// The PRNG picks which pixel positions to write bits into, in a scattered "random" order.
//
// WHY IS THIS BETTER THAN SEQUENTIAL?
// Sequential LSB modifies pixels in a predictable pattern starting from the top left corner.
// Statistical analysis tools (called steganalysis) can detect this pattern by looking at the
// histogram of pixel values. Stochastic LSB scatters changes across the whole image,
// making it look like natural random noise and much harder to detect.
//
// HOW DO BOTH SIDES AGREE ON THE SAME PIXELS?
// Because the sender and receiver both use the same password, they both create
// the same PRNG with the same seed. So they independently generate the same
// list of "random" positions. Same password means same pixel positions every time.
package com.hiddeninpixel.core;

// BufferedImage is Java's in-memory image: a grid of pixels you can read and write.
import java.awt.image.BufferedImage;

// Random is Java's built-in pseudo-random number generator.
import java.util.Random;

// SecurityProvider converts a password into a seed number for the PRNG.
import com.hiddeninpixel.service.SecurityProvider;

// BitManipulator handles the actual bit-level operations on pixel channels.
import com.hiddeninpixel.service.BitManipulator;

// All our custom error types so we can throw meaningful exceptions.
import com.hiddeninpixel.exception.*;

public class StochasticLSB implements StegoAlgorithm {

    // Helper that does the bit operations (splitting pixels, embedding, extracting LSBs).
    private final BitManipulator bitManipulator;

    // Helper that turns the password string into a long integer (the PRNG seed).
    private final SecurityProvider securityProvider;

    // The first 32 bits we embed are not the message itself.
    // They store the LENGTH of the message so the decoder knows how many bits to read.
    private static final int HEADER_SIZE = 32;

    // Constructor: create both helper objects this class needs.
    public StochasticLSB() {
        this.bitManipulator = new BitManipulator();
        this.securityProvider = new SecurityProvider();
    }

    // Hides the payload bytes inside the cover image.
    // The pixel positions used are determined by the password, not by order.
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        // A password is required because without it we cannot generate the random positions.
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("Stochastic LSB requires a password seed");
        }

        // Check that the message will actually fit inside the image.
        // Each pixel has 3 channels (R, G, B), each contributing 1 bit of capacity.
        if (payload.length > getCapacity(cover)) {
            throw new InsufficientCapacityException("Payload exceeds randomized capacity");
        }

        // Make a deep copy of the original image so we never touch the original pixels.
        // All our changes go into this copy.
        BufferedImage stego = deepCopy(cover);

        // Convert the password string into a long integer (seed).
        // Same password always gives the same seed because SecurityProvider uses PBKDF2 internally.
        long seed = securityProvider.passwordToSeed(key);

        // Create the PRNG (pseudo-random number generator) seeded with the password-derived number.
        // Using "new Random(seed)" means the sequence of random numbers is always the same
        // for the same seed. This is what makes the hiding and extracting agree.
        Random prng = new Random(seed);

        // Calculate the total number of bits we need to embed.
        // Each byte of payload is 8 bits, plus the 32-bit header.
        int totalBits = (payload.length * 8) + HEADER_SIZE;

        // Generate the full list of pixel bit positions we will use.
        // Each position is an index into the flat stream of channel LSBs across the image.
        int[] bitPositions = generateUniquePositions(prng, totalBits, cover);

        // "posIndex" tracks which entry in bitPositions we are currently writing to.
        int posIndex = 0;

        // STEP 1: Embed the 32-bit header (the payload length as a binary integer).
        // The decoder reads this first to know how many message bits to extract.
        int length = payload.length;
        for (int i = 0; i < 32; i++) {
            // (31 - i) means we write the most important bit first (big-endian order).
            int bit = (length >> (31 - i)) & 1;

            // Write this one bit into the image at the position the PRNG gave us.
            embedBitAtPosition(stego, bitPositions[posIndex++], bit);
        }

        // STEP 2: Embed every bit of every byte of the actual message.
        for (byte b : payload) {
            // Loop through all 8 bits of this byte, from bit 7 (most important) to bit 0.
            for (int i = 7; i >= 0; i--) {
                // Pull out bit i from this byte.
                int bit = (b >> i) & 1;

                // Write this bit into the next random position in the image.
                embedBitAtPosition(stego, bitPositions[posIndex++], bit);
            }
        }

        // Return the finished stego image. It visually looks identical to the original.
        return stego;
    }

    // Extracts the hidden message from a stego image.
    // We regenerate the same random positions from the same password to find the bits.
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        // Cannot decode without the password since we need the same PRNG seed.
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("Stochastic LSB requires the original password");
        }

        // Re-derive the same seed from the same password.
        long seed = securityProvider.passwordToSeed(key);

        // Recreate the same PRNG with the same seed. It will produce the same sequence.
        Random prng = new Random(seed);

        // STEP 1: Read the first 32 positions (the header) to find the message length.
        // We need the length first so we know how many more positions to generate.
        int[] headerPositions = new int[HEADER_SIZE];
        // The maximum valid bit position is total channels across the whole image.
        int maxPos = stego.getWidth() * stego.getHeight() * 3;
        for (int i = 0; i < HEADER_SIZE; i++) {
            // Generate a random position between 0 and maxPos.
            headerPositions[i] = prng.nextInt(maxPos);
        }

        // Reconstruct the 32-bit length integer by reading one bit at a time.
        int length = 0;
        for (int i = 0; i < 32; i++) {
            // Shift existing bits left to make room, then OR in the next bit.
            length = (length << 1) | extractBitAtPosition(stego, headerPositions[i]);
        }

        // Sanity check: if the length is impossible, the password was probably wrong.
        // A wrong password gives a wrong seed which gives wrong positions and garbage bits.
        if (length <= 0 || length > getCapacity(stego)) {
            throw new InvalidKeyException("Invalid length or wrong password");
        }

        // STEP 2: Now that we know the length, regenerate ALL positions from the very start.
        // We start from the same seed so the positions match what was used when hiding.
        // We need header + payload positions all at once for correct indexing.
        int totalBits = length * 8;
        int[] bitPositions = generateUniquePositions(new Random(seed), totalBits + HEADER_SIZE, stego);

        // Read each byte of the payload from its scattered random positions.
        byte[] payload = new byte[length];
        for (int i = 0; i < length; i++) {
            byte b = 0;
            for (int j = 7; j >= 0; j--) {
                // The payload bits start after the 32 header bits in the bitPositions array.
                // (i * 8) jumps to the right byte's set of 8 positions.
                // (7 - j) selects which of those 8 positions we are on.
                int posIndex = HEADER_SIZE + (i * 8) + (7 - j);

                // Read the bit from that position and OR it into the correct bit slot of b.
                b |= (extractBitAtPosition(stego, bitPositions[posIndex]) << j);
            }
            // Store the fully assembled byte in our payload array.
            payload[i] = b;
        }

        return payload;
    }

    // Returns the maximum number of bytes that can be hidden in the given image.
    // Total available bit slots = width x height x 3 channels.
    // We subtract the 32-bit header and divide by 8 to get bytes.
    @Override
    public int getCapacity(BufferedImage image) {
        return (image.getWidth() * image.getHeight() * 3 - HEADER_SIZE) / 8;
    }

    // Generates an array of "count" random bit positions using the PRNG.
    // Each value is a flat index into the stream of all channel LSBs across the image.
    // Example: position 0 = Red channel of pixel (0,0), position 1 = Green of (0,0), etc.
    private int[] generateUniquePositions(Random prng, int count, BufferedImage img) {
        // The highest valid index is one less than total channels in the image.
        int maxPos = img.getWidth() * img.getHeight() * 3;

        int[] positions = new int[count];

        // Ask the PRNG for "count" random integers, each in the range [0, maxPos).
        for (int i = 0; i < count; i++) {
            positions[i] = prng.nextInt(maxPos);
        }

        return positions;
    }

    // Writes one bit into the image at a specific flat bit position.
    // bitPos is converted back to (x, y) pixel coordinates and a channel number.
    private void embedBitAtPosition(BufferedImage img, int bitPos, int bit) {
        // Each pixel contributes 3 positions (R=0, G=1, B=2).
        // Divide by 3 to get which pixel, and mod 3 to get which channel.
        int pixelIndex = bitPos / 3;
        int channel = bitPos % 3;  // 0 = Red, 1 = Green, 2 = Blue

        // Convert flat pixel index to 2D grid coordinates.
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();

        // Read the current pixel value (packed as a single integer).
        int rgb = img.getRGB(x, y);

        // Split into three separate channel values [R, G, B].
        int[] channels = bitManipulator.splitRGB(rgb);

        // Replace the LSB of the target channel with our bit.
        channels[channel] = bitManipulator.embedLSB(channels[channel], bit);

        // Pack the three channels back into one integer and write it to the image.
        img.setRGB(x, y, bitManipulator.mergeRGB(channels));
    }

    // Reads one bit from the image at a specific flat bit position.
    // Exact mirror of embedBitAtPosition, but reads instead of writes.
    private int extractBitAtPosition(BufferedImage img, int bitPos) {
        int pixelIndex = bitPos / 3;
        int channel = bitPos % 3;
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();

        int rgb = img.getRGB(x, y);
        int[] channels = bitManipulator.splitRGB(rgb);

        // Return just the LSB (0 or 1) of the chosen channel.
        return bitManipulator.extractLSB(channels[channel]);
    }

    // Creates a brand new independent copy of a BufferedImage.
    // "Deep copy" means the new image has its own pixel data in memory.
    // Changing the copy does not affect the original at all.
    private BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
            source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        // Draw all pixels from source onto the blank copy canvas.
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }
}
