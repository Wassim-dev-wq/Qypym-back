package org.fivy.matchservice.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum PlayerRole {
    CREATOR,
    PLAYER,
    SUBSTITUTE,
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    FORWARD,
    STRIKER,
    UNKNOWN,
}