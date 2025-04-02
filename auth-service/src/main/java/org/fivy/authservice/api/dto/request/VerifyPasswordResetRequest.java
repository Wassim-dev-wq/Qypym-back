package org.fivy.authservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasswordResetRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Verification code is required")
    private String code;
}