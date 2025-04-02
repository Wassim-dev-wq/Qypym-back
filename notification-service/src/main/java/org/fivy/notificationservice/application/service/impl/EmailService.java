package org.fivy.notificationservice.application.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Configuration freemarkerConfig;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public boolean sendVerificationEmail(String toEmail, String verificationCode, int codeExpiration) {
        try {
            Template template = freemarkerConfig.getTemplate("verification-code-email.ftl");
            Map<String, Object> model = new HashMap<>();
            model.put("code", verificationCode);
            model.put("codeExpiration", codeExpiration);
            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Vérification de votre adresse email - FIVY");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
            return true;
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Failed to send verification email to {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetCode, int expirationMinutes) {
        try {
            log.info("Preparing password reset email for: {}", toEmail);

            Template template = freemarkerConfig.getTemplate("password-reset-email.ftl.ftl");
            Map<String, Object> model = new HashMap<>();
            model.put("resetCode", resetCode);
            model.put("expirationMinutes", expirationMinutes);
            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Réinitialisation de votre mot de passe - FIVY");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
            return true;
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}