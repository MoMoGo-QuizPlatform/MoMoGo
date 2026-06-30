package com.momogo.core.common.security;

public interface PasswordEncryptor {
    String encrypt(String rawPassword);
    boolean matches(String rawPassword, String encryptedPassword);
}
