package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "match_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private Integer teamNumber;

    @Column(nullable = false)
    private String name;
}