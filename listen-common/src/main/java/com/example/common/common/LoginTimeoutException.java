package com.example.common.common;

public class LoginTimeoutException extends RuntimeException {
    public LoginTimeoutException(String message) {
        super(message);
    }
}
