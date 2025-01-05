package org.fivy.apigateway.config;

import org.fivy.apigateway.filter.UserHeadersFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class GatewayConfiguration {

    @Autowired
    private UserHeadersFilter userHeadersFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .preserveHostHeader()
                                .filter(userHeadersFilter)
                        )
                        .uri("http://localhost:9090"))
                .build();
    }
}