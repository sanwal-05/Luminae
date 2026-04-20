package com.hiddeninpixel.exception;

public class StegoException extends Exception {
    public StegoException(String message) {
        super(message);
    }
    
    public StegoException(String message, Throwable cause) {
        super(message, cause);
    }
}
