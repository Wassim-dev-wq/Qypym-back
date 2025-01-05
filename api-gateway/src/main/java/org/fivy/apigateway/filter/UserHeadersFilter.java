package org.fivy.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Component
public class UserHeadersFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(UserHeadersFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .filter(ctx -> ctx.getAuthentication() != null)
                .filter(ctx -> ctx.getAuthentication() instanceof JwtAuthenticationToken)
                .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication())
                .map(auth -> {
                    logger.debug("Processing JWT authentication for headers");

                    String userId = auth.getToken().getClaimAsString("sub");
                    String email = auth.getToken().getClaimAsString("email");

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-ID", userId != null ? userId : "")
                            .header("X-User-Email", email != null ? email : "")
                            .build();

                    logger.debug("Added user headers to request - UserId: {}, Email: {}", userId, email);

                    return exchange.mutate()
                            .request(mutatedRequest)
                            .build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }
}