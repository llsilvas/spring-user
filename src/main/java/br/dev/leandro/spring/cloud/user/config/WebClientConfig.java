package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Profile("!test")  // Ativa para todos os perfis exceto "test"
public class WebClientConfig {

    @Bean
    public WebClient webClient(KeycloakProperties keycloakProperties) {
        return WebClient.builder()
                .baseUrl(keycloakProperties.getAuthServerUrl())
                .build();
    }
}
