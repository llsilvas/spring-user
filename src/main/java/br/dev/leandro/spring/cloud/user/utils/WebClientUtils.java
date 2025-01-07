package br.dev.leandro.spring.cloud.user.utils;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class WebClientUtils {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    private final WebClient webClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public WebClientUtils(WebClient webClient, KeycloakProperties keycloakProperties) {
        this.webClient = webClient;
        this.realm = keycloakProperties.getRealm();
        this.clientId = keycloakProperties.getClientId();
        this.clientSecret = keycloakProperties.getClientSecret();
    }


    public WebClient.RequestHeadersSpec<?> createPostRequest(String token, String uriTemplate, Object body, Object... uriVariables) {
        String uri = buildUri(uriTemplate, uriVariables);
        return webClient.post()
                .uri(uri, realm)
                .header(AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    private String buildUri(String uriTemplate, Object[] uriVariables) {
        return UriComponentsBuilder.fromUriString(uriTemplate)
                .build(uriVariables)
                .toString();
    }

    public WebClient.RequestHeadersSpec<?> createGetRequest(String token, String uriTemplate, Object... uriVariables) {
        String uri = buildUri(uriTemplate, uriVariables);
        return webClient.get()
                .uri(uri, realm)
                .header(AUTHORIZATION, BEARER + token);
    }

    public WebClient.RequestHeadersSpec<?> createPutRequest(String token, String uriTemplate, Object body, Object... uriVariables) {
        String uri = buildUri(uriTemplate, uriVariables);
        return webClient.put()
                .uri(uri, realm)
                .header(AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }


    public Mono<String> getAdminAccessToken() {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new AuthenticationException("Token inválido ou expirado."));
                    }
                    return response.createException().flatMap(Mono::error);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Erro interno ao obter o token.")))
                .bodyToMono(String.class);
    }
}
