package org.fivy.authservice.api.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutRequest {
    private String userId;
    private String refreshToken;
}
