FROM quay.io/keycloak/keycloak:latest as builder

# Configure HTTP during build
ENV KC_HTTP_ENABLED=true
ENV KC_HTTPS_REQUIRED=false
ENV KC_HTTP_PORT=8180
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false

WORKDIR /opt/keycloak
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENV KC_DB=postgres
ENV KC_DB_URL=jdbc:postgresql://db:5432/usersdb
ENV KC_DB_USERNAME=wassim
ENV KC_DB_PASSWORD=wassim
ENV KC_HOSTNAME=keycloak-service
ENV KC_HOSTNAME_PORT=8180
ENV KC_HTTP_PORT=8180
ENV KC_HTTP_ENABLED=true
ENV KC_HTTPS_REQUIRED=false

EXPOSE 8180

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--http-enabled=true", "--hostname-strict=false"]