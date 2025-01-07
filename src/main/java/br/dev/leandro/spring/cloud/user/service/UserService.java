package br.dev.leandro.spring.cloud.user.service;

import br.dev.leandro.spring.cloud.user.dto.UserDto;
import br.dev.leandro.spring.cloud.user.dto.UserUpdateDto;
import br.dev.leandro.spring.cloud.user.exception.ResourceNotFoundException;
import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import br.dev.leandro.spring.cloud.user.utils.WebClientUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RefreshScope
@Service
@Getter
public class UserService {

    private final WebClientUtils webClientUtils;
    private final String realm;

    public UserService(WebClientUtils webClientUtils, KeycloakProperties keycloakProperties) {
        this.webClientUtils = webClientUtils;
        this.realm = keycloakProperties.getRealm();
    }

    public Mono<Void> createUser(UserDto userDto) {
        return webClientUtils.getAdminAccessToken()
                .flatMap(token -> {
                    log.info("Token JWT obtido com sucesso: {}", token);
                    Map<String, Object> user = Map.of(
                            "username", userDto.username(),
                            "email", userDto.email(),
                            "firstName", userDto.firstName(),
                            "lastName", userDto.lastName(),
                            "enabled", true,
                            "credentials", List.of(Map.of(
                                    "type", "password",
                                    "value", userDto.password(),
                                    "temporary", false
                            )));
                    return webClientUtils.createPostRequest(token, "/admin/realms/{realm}/users", user, realm)
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


    public Mono<Void> updateUser(String id, UserUpdateDto userUpdateDto) {
        return webClientUtils.getAdminAccessToken()
                .flatMap(token -> {
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("username", userUpdateDto.username());
                            userUpdateDto.email().ifPresent(email -> payload.put("email", email));
                            userUpdateDto.firstName().ifPresent(firstName -> payload.put("firstName", firstName));
                            userUpdateDto.lastName().ifPresent(lastName -> payload.put("lastName", lastName));
                            userUpdateDto.password().ifPresent(password -> payload.put("password", password));

                            return webClientUtils.createPutRequest(token, "/admin/realms/{realm}/users/{id}",payload, realm, id)
                                    .retrieve()
                                    .onStatus(HttpStatus.NOT_FOUND::equals, response -> {
                                        log.info("Usuário não encontrado.");
                                        return Mono.error(new ResourceNotFoundException("Usuário não encontrado."));
                                    })
                                    .onStatus(HttpStatus.FORBIDDEN::equals, response -> {
                                        log.info("Permissão negada para atualizar o usuário.");
                                        return Mono.error(new AuthenticationException("Permissão negada para atualizar o usuário."));
                                    })
                                    .onStatus(HttpStatus.UNAUTHORIZED::equals, response ->
                                            Mono.error(new RuntimeException("Token inválido ou expirado.")))
                                    .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, response ->
                                            Mono.error(new RuntimeException("Erro interno no Keycloak.")))
                                    .bodyToMono(Void.class)
                                    .then(Mono.defer(() -> {
                                        // Utiliza ifPresentOrElse para verificar a presença do password
                                        AtomicReference<Mono<Void>> result = new AtomicReference<>(Mono.empty());
                                        userUpdateDto.password().ifPresentOrElse(
                                                password -> result.set(setUserPassword(id, password, token)),
                                                () -> log.warn("Password não informado para o Usuário com ID: {}", id));
                                        return result.get();
                                    }));
                        }
                )
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga a exceção original
                    }
                    log.error("Erro inesperado ao atualizar usuário: {}", e.getMessage(), e);
                    return Mono.error(new RuntimeException("Exception: " + e.getMessage(), e));
                });

    }

    public Mono<Void> assignRoleToUser(String userId, String roleName) {
        log.info("Iniciando assignRoleToUser para User ID: {}, Role: {}", userId, roleName);

        return webClientUtils.getAdminAccessToken()
                .flatMap(token ->
                        // Buscar roles
                        webClientUtils.createGetRequest(token, "/admin/realms/{realm}/roles", realm)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, response -> {
                                    log.error("Erro ao buscar roles: {}", response.statusCode());
                                    return response.createException().flatMap(Mono::error);
                                })
                                .bodyToFlux(Map.class)
                                .filter(role -> role.get("name").equals(roleName))
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Role não encontrada: " + roleName)))
                                .single()
                                .flatMap(role -> {
                                    String roleId = (String) role.get("id");
                                    log.info("Role ID encontrado: {}", roleId);

                                    // Atribuir role ao usuário
                                    return webClientUtils.createPostRequest(token,
                                                    "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                                                    Collections.singletonList(Map.of("id", roleId, "name", roleName)), realm, userId)
                                            .retrieve()
                                            .onStatus(HttpStatusCode::is5xxServerError, response -> {
                                                log.error("Erro ao atribuir role ao usuário: {}", response.statusCode());
                                                return Mono.error(new RuntimeException("Erro interno ao atribuir role"));
                                            })
                                            .bodyToMono(Void.class);
                                })
                );
    }


    private Mono<Void> setUserPassword(String id, String password, String token) {
        return webClientUtils.createPutRequest(token,
                        "/admin/realms/{realm}/users/{id}/reset-password",
                        Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        ), realm, id)
                .retrieve()
                .bodyToMono(Void.class);
    }



}
