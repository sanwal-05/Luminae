// XorLSB adds a security layer on top of the basic SequentialLSB algorithm.
//
// HOW IT WORKS:
// Before hiding, we encrypt the message using XOR (exclusive-or) with a keystream.
// The keystream is derived from the user's password using a cryptographic function (PBKDF2).
// XOR is symmetric: (message XOR key) XOR key = message.
// This means the exact same operation is used to both encrypt and decrypt.
//
// We also add a small "preamble" (the bytes for "HIP" = Luminae) at the start.
// When decoding, we check if the preamble is correct. If it is not, the password is wrong.
// This is the tamper check that gives the user a "wrong password" error instead of garbage output.
//
// This class uses SequentialLSB internally (composition instead of re-writing the same logic).
// The decorator concept: XorLSB wraps SequentialLSB and adds encryption around it.
package com.hiddeninpixel.core;

// BufferedImage is Java's in-memory image representation.
import java.awt.image.BufferedImage;

// SecurityProvider handles the cryptographic key generation from a password.
import com.hiddeninpixel.service.SecurityProvider;

// Import all exception types.
import com.hiddeninpixel.exception.*;

public class XorLSB implements StegoAlgorithm {

    // We reuse SequentialLSB for the actual pixel-level embedding and extraction.
    // We just handle encryption/decryption around it.
    private final SequentialLSB baseLSB;

    // SecurityProvider generates the keystream from the user's password.
    private final SecurityProvider securityProvider;

    // The preamble is a fixed 3-byte sequence embedded before the real message.
    // "L", "U", "N" in ASCII (the first three letters of Luminae).
    // When decoding, we verify these bytes. If they are wrong, the password is wrong.
    private static final byte[] PREAMBLE = {0x4C, 0x55, 0x4E};

    // Constructor: create the two helper objects we depend on.
    public XorLSB() {
        this.baseLSB = new SequentialLSB();
        this.securityProvider = new SecurityProvider();
    }

    // Hides the payload after encrypting it with XOR.
    @Override
    public BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException {
        // XOR-LSB requires a password because without it, there is no keystream to encrypt with.
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("XOR-LSB requires a non-empty key");
        }

        // Derive a keystream of bytes from the password.
        // Length = preamble + actual payload, because we need to encrypt both.
        byte[] keyStream = securityProvider.deriveKeyStream(key, payload.length + PREAMBLE.length);

        // Stick the preamble in front of the real payload.
        byte[] combined = concatenate(PREAMBLE, payload);

        // XOR every byte of combined with the corresponding byte from the keystream.
        // This scrambles the data so it cannot be read without the same key.
        byte[] encrypted = xorBytes(combined, keyStream);

        // Now use SequentialLSB to embed the encrypted bytes into the image.
        // We pass null for the key because SequentialLSB does not need a key itself.
        return baseLSB.hide(cover, encrypted, null);
    }

    // Extracts and decrypts the hidden message.
    @Override
    public byte[] extract(BufferedImage stego, String key) throws StegoException {
        // We cannot decrypt without the password.
        if (key == null || key.isEmpty()) {
            throw new InvalidKeyException("XOR-LSB requires the original key");
        }

        // Use SequentialLSB to pull out the raw (still encrypted) bytes.
        byte[] encrypted = baseLSB.extract(stego, null);

        // Re-derive the same keystream using the same password.
        // (Same password + same PBKDF2 settings always gives the same keystream.)
        byte[] keyStream = securityProvider.deriveKeyStream(key, encrypted.length);

        // XOR the encrypted data with the keystream to get back the original.
        // Because XOR undoes itself: (A XOR B) XOR B = A.
        byte[] decrypted = xorBytes(encrypted, keyStream);

        // Check the preamble to verify the password was correct.
        for (int i = 0; i < PREAMBLE.length; i++) {
            // If any preamble byte does not match, the decryption produced garbage.
            // This means the wrong password was used.
            if (decrypted[i] != PREAMBLE[i]) {
                throw new InvalidKeyException("Key verification failed - incorrect password");
            }
        }

        // Return only the real message, skipping the preamble bytes at the front.
        byte[] payload = new byte[decrypted.length - PREAMBLE.length];
        System.arraycopy(decrypted, PREAMBLE.length, payload, 0, payload.length);
        return payload;
    }

    // Capacity is slightly less than SequentialLSB because we use some space for the preamble.
    @Override
    public int getCapacity(BufferedImage image) {
        return baseLSB.getCapacity(image) - PREAMBLE.length;
    }

    // XORs two byte arrays together, element by element.
    // If the key is shorter than the data, it wraps around (key[i % key.length]).
    private byte[] xorBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            // XOR each data byte with the corresponding key byte.
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    // Joins two byte arrays into one by copying them back-to-back.
    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        // Copy array 'a' into the beginning of result.
        System.arraycopy(a, 0, result, 0, a.length);
        // Copy array 'b' right after where 'a' ended.
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
