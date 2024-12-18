package org.fivy.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordRequest {
    private String email;
}
