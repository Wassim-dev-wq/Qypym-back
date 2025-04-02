package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.FeedbackRequestStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequestResponse {
    private UUID id;
    private UUID matchId;
    private String matchTitle;
    private FeedbackRequestStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiryAt;
    private int feedbackCount;
    private int totalPlayersInMatch;
    private boolean userHasSubmitted;
}