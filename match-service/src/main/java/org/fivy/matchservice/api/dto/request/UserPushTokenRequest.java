package org.fivy.matchservice.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPushTokenRequest {
    private UUID userId;

    @JsonProperty("token")
    private String expoToken;
}
