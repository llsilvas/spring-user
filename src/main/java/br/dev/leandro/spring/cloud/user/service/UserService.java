package br.dev.leandro.spring.cloud.user.service;

import br.dev.leandro.spring.cloud.user.KeycloakProperties;
import br.dev.leandro.spring.cloud.user.model.UserDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RefreshScope
@Service
@Getter
public class UserService {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;

    @Value("${app.mensagem.usuario}")
    private String msgUsuario;

    @Value("${spring.keycloak.admin.client-id}")
    private String clientId;

    @Value("${spring.keycloak.admin.client-secret}")
    private String clientSecret;

    @Value("${spring.keycloak.admin.auth-server-url}")
    private String authServerUrl;

    @Value("${spring.keycloak.admin.realm}")
    private String realm;

    public UserService(WebClient webClient, KeycloakProperties keycloakProperties) {
        this.webClient = webClient;
        this.keycloakProperties = keycloakProperties;
    }

    public Mono<Void> createUser(UserDto userDto) {
        return getAdminAccessToken()
                .flatMap(token -> {
                    log.info("Token JWT obtido com sucesso: {}", token);
                    return createRequest(token, "/admin/realms/{realm}/users",
                            Map.of(
                                    "username", userDto.username(),
                                    "email", userDto.email(),
                                    "firstName", userDto.firstName(),
                                    "lastName", userDto.lastName(),
                                    "enabled", true,
                                    "credentials", List.of(Map.of(
                                            "type", "password",
                                            "value", userDto.password(),
                                            "temporary", false
                                    ))))
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    String location = response.headers().asHttpHeaders().getFirst("Location");
                                    log.info("Header Location: {}", location);
                                    if (location != null) {
                                        String userId = location.substring(location.lastIndexOf("/") + 1);
                                        log.info("User ID extraído: {}", userId);
                                        return assignRoleToUser(userId, userDto.role());
                                    }
                                    return Mono.error(new RuntimeException("Header Location não encontrado"));
                                }
                                return response.createException().flatMap(Mono::error);
                            });
                });
    }

    private WebClient.RequestHeadersSpec<?> createRequest(String token, String uri, Object body) {
        return webClient.post()
                .uri(uri, keycloakProperties.getRealm())
                .header(AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    public Mono<Void> assignRoleToUser(String userId, String roleName) {
        log.info("Iniciando assignRoleToUser para User ID: {}, Role: {}", userId, roleName);
        return getAdminAccessToken()
                .flatMap(token -> webClient.get()
                        .uri("/admin/realms/{realm}/roles", keycloakProperties.getRealm())
                        .header(AUTHORIZATION, BEARER + token)
                        .retrieve()
                        .bodyToFlux(Map.class)
                        .filter(role -> role.get("name").equals(roleName))
                        .single()
                        .flatMap(role -> {
                            String roleId = (String) role.get("id");
                            log.info("Role ID encontrado: {}", roleId);
                            return webClient.post()
                                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                                            keycloakProperties.getRealm(), userId)
                                    .header(AUTHORIZATION, BEARER + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Collections.singletonList(Map.of("id", roleId, "name", roleName)))
                                    .retrieve()
                                    .bodyToMono(Void.class);
                        }));
    }


    public Mono<Void> updateUser(Long id, UserDto userDto) {
        return getAdminAccessToken()
                .flatMap(token -> webClient.put()
                        .uri("/admin/realms/{realm}/users/{id}", keycloakProperties.getRealm(), id)
                        .header(AUTHORIZATION, BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "username", userDto.username(),
                                "email", userDto.email(),
                                "firstName", userDto.firstName(),
                                "lastName", userDto.lastName(),
                                "enabled", true
                        ))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Erro ao atualizar usuário: " + body))))
                        .bodyToMono(Void.class)
                        .then(setUserPassword(id, userDto.password(), token))
                )
                .onErrorResume(e -> {
                    // Lógica de fallback ou logging
                    return Mono.error(new RuntimeException("Falha ao atualizar usuário", e));
                });
    }

    private Mono<Void> setUserPassword(Long id, String password, String token) {
        return webClient.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", keycloakProperties.getRealm(), id)
                .header(AUTHORIZATION, BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
                .retrieve()
                .bodyToMono(Void.class);
    }

    private Mono<String> getAdminAccessToken() {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", keycloakProperties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", keycloakProperties.getClientId())
                        .with("client_secret", keycloakProperties.getClientSecret()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"));
    }
}
