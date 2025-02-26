package org.fivy.matchservice.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchJoinRequestDto {

    private UUID preferredTeamId;
    private String position;
    private String experience;
    private String personalNote;
    private boolean available;
    @Size(max = 500, message = "Message cannot be more than 500 characters")
    private String message;
}