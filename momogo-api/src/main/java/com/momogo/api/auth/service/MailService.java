package com.momogo.api.auth.service;

public interface MailService {

    void sendTemporaryPassword(String toEmail, String tempPassword);
}
