package org.fivy.matchservice.domain.event;

public enum MatchEventType {
    MATCH_CREATED,
    MATCH_UPDATED,
    MATCH_DELETED,
    MATCH_STATUS_UPDATED,
    PLAYER_STATUS_UPDATED,
    PLAYER_JOINED,
    PLAYER_LEFT,
    MATCH_STARTED,
    MATCH_COMPLETED,
    MATCH_CANCELLED,
    MATCH_SAVED,
    MATCH_UNSAVED
}