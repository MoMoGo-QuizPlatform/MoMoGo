package com.momogo.api.auth.exception;

public class JwtSerializationException extends RuntimeException {

    public JwtSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
