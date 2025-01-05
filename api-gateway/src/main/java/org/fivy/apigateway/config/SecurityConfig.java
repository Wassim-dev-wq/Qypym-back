package org.fivy.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Initializing SecurityWebFilterChain configuration");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> {
                    logger.debug("Configuring authorization rules");
                    exchange
                            .pathMatchers("/api/v1/auth/**").permitAll()
                            .pathMatchers("/api/v1/users/**").hasRole("USER")
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token ->
                                Mono.deferContextual(contextView -> {
                                    if (contextView.hasKey(ServerWebExchange.class)) {
                                        return jwtAuthConverter.convert(token);
                                    }
                                    return jwtAuthConverter.convert(token);
                                })
                        ))
                )
                .build();
    }
}