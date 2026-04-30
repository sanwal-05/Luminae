// Thrown when the password used to extract a message does not match the one used to hide it.
// Also thrown when data inside the image looks corrupted or was not hidden by this app.
// Giving this its own class lets the UI show "wrong password" messages clearly.
package com.hiddeninpixel.exception;

public class InvalidKeyException extends StegoException {
    public InvalidKeyException(String message) {
        super(message);
    }
}
