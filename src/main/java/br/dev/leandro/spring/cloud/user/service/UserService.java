package br.dev.leandro.spring.cloud.user.service;

import br.dev.leandro.spring.cloud.user.KeycloakProperties;
import br.dev.leandro.spring.cloud.user.model.UserDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RefreshScope
@Service
@Getter
public class UserService {

    @Value("${app.mensagem.usuario}")
    private String msgUsuario;

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;

    @Value("${spring.keycloak.admin.client-id}")
    private String clientId;

    @Value("${spring.keycloak.admin.client-secret}")
    private String clientSecret;

    @Value("${spring.keycloak.admin.auth-server-url}")
    private String authServerUrl;

    @Value("${spring.keycloak.admin.realm}")
    private String realm;

    @Autowired
    public UserService(WebClient.Builder webClientBuilder, KeycloakProperties keycloakProperties) {
        this.webClient = webClientBuilder.baseUrl(keycloakProperties.getAuthServerUrl()).build();
        this.keycloakProperties = keycloakProperties;
    }

    private Mono<String> getAdminAccessToken() {
        // Faz a requisição para obter o token de acesso

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


    public Mono<Void> createUser(UserDto userDto) {
        return getAdminAccessToken()
                .flatMap(token -> {
                    log.info("Token JWT obtido com sucesso: {}", token);
                    return webClient.post()
                            .uri("/admin/realms/{realm}/users", keycloakProperties.getRealm())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "username", userDto.username(),
                                    "email", userDto.email(),
                                    "firstName", userDto.firstName(),
                                    "lastName", userDto.lastName(),
                                    "enabled", true,
                                    "credentials", List.of(Map.of(
                                            "type", "password",
                                            "value", userDto.password(),
                                            "temporary", false
                                    ))
                            ))
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


    public Mono<Void> assignRoleToUser(String userId, String roleName) {
        log.info("Iniciando assignRoleToUser para User ID: {}, Role: {}", userId, roleName);
        return getAdminAccessToken()
                .flatMap(token -> webClient.get()
                        .uri("/admin/realms/{realm}/roles", keycloakProperties.getRealm())
                        .header("Authorization", "Bearer " + token)
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
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Collections.singletonList(Map.of("id", roleId, "name", roleName)))
                                    .retrieve()
                                    .bodyToMono(Void.class);
                        }));
    }



}
