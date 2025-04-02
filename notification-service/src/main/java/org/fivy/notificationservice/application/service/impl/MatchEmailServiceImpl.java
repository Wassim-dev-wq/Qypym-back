package org.fivy.notificationservice.application.service.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.application.service.MatchEmailService;
import org.fivy.notificationservice.domain.event.email.MatchEmailEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchEmailServiceImpl implements MatchEmailService {

    private final JavaMailSender mailSender;
    private final Configuration freemarkerConfig;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public boolean sendVerificationCodeEmail(MatchEmailEvent event) {
        try {
            Template template = freemarkerConfig.getTemplate("match-verification-email.ftl");

            Map<String, Object> model = new HashMap<>();
            model.put("firstName", event.getFirstName());
            model.put("matchTitle", event.getMatchTitle());
            model.put("matchDate", event.getMatchStartDate().format(DATE_FORMATTER));
            model.put("matchLocation", event.getMatchLocation());
            model.put("matchFormat", event.getMatchFormat());
            model.put("verificationCode", event.getVerificationCode());
            model.put("codeValidityMinutes", event.getVerificationCodeTtl());

            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(event.getEmail());
            helper.setSubject("Code de vérification pour votre match - " + event.getMatchTitle());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.debug("Verification email sent successfully to {}", event.getEmail());
            return true;
        } catch (IOException e) {
            log.error("Failed to load email template for match verification: {}", e.getMessage(), e);
            return false;
        } catch (TemplateException e) {
            log.error("Failed to process email template for match verification: {}", e.getMessage(), e);
            return false;
        } catch (MessagingException e) {
            log.error("Failed to create or send verification email to {}: {}", event.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendMatchReminderEmail(MatchEmailEvent event) {
        try {
            Template template = freemarkerConfig.getTemplate("match-reminder-email.ftl");

            Map<String, Object> model = new HashMap<>();
            model.put("firstName", event.getFirstName());
            model.put("matchTitle", event.getMatchTitle());
            model.put("matchDate", event.getMatchStartDate().format(DATE_FORMATTER));
            model.put("matchLocation", event.getMatchLocation());
            model.put("matchFormat", event.getMatchFormat());

            if (event.getTeamName() != null && !event.getTeamName().isEmpty()) {
                model.put("teamName", event.getTeamName());
            }

            if (event.getPlayerRole() != null && !event.getPlayerRole().isEmpty()) {
                model.put("playerRole", event.getPlayerRole());
            }

            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(event.getEmail());
            helper.setSubject("Rappel: Votre match commence bientôt - " + event.getMatchTitle());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.debug("Reminder email sent successfully to {}", event.getEmail());
            return true;
        } catch (IOException e) {
            log.error("Failed to load email template for match reminder: {}", e.getMessage(), e);
            return false;
        } catch (TemplateException e) {
            log.error("Failed to process email template for match reminder: {}", e.getMessage(), e);
            return false;
        } catch (MessagingException e) {
            log.error("Failed to create or send reminder email to {}: {}", event.getEmail(), e.getMessage(), e);
            return false;
        }
    }
}