package com.crown.billing.service;

public class InsufficientTokenException extends RuntimeException {
    public InsufficientTokenException(String message) {
        super(message);
    }
}
