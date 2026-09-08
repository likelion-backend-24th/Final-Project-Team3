package com.example.memberservice.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code, long validityMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[컨퍼런스] 이메일 인증 코드");
        message.setText("인증코드: " + code + "\n" + validityMinutes + "분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}
