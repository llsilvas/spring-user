package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import br.dev.leandro.spring.cloud.user.utils.TokenUtils;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
//@Profile("!test")  // Ativa para todos os perfis exceto "test"
public class WebClientConfig {

    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector() {
        return new ReactorClientHttpConnector(
                HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(5))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
        );
    }

    @Bean("keycloakWebClient")
    public WebClient keycloakWebClient(
            KeycloakProperties keycloakProperties,
            ReactorClientHttpConnector connector
    ) {
        log.info("KeycloakProperties URL: {}", keycloakProperties.getAuthServerUrl());
        return WebClient.builder()
                .baseUrl(keycloakProperties.getAuthServerUrl())
                .clientConnector(connector)
                .build();
    }

    @Bean("eventWebClient")
    public WebClient eventWebClient(
            @Value("${event.url}") String eventUrl
    ) {
        log.info("Event URL: {}", eventUrl);
        return WebClient.builder()
                .baseUrl(eventUrl)
                .filter(propagateToken())
                .build();
    }

    private ExchangeFilterFunction propagateToken() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest ->
                Mono.deferContextual(ctx -> {
                    String token = TokenUtils.getToken(ctx);
                    log.debug("Token JWT propagado: {}...", token != null ? token.substring(0, 10) : "null");
                    if (token != null) {
                        ClientRequest newRequest = ClientRequest.from(clientRequest)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .build();
                        return Mono.just(newRequest);
                    }
                    return Mono.just(clientRequest);
                })
        );
    }
}
