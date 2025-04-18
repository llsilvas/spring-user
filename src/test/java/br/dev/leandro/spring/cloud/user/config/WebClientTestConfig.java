package br.dev.leandro.spring.cloud.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
@Profile("test")  // Ativa apenas para o perfil "test"
public class WebClientTestConfig {

    @Value("${spring.keycloak.admin.auth-server-url}")
    private String authServerUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(authServerUrl)
                .build();
    }


}
