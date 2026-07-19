package com.thalicloud.delivery.exception;

// FR-2.9 — thrown from the submit endpoint when required fields/documents are missing.
public class RegistrationIncompleteException extends RuntimeException {
    public RegistrationIncompleteException(String message) {
        super(message);
    }
}
