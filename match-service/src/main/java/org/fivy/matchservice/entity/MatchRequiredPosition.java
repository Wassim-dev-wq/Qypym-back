package org.fivy.matchservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "\"MatchRequiredPositions\"")
@IdClass(MatchRequiredPositionId.class)
public class MatchRequiredPosition {

    @Id
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "matchId", nullable = false)
    private UUID matchId;

    @Id
    @NotNull
    @Column(name = "positionid", nullable = false)
    private UUID positionId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}