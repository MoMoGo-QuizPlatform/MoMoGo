package com.momogo.api.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendTemporaryPassword(String toEmail, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[MoMoGo] 임시 비밀번호 발급 안내");
        message.setText("안녕하세요. MoMoGo 서비스입니다.\n\n"
                + "요청하신 임시 비밀번호가 발급되었습니다.\n"
                + "임시 비밀번호: [" + tempPassword + "]\n\n"
                + "본 임시 비밀번호는 발급 후 10분간만 유효합니다.\n"
                + "로그인 후 반드시 비밀번호를 변경해 주세요."
        );
        mailSender.send(message);
    }
}

