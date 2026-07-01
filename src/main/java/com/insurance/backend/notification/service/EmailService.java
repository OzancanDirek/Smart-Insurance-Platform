package com.insurance.backend.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService
{

    private final JavaMailSender mailSender;

    @Async
    public void sendClaimStatusEmail(String toEmail, String customerName, String claimTitle, String status)
    {
        try
        {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Hasar Başvurunuz Güncellendi - " + claimTitle);

            boolean isApproved = status.equals("APPROVED");
            String statusText = isApproved ? "Onaylandı" : "Reddedildi";
            String statusColor = isApproved ? "#059669" : "#DC2626";
            String statusBg = isApproved ? "#D1FAE5" : "#FEE2E2";
            String statusIcon = isApproved ? "✅" : "❌";

            String html = "<!DOCTYPE html>" +
                    "<html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>" +
                    "<div style='max-width:560px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>" +
                    "<div style='background:#1B3A6B;padding:32px 40px;'>" +
                    "<span style='color:#fff;font-size:18px;font-weight:700;'>🛡 InsuranceOS</span>" +
                    "</div>" +
                    "<div style='padding:40px;'>" +
                    "<p style='color:#888;font-size:13px;margin:0 0 8px;'>Sayın,</p>" +
                    "<h1 style='color:#1a1a2e;font-size:22px;font-weight:700;margin:0 0 24px;'>" + customerName + "</h1>" +
                    "<p style='color:#555;font-size:15px;line-height:1.6;margin:0 0 24px;'>Aşağıdaki hasar başvurunuzun durumu güncellenmiştir.</p>" +
                    "<div style='background:#f9f9f9;border-radius:8px;padding:20px;margin-bottom:24px;'>" +
                    "<p style='color:#888;font-size:12px;font-weight:600;text-transform:uppercase;margin:0 0 6px;'>Başvuru</p>" +
                    "<p style='color:#1a1a2e;font-size:16px;font-weight:600;margin:0;'>" + claimTitle + "</p>" +
                    "</div>" +
                    "<div style='text-align:center;margin-bottom:32px;'>" +
                    "<div style='display:inline-block;background:" + statusBg + ";color:" + statusColor + ";padding:12px 32px;border-radius:24px;font-size:16px;font-weight:700;'>" +
                    statusIcon + " " + statusText +
                    "</div></div>" +
                    "<p style='color:#555;font-size:14px;line-height:1.6;margin:0 0 24px;'>Detaylı bilgi için sisteme giriş yapabilirsiniz.</p>" +
                    "<div style='text-align:center;'>" +
                    "<a href='http://localhost:3000/claims' style='display:inline-block;background:#1B3A6B;color:#fff;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;text-decoration:none;'>Başvurularımı Görüntüle</a>" +
                    "</div></div>" +
                    "<div style='padding:24px 40px;border-top:1px solid #f0f0f0;text-align:center;'>" +
                    "<p style='color:#aaa;font-size:12px;margin:0;'>© 2026 InsuranceOS · Tüm hakları saklıdır</p>" +
                    "</div></div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Email gönderilemedi: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token)
    {
        System.out.println("EMAIL GÖNDERİLİYOR: " + to);
        try
        {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("InsuranceOS — Şifre Sıfırlama");
            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            message.setText("Merhaba " + firstName + ",\n\nŞifre sıfırlama linkiniz:\n" + resetLink + "\n\nBu link 1 saat geçerlidir.");
            mailSender.send(message);
            System.out.println("EMAIL GÖNDERİLDİ");
        }
        catch (Exception e)
        {
            System.out.println("EMAIL HATASI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}