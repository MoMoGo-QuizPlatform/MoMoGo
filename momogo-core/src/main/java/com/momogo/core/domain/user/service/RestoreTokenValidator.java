package com.momogo.core.domain.user.service;

public interface RestoreTokenValidator {

    String getEmailFromRestoreToken(String restoreToken);
}
