package org.fivy.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("lb://USER-SERVICE"))
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**", "/api/v1/user-push-tokens/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("lb://NOTIFICATION-SERVICE"))
                .route("match-service", r -> r
                        .path("/api/v1/matches/**","/api/v1/player/**","/api/v1/feedback/**")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("lb://MATCH-SERVICE"))
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("lb://AUTH-SERVICE"))
                .build();
    }
}