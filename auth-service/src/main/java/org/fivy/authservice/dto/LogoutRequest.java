package org.fivy.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutRequest {
    private String userId;
    private String refreshToken;
}
