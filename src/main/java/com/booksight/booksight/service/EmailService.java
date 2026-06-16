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
        String subject = "BookSight'a Hoş Geldin!";
        String body = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;'>"
                + "<h2 style='color:#2D4150;'>Merhaba " + username + "! </h2>"
                + "<p>BookSight hesabın başarıyla oluşturuldu.</p>"
                + "<p>Artık kitapları keşfedebilir, yorum yapabilir ve "
                + "AI destekli kişiselleştirilmiş öneriler alabilirsin.</p>"
                + "<p style='color:#8BC3A3;font-weight:bold;'>İyi okumalar!</p>"
                + "<hr style='border:1px solid #E5EAEE;'>"
                + "<p style='font-size:12px;color:#6B7A85;'>BookSight Ekibi</p>"
                + "</div>";
        sendHtmlEmail(to, subject, body);
    }

    public void sendPasswordResetCode(String to, String code) {
        String subject = "BookSight - Şifre Sıfırlama Kodunuz";
        String body = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;'>"
                + "<h2 style='color:#2D4150;'>Şifre Sıfırlama</h2>"
                + "<p>Şifrenizi sıfırlamak için aşağıdaki kodu kullanın:</p>"
                + "<div style='background:#F5FAF7;border-radius:12px;padding:20px;"
                + "text-align:center;margin:20px 0;'>"
                + "<span style='font-size:32px;font-weight:bold;letter-spacing:8px;"
                + "color:#8BC3A3;'>" + code + "</span></div>"
                + "<p style='color:#6B7A85;font-size:13px;'>"
                + "Bu kod 10 dakika içinde geçerliliğini yitirecektir. "
                + "Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>"
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
            log.info("E-posta gönderildi: {}", to);
        } catch (Exception e) {
            log.error("E-posta gönderilemedi: {}", e.getMessage());
            throw new RuntimeException("E-posta gönderilemedi.");
        }
    }
}