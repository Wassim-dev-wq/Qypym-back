package org.fivy.matchservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeVerificationRequest {
    @NotBlank(message = "Verification code cannot be empty")
    private String verificationCode;
}