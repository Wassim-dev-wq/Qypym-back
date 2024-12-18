package org.fivy.matchservice.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class MatchRequiredPositionId implements Serializable {
    private UUID matchId;
    private UUID positionId;

    public MatchRequiredPositionId() {
    }

    public MatchRequiredPositionId(UUID matchId, UUID positionId) {
        this.matchId = matchId;
        this.positionId = positionId;
    }
}