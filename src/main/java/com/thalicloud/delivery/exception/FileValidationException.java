package com.thalicloud.delivery.exception;

// FR-2.6 — file size / format rejected server-side.
public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}
