package com.momogo.api.auth.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.util.EmailFormatter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendTemporaryPassword(String toEmail, String tempPassword) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("[MoMoGo] 임시 비밀번호 발급 안내");

            String htmlContent = "<div style=\"font-family: 'Malgun Gothic', Arial, sans-serif; font-size: 15px; line-height: 1.6; color: #333;\">"
                    + "<p style=\"font-size: 16px;\">안녕하세요. <strong>MoMoGo</strong> 서비스입니다.</p>"
                    + "<p>요청하신 임시 비밀번호가 발급되었습니다.</p>"
                    + "<div style=\"margin: 20px 0; padding: 15px; background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; display: inline-block;\">"
                    + "임시 비밀번호: <strong style=\"font-size: 20px; color: #6366f1; letter-spacing: 1px;\">" + tempPassword + "</strong>"
                    + "</div>"
                    + "<p style=\"color: #ef4444; font-size: 14px; font-weight: bold;\">※ 본 임시 비밀번호는 발급 후 3분간만 유효합니다.</p>"
                    + "<p>로그인 후 반드시 비밀번호를 변경해 주세요.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("임시 비밀번호 이메일 발송 실패: toEmail: {}", EmailFormatter.mask(toEmail), e);
            throw new BusinessException(GlobalErrorCode.MAIL_SEND_FAILED, "이메일 발송 중 오류가 발생하였습니다.");
        }
    }
}

