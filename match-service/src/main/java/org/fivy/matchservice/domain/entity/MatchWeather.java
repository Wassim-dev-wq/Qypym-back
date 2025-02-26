package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_weather")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchWeather {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Column(nullable = false)
    private Integer weatherId;

    @Column(nullable = false)
    private Integer temperature;

    @Column(nullable = false)
    private String condition;

    @Column(nullable = false)
    private Integer humidity;

    @Column(nullable = false)
    private Integer windSpeed;

    @Column(nullable = false)
    private Integer cloudCoverage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
