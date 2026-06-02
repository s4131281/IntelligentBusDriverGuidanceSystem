package com.rmit.intelligentbus.service;

/**
 * Thrown when a business rule or validation rule fails.
 */
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) {
        super(message);
    }
}
