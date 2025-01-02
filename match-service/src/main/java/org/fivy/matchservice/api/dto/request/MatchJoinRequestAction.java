package org.fivy.matchservice.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchJoinRequestAction {
    @NotNull
    private String action;
}