package org.fivy.notificationservice.application.service;

import org.fivy.notificationservice.domain.event.email.MatchEmailEvent;

public interface MatchEmailService {
    boolean sendVerificationCodeEmail(MatchEmailEvent event);
    boolean sendMatchReminderEmail(MatchEmailEvent event);
}
