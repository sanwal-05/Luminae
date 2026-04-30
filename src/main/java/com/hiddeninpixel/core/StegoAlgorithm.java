// StegoAlgorithm is an INTERFACE, not a real class.
// An interface is like a job description or a contract.
// It says: "any class that wants to be a steganography algorithm MUST provide these three methods."
// It does not contain any code itself, just the method signatures (names and parameters).
//
// Why do we have this? Because there are 4 different algorithms (Sequential, XOR, Stochastic, Metadata).
// The UI only needs to call "algorithm.hide()" without caring WHICH algorithm it is.
// This is called the "Strategy" design pattern.
// You can swap one algorithm for another without changing any UI code at all.
// It also follows the Open/Closed Principle: open for adding new algorithms, closed for modifying old code.
package com.hiddeninpixel.core;

// BufferedImage is how Java holds an image in memory (a grid of pixels).
import java.awt.image.BufferedImage;

// Our base error type so callers know what exceptions to expect.
import com.hiddeninpixel.exception.StegoException;

public interface StegoAlgorithm {

    /**
     * Hides secret data inside a cover image.
     * @param cover   The original image we are hiding data inside.
     * @param payload The secret message converted to a byte array.
     * @param key     The password. Some algorithms need it, some do not (pass null if not needed).
     * @return        A new image that looks like the original but contains the hidden message.
     * @throws StegoException if the message is too large or any other problem occurs.
     */
    BufferedImage hide(BufferedImage cover, byte[] payload, String key) throws StegoException;

    /**
     * Extracts hidden data from a stego image.
     * @param stego The image that contains a hidden message.
     * @param key   The password. Must match the one used when hiding. Pass null if not needed.
     * @return      The extracted message as a byte array.
     * @throws StegoException if the password is wrong, data is corrupted, or extraction fails.
     */
    byte[] extract(BufferedImage stego, String key) throws StegoException;

    /**
     * Calculates how many bytes of secret data can be hidden inside this image.
     * Larger images can hold more data.
     * @param image The image to check.
     * @return      The maximum number of bytes that can be hidden.
     */
    int getCapacity(BufferedImage image);
}
