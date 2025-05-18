package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
@Profile("!test")  // Ativa para todos os perfis exceto "test"
public class WebClientConfig {

    @Bean
    public WebClient webClient(KeycloakProperties keycloakProperties) {
        log.info("KeycloakProperties URL: " + keycloakProperties.getAuthServerUrl());
        return WebClient.builder()
                .baseUrl(keycloakProperties.getAuthServerUrl())
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(5))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                ))
                .build();
    }
}
