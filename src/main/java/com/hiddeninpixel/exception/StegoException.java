// StegoException is the base class for all errors that can happen in this app.
// By creating our own exception type, the code can catch any steganography-related
// error with a single "catch (StegoException e)" block if needed.
//
// It extends Java's built-in "Exception" class, which gives it all the standard
// behavior like storing a message and a cause.
package com.hiddeninpixel.exception;

public class StegoException extends Exception {

    // Constructor that takes just a human-readable message describing what went wrong.
    public StegoException(String message) {
        super(message);
    }

    // Constructor that takes both a message AND the original underlying error that caused this.
    // Wrapping the cause like this preserves the full error chain for debugging.
    public StegoException(String message, Throwable cause) {
        super(message, cause);
    }
}
