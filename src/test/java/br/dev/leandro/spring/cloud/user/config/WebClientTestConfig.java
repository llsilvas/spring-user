package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
@Profile("test")  // Ativa apenas para o perfil "test"
public class WebClientTestConfig {

    @Bean
    public WebClient webClient(KeycloakProperties keycloakProperties) {
        return WebClient.builder()
                .baseUrl(keycloakProperties.getAuthServerUrl())
                .build();
    }
}
