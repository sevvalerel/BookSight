package com.booksight.booksight.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String username) {
        String subject = "BookSight'a Hos Geldin!";
        String body = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;'>"
            + "<h2 style='color:#2D4150;'>Merhaba " + username + "! </h2>"
            + "<p>BookSight hesabin basariyla olusturuldu.</p>"
            + "<p>Artik kitaplari kesfedebilir, yorum yapabilir ve "
            + "AI destekli kisisellestirilmis oneriler alabilirsin.</p>"
            + "<p style='color:#8BC3A3;font-weight:bold;'>Iyi okumalar!</p>"
            + "<hr style='border:1px solid #E5EAEE;'>"
            + "<p style='font-size:12px;color:#6B7A85;'>BookSight Ekibi</p>"
            + "</div>";
        sendHtmlEmail(to, subject, body);
    }

    public void sendPasswordResetCode(String to, String code) {
        String subject = "BookSight - Sifre Sifirlama Kodunuz";
        String body = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;'>"
            + "<h2 style='color:#2D4150;'>Sifre Sifirlama</h2>"
            + "<p>Sifrenizi sifirlamak icin asagidaki kodu kullanin:</p>"
            + "<div style='background:#F5FAF7;border-radius:12px;padding:20px;"
            + "text-align:center;margin:20px 0;'>"
            + "<span style='font-size:32px;font-weight:bold;letter-spacing:8px;"
            + "color:#8BC3A3;'>" + code + "</span></div>"
            + "<p style='color:#6B7A85;font-size:13px;'>"
            + "Bu kod 10 dakika icinde gecerliliini yitirecektir. "
            + "Eger bu islemi siz yapmadiyseniz, bu e-postayi gormezden gelebilirsiniz.</p>"
            + "<hr style='border:1px solid #E5EAEE;'>"
            + "<p style='font-size:12px;color:#6B7A85;'>BookSight Ekibi</p>"
            + "</div>";
        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@booksight.com");
            mailSender.send(message);
            log.info("E-posta gonderildi: {}", to);
        } catch (Exception e) {
            log.error("E-posta gonderilemedi: {}", e.getMessage());
            throw new RuntimeException("E-posta gonderilemedi.");
        }
    }
}
