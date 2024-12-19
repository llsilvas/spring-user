package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.KeycloakProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(KeycloakProperties keycloakProperties) {
        return WebClient.builder()
                .baseUrl(keycloakProperties.getAuthServerUrl())
                .build();
    }
}

