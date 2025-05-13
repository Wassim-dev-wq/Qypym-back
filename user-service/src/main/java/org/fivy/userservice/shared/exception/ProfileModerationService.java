package org.fivy.userservice.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProfileModerationService {

    public void validateProfileContent(UpdateProfileRequest request) {
        log.debug("Validating profile content for inappropriate material");
        if (request.getFirstName() != null) {
            validateField(request.getFirstName(), "firstName", "Le prénom", "First name");
        }
        if (request.getLastName() != null) {
            validateField(request.getLastName(), "lastName", "Le nom", "Last name");
        }
        if (request.getBio() != null) {
            validateField(request.getBio(), "bio", "La bio", "Bio");
        }
        if (request.getUsername() != null) {
            validateField(request.getUsername(), "username", "Le nom d'utilisateur", "Username");
        }
        log.debug("Profile content validation completed successfully");
    }

    private void validateField(String text, String fieldName, String frenchFieldLabel, String englishFieldLabel) {
        if (ContentModerationUtil.containsInappropriateContent(text)) {
            log.warn("Inappropriate content detected in {}: {}", fieldName, text);
            String errorMessage = String.format("%s contient du contenu inapproprié. %s contains inappropriate content.",
                    frenchFieldLabel, englishFieldLabel);
            throw new UserException(
                    errorMessage,
                    "INAPPROPRIATE_CONTENT",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}