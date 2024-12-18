package org.fivy.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthConverter.class);
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Value("${keycloak.client-id}")
    private String clientId;

    private void addUserHeaders(ServerWebExchange exchange, Jwt jwt) {
        if (exchange != null) {
            String userId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("name");
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            String roles = "";
            if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
                if (clientAccess != null && clientAccess.containsKey("roles")) {
                    Collection<String> rolesList = (Collection<String>) clientAccess.get("roles");
                    roles = String.join(",", rolesList);
                }
            }
            exchange.getRequest().mutate()
                    .header("X-User-ID", userId != null ? userId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Name", name != null ? name : "")
                    .header("X-User-Roles", roles)
                    .build();
            logger.debug("Added user headers - UserId: {}, Email: {}, Roles: {}",
                    userId, email, roles);
        }
    }

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        logger.info("Starting JWT conversion process");
        logger.debug("Received JWT: {}", jwt.getTokenValue());
        Collection<GrantedAuthority> defaultAuthorities = jwtGrantedAuthoritiesConverter.convert(jwt);
        logger.debug("Default authorities extracted: {}", defaultAuthorities);
        Collection<GrantedAuthority> allAuthorities = Stream.concat(
                defaultAuthorities.stream(),
                extractAllRoles(jwt).stream()
        ).collect(Collectors.toSet());
        logger.info("Final combined authorities: {}", allAuthorities);
        return Mono.deferContextual(contextView -> {
            if (contextView.hasKey(ServerWebExchange.class)) {
                ServerWebExchange exchange = contextView.get(ServerWebExchange.class);
                addUserHeaders(exchange, jwt);
            }
            return Mono.just(new JwtAuthenticationToken(jwt, allAuthorities));
        });
    }

    private Collection<GrantedAuthority> extractAllRoles(Jwt jwt) {
        Set<GrantedAuthority> roles = new HashSet<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            roles.addAll(extractRoles(realmRoles));
        }
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
            if (clientAccess != null && clientAccess.containsKey("roles")) {
                Collection<String> clientRoles = (Collection<String>) clientAccess.get("roles");
                roles.addAll(extractRoles(clientRoles));
            }
        }
        return roles;
    }

    private Set<GrantedAuthority> extractRoles(Collection<String> roles) {
        return roles.stream()
                .map(role -> {
                    String authorityRole = "ROLE_" + role.toUpperCase();
                    logger.debug("Converting role {} to authority {}", role, authorityRole);
                    return new SimpleGrantedAuthority(authorityRole);
                })
                .collect(Collectors.toSet());
    }
}