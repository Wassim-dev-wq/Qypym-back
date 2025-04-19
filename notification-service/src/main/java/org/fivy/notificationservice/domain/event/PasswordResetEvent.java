package org.fivy.notificationservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEvent {
    private String email;
    private String resetCode;
    private int resetCodeTtl;
}
