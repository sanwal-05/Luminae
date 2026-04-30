// Thrown when the user tries to hide a message that is too large to fit inside the image.
// Each image has a maximum capacity based on its size (number of pixels).
// If the message exceeds that limit, we throw this specific error so the UI can
// show a helpful and specific message to the user.
package com.hiddeninpixel.exception;

public class InsufficientCapacityException extends StegoException {
    public InsufficientCapacityException(String message) {
        super(message);
    }
}
