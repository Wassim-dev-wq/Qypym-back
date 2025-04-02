package org.fivy.matchservice.domain.enums;

import lombok.Data;

public enum NotificationType {
    MATCH_CREATED,
    MATCH_UPDATED,
    MATCH_DELETED,
    MATCH_STATUS_CHANGED,
    PLAYER_JOINED,
    PLAYER_LEFT,
    JOIN_REQUEST_RECEIVED,
    JOIN_REQUEST_ACCEPTED,
    JOIN_REQUEST_REJECTED
}


