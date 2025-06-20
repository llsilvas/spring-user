package br.dev.leandro.spring.cloud.user.utils;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class WebClientUtils {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    private final WebClient webClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public WebClientUtils(@Qualifier("keycloakWebClient") WebClient webClient, KeycloakProperties keycloakProperties) {
        this.webClient = webClient;
        this.realm = keycloakProperties.getRealm();
        this.clientId = keycloakProperties.getClientId();
        this.clientSecret = keycloakProperties.getClientSecret();
    }

    public WebClient.RequestHeadersSpec<?> createPostRequest(String token, String uriTemplate, Object body, Map<String, Object> uriVariables) {
        var uri = buildUri(uriTemplate, uriVariables);
        return webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    public WebClient.RequestHeadersSpec<?> createPutRequest(String token, String uriTemplate, Object body, Map<String, Object> uriVariables) {
        var uri = buildUri(uriTemplate, uriVariables);
        return webClient.put()
                .uri(uri)
                .header(AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    private String buildUri(String uriTemplate, Map<String, Object> uriVariables) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("realm", this.realm);
        if (uriVariables != null) {
            vars.putAll(uriVariables);
        }
        String uri = UriComponentsBuilder.fromUriString(uriTemplate)
                .buildAndExpand(vars)
                .toString();
        log.info("URI construída: {}", uri);
        return uri;
    }

    public WebClient.RequestHeadersSpec<?> createGetRequest(String token, String uriTemplate) {
        var uri = buildUri(uriTemplate, null);
        return webClient.get()
                .uri(uri)
                .header(AUTHORIZATION, BEARER + token);
    }

    public WebClient.RequestHeadersSpec<?> createGetRequest(String token, String uriTemplate, Map<String, Object> id) {
        var uri = buildUri(uriTemplate, id);
        return webClient.get().uri(uri)
                .header(AUTHORIZATION, BEARER + token);
    }

    public WebClient.RequestHeadersSpec<?> createDeleteRequest(String token, String uriTemplate, Map<String, Object> uriVariables) {
        var uri = buildUri(uriTemplate, uriVariables);
        return webClient.delete()
                .uri(uri)
                .header(AUTHORIZATION, BEARER + token);

    }


    public Mono<String> getAdminAccessToken() {
        var uri = buildUri("/realms/{realm}/protocol/openid-connect/token", null);
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new BadCredentialsException("Token inválido ou expirado."));
                    }
                    return response.createException().flatMap(Mono::error);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Erro interno ao obter o token.")))
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {}) // Mapeia para Map
                .map(response -> response.get("access_token"))
                .doOnSuccess(token -> System.out.println("Token recebido: " + token))
                .doOnError(error -> System.err.println("Erro ao obter token: " + error.getMessage()));
    }
}

