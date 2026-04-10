package com.crown.billing.service;

import lombok.Getter;

@Getter
public class InsufficientTokenException extends RuntimeException {

    private final int required;
    private final int balance;

    public InsufficientTokenException(String message) {
        super(message);
        this.required = 0;
        this.balance = 0;
    }

    public InsufficientTokenException(String message, int required, int balance) {
        super(message);
        this.required = required;
        this.balance = balance;
    }
}
