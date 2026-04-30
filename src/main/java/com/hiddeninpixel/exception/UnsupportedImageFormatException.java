// Thrown when the user tries to load a file that is not in a supported format.
// Currently, only PNG images are supported because PNG is lossless.
// JPEG compression would corrupt any hidden data, so we reject it with this specific error.
package com.hiddeninpixel.exception;

public class UnsupportedImageFormatException extends StegoException {
    public UnsupportedImageFormatException(String message) {
        super(message);
    }
}
