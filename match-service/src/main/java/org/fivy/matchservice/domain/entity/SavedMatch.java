package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private ZonedDateTime savedAt;

    @Version
    private Long version;
}