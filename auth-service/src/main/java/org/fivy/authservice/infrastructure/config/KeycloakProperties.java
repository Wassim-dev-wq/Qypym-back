package org.fivy.authservice.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "keycloak")
@Validated
@Data
public class KeycloakProperties {
    @NotBlank
    private String authServerUrl;

    @NotBlank
    private String realm;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    private Integer tokenValiditySeconds = 300;
    private Integer refreshTokenValiditySeconds = 3600;
    private Boolean enableSslVerification = true;
}
