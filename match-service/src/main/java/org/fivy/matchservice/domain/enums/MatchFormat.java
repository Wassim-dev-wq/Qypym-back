package org.fivy.matchservice.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum MatchFormat {
    FIVE_V_FIVE("5v5", 10),
    SIX_V_SIX("6v6", 12),
    SEVEN_V_SEVEN("7v7", 14),
    EIGHT_V_EIGHT("8v8", 16),
    NINE_V_NINE("9v9", 18),
    ELEVEN_V_ELEVEN("11v11", 22);

    private final String format;
    @Getter
    private final int maxPlayers;

    MatchFormat(String format, int maxPlayers) {
        this.format = format;
        this.maxPlayers = maxPlayers;
    }

    @JsonValue
    public String getFormat() {
        return format;
    }

    @JsonCreator
    public static MatchFormat fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();

        for (MatchFormat format : MatchFormat.values()) {
            if (format.getFormat().equals(normalized)) {
                return format;
            }
        }

        throw new IllegalArgumentException("Invalid match format: " + value +
                ". Accepted values are: 5v5, 6v6, 7v7, 8v8, 9v9, 11v11");
    }
}