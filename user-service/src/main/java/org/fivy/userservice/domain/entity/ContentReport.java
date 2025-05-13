package org.fivy.userservice.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.fivy.userservice.domain.enums.ReportStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReport {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID reporterId;

    @Column(nullable = false)
    private UUID reportedUserId;

    @Size(max = 500)
    @Column(length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        status = ReportStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}