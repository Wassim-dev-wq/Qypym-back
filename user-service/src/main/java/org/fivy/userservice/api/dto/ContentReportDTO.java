package org.fivy.userservice.api.dto;

import lombok.*;
import org.fivy.userservice.domain.enums.ReportStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReportDTO {
    private UUID id;
    private UUID reporterId;
    private UUID reportedUserId;
    private String details;
    private ReportStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}