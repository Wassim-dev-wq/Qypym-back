package org.fivy.authservice.api.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPasswordRequest {
    private String userId;
    private String newPassword;
}
