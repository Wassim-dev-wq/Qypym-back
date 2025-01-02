package org.fivy.authservice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.fivy.authservice.shared.exception.AuthException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
@Slf4j
public class KeycloakConfig {

    @Bean
    public Keycloak keycloakInstance(KeycloakProperties properties) {
        log.info("Initializing Keycloak instance with realm: {}", properties.getRealm());
        return KeycloakBuilder.builder().serverUrl(properties.getAuthServerUrl()).realm(properties.getRealm()).clientId(properties.getClientId()).clientSecret(properties.getClientSecret()).grantType(OAuth2Constants.CLIENT_CREDENTIALS).build();
    }

    @Bean
    public RestTemplate keycloakRestTemplate(KeycloakProperties properties) {
        RestTemplate restTemplate = new RestTemplate();

        if (!properties.getEnableSslVerification()) {
            restTemplate.setRequestFactory(disabledSslRequestFactory());
        }

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                throw new AuthException("Keycloak API error: " + response.getStatusText(), "KEYCLOAK_" + response.getStatusCode().value(), HttpStatus.valueOf(response.getStatusCode().value()));
            }
        });

        return restTemplate;
    }

    private ClientHttpRequestFactory disabledSslRequestFactory() {
        try {
            TrustAllStrategy acceptingTrustStrategy = new TrustAllStrategy();

            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(acceptingTrustStrategy).build();

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", new SSLConnectionSocketFactory(sslContext)).register("http", new PlainConnectionSocketFactory()).build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(100);
            cm.setDefaultMaxPerRoute(20);

            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();

            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000);
            requestFactory.setConnectionRequestTimeout(5000);

            return requestFactory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }
}