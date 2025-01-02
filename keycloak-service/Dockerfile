FROM quay.io/keycloak/keycloak:latest as builder

# Build time configuration
ENV KC_DB=postgres \
    KC_FEATURES=token-exchange \
    KC_HEALTH_ENABLED=true \
    KC_METRICS_ENABLED=true \
    KC_HTTP_RELATIVE_PATH=/auth \
    KC_BOOTSTRAP_ADMIN_USER=${KC_ADMIN_USER} \
    KC_BOOTSTRAP_ADMIN_PASSWORD=${KC_ADMIN_PASSWORD}

WORKDIR /opt/keycloak
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Set admin credentials correctly
ENV KC_BOOTSTRAP_ADMIN_USER=${KC_ADMIN_USER} \
    KC_BOOTSTRAP_ADMIN_PASSWORD=${KC_ADMIN_PASSWORD}

USER 1000
EXPOSE 8180

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]