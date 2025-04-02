package org.fivy.userservice.domain.event.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationEvent {
    private String email;
    private String verificationCode;
    private int verificationCodeTtl;
}
