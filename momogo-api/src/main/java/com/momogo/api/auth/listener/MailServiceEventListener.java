package com.momogo.api.auth.listener;

import com.momogo.api.auth.service.MailService;
import com.momogo.core.common.config.AsyncConfig;
import com.momogo.core.domain.user.event.TemporaryPasswordGeneratedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MailServiceEventListener {

    private final MailService mailService;

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTemporaryPasswordEvent(TemporaryPasswordGeneratedEvent event) {
        mailService.sendTemporaryPassword(event.email(), event.tempPassword());
    }
}
