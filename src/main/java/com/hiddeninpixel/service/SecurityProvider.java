// SecurityProvider handles all the cryptographic (security-related) operations.
// Its two jobs are:
// 1. Turn a user's password into a stream of secret bytes (for XOR encryption).
// 2. Turn a user's password into a number (to seed a random number generator).
//
// It uses PBKDF2 (Password-Based Key Derivation Function 2), a standard algorithm
// that is deliberately slow to compute. This makes it hard for attackers to guess
// passwords by trying millions of them quickly (brute-force attacks).
package com.hiddeninpixel.service;

// These are Java's built-in cryptography classes.
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class SecurityProvider {

    // The "salt" is extra random data mixed into the password before hashing.
    // Using a salt means two people with the same password still get different keys
    // if they use different salts. Our salt is fixed for this project.
    private static final byte[] SALT = "Luminae2025".getBytes();

    // How many times PBKDF2 repeats the hashing process internally.
    // More iterations = slower = harder to crack. 10,000 is a reasonable balance.
    private static final int ITERATIONS = 10000;

    // Takes a user's password and generates a byte array of the requested length.
    // This byte array is used as the encryption key in XOR-LSB.
    // The same password always produces the same key (deterministic), which is
    // essential so the receiver can decrypt with the same key used to encrypt.
    public byte[] deriveKeyStream(String password, int length) {
        try {
            // PBEKeySpec is the recipe: "hash this password with this salt, these many times,
            // and give me 'length * 8' bits of output". (* 8 because the parameter is in bits)
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, length * 8);

            // PBKDF2WithHmacSHA256 is the specific algorithm name Java understands.
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            // Actually run the computation and get the resulting key bytes.
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Copy the bytes into a new array of exactly the requested length.
            byte[] keyStream = new byte[length];
            System.arraycopy(hash, 0, keyStream, 0, Math.min(hash.length, length));
            return keyStream;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // If the algorithm is not available, the whole app cannot work securely.
            throw new RuntimeException("PBKDF2 failed", e);
        }
    }

    // Converts a password into a single long integer (64-bit number).
    // This number is used as the seed for the random number generator in StochasticLSB.
    // The same password always gives the same seed, so both sides pick the same "random" pixels.
    public long passwordToSeed(String password) {
        // Derive 8 bytes from the password (8 bytes = 64 bits = one long).
        byte[] hash = deriveKeyStream(password, 8);

        // Stitch the 8 bytes together into one 64-bit long integer.
        // We shift the existing seed left 8 bits each time to make room for the next byte.
        long seed = 0;
        for (int i = 0; i < 8; i++) {
            // "& 0xFF" converts a signed byte (-128 to 127) into an unsigned value (0 to 255).
            seed = (seed << 8) | (hash[i] & 0xFF);
        }
        return seed;
    }
}
