package br.dev.leandro.spring.cloud.user.service;

import br.dev.leandro.spring.cloud.user.dto.UserDto;
import br.dev.leandro.spring.cloud.user.dto.UserUpdateDto;
import br.dev.leandro.spring.cloud.user.exception.ResourceNotFoundException;
import br.dev.leandro.spring.cloud.user.exception.handler.WebClientErrorHandler;
import br.dev.leandro.spring.cloud.user.utils.WebClientUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RefreshScope
@Service
@Getter
public class UserService {

    public static final String ERRO_INESPERADO_AO_ATUALIZAR_USUARIO = "Erro inesperado ao atualizar usuário";
    private final WebClientUtils webClientUtils;

    public UserService(WebClientUtils webClientUtils) {
        this.webClientUtils = webClientUtils;
    }

    public Mono<Void> createUser(UserDto userDto) {
        return webClientUtils.getAdminAccessToken()
                .flatMap(token -> {
                    log.info("Token JWT obtido com sucesso: {}", token);
                    Map<String, Object> user = buildUserPayload(userDto);
                    return webClientUtils.createPostRequest(token, "/admin/realms/{realm}/users", user, null)
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
                                return WebClientErrorHandler.handleErrorStatus(response);
                            });
                })
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga exceções conhecidas sem encapsulá-las novamente
                    }
                    log.error(ERRO_INESPERADO_AO_ATUALIZAR_USUARIO, e);
                    return Mono.error(e); // Propaga a exceção original sem adicionar prefixos adicionais
                });

    }

    public Mono<Void> updateUser(String id, UserUpdateDto userUpdateDto) {
        return webClientUtils.getAdminAccessToken()
                .flatMap(token -> {
                    Map<String, Object> payload = buildUpdateUserPayload(userUpdateDto);

                    return webClientUtils.createPutRequest(token, "/admin/realms/{realm}/users/{id}", payload, Map.of("id", id))
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(Void.class)
                                            .then(handlePasswordUpdate(userUpdateDto.password(), id, token));
                                }
                                // Em caso de erro, usa o tratamento centralizado
                                return WebClientErrorHandler.handleErrorStatus(response);
                            });
                })
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga exceções conhecidas sem encapsulá-las novamente
                    }
                    log.error(ERRO_INESPERADO_AO_ATUALIZAR_USUARIO, e);
                    return Mono.error(e); // Propaga a exceção original sem adicionar prefixos adicionais
                });

    }

    public Mono<Void> deleteUser(String id) {
        return webClientUtils.getAdminAccessToken()
                .flatMap(token ->
                        webClientUtils.createDeleteRequest(token, "/admin/realms/{realm}/users/{id}", Map.of("id", id))
                                .exchangeToMono(response -> {
                                    if (response.statusCode().is2xxSuccessful()) {
                                        return Mono.empty();
                                    }
                                    return WebClientErrorHandler.handleErrorStatus(response).then();
                                })
                ).onErrorResume(e -> {
                    log.error("Erro ao excluir o user: {}", id, e);
                    return Mono.error(e);
                });
    }

    public Mono<Void> assignRoleToUser(String userId, String roleName) {
        log.info("Iniciando assignRoleToUser para User ID: {}, Role: {}", userId, roleName);
        Map<String, Object> uriVariables = Map.of("userId", userId);
        return webClientUtils.getAdminAccessToken()
                .flatMap(token ->
                        // Buscar roles
                        webClientUtils.createGetRequest(token, "/admin/realms/{realm}/roles")
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
                                                    Collections.singletonList(Map.of("id", roleId, "name", roleName)),
                                                    uriVariables)
                                            .exchangeToMono(response -> {
                                                if (response.statusCode().is2xxSuccessful()) {
                                                    return response.bodyToMono(Void.class);
                                                }
                                                return WebClientErrorHandler.handleErrorStatus(response);
                                            });
                                })
                )
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga exceções conhecidas sem encapsulá-las novamente
                    }
                    log.error(ERRO_INESPERADO_AO_ATUALIZAR_USUARIO, e);
                    return Mono.error(e); // Propaga a exceção original sem adicionar prefixos adicionais
                });

    }

    @NotNull
    private static Map<String, Object> buildUserPayload(UserDto userDto) {
        return Map.of(
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
    }

    private Mono<Void> handlePasswordUpdate(Optional<String> passwordOpt, String id, String token) {
        return passwordOpt.map(password -> setUserPassword(id, password, token))
                .orElseGet(() -> {
                    log.warn("Password não informado para o Usuário com ID: {}", id);
                    return Mono.error(new IllegalArgumentException(
                            "Senha não informada para o usuário com ID: " + id));

                }).onErrorResume(e -> {
                    log.error(ERRO_INESPERADO_AO_ATUALIZAR_USUARIO, e);
                    return Mono.error(e);
                });
    }

    @NotNull
    private static Map<String, Object> buildUpdateUserPayload(UserUpdateDto userUpdateDto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", userUpdateDto.username());
        userUpdateDto.email().ifPresent(email -> payload.put("email", email));
        userUpdateDto.firstName().ifPresent(firstName -> payload.put("firstName", firstName));
        userUpdateDto.lastName().ifPresent(lastName -> payload.put("lastName", lastName));
        userUpdateDto.password().ifPresent(password -> payload.put("password", password));
        return payload;
    }

    private Mono<Void> setUserPassword(String id, String password, String token) {
        return webClientUtils.createPutRequest(token,
                        "/admin/realms/{realm}/users/{id}/reset-password",
                        Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        ), Map.of("id", id))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<UserDto> findUserById(String id) {
        return webClientUtils.getAdminAccessToken()
                .flatMap(token -> webClientUtils.createGetRequest(token, "/admin/realms/{realm}/users/{id}", Map.of("id", id))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> {
                            log.error("Erro ao buscar o usuário por ID: {}", id);
                            return response.createException().flatMap(Mono::error);
                        })
                        .bodyToMono(UserDto.class)
                ).onErrorResume(e -> {
                    log.error("Erro ao buscar o usuário: {}", id, e);
                    return Mono.error(e);
                });
    }

    public Mono<Map<String, Object>> findAllUsers(String search, Integer first, Integer max) {
        Map<String, Object> queryParams = new HashMap<>();
        if (!search.isEmpty()) {
            queryParams.put("search", search); // Certifica-se de usar "search"
        }
        if (first != null) {
            queryParams.put("first", first);
        }
        if (max != null) {
            queryParams.put("max", max);
        }

        log.info("Enviando requisição ao Keycloak com parâmetros: {}", queryParams);
        long startTime = System.currentTimeMillis();

        return webClientUtils.getAdminAccessToken()
                .flatMap(token -> {
                    String finalUrl = "/admin/realms/{realm}/users?search=" + URLEncoder.encode(search, StandardCharsets.UTF_8);

                    log.info("Chamando Keycloak: {} com parâmetros: {}", finalUrl, queryParams);

                    Mono<List<UserDto>> usersMono = webClientUtils.createGetRequest(token, finalUrl, queryParams)
                            .retrieve()
                            .bodyToFlux(UserDto.class)
                            .collectList()
                            .doOnNext(users -> log.info("Usuários retornados do Keycloak: {}", users));

                    Mono<Integer> countMono = webClientUtils.createGetRequest(token, "/admin/realms/{realm}/users/count")
                            .retrieve()
                            .bodyToMono(String.class) // Obtém a resposta como String
                            .map(body -> {
                                try {
                                    return Integer.parseInt(body.trim()); // Converte para Integer manualmente
                                } catch (NumberFormatException e) {
                                    log.error("Erro ao converter contagem de usuários: {}", body);
                                    return 0; // Retorna 0 caso a conversão falhe
                                }
                            })
                            .doOnNext(total -> log.info("Total de usuários no Keycloak: {}", total));


                    return Mono.zip(usersMono, countMono)
                            .map(tuple -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("total", tuple.getT2());
                                result.put("users", tuple.getT1());
                                result.put("page", first / max + 1);
                                result.put("pageSize", max);
                                long duration = System.currentTimeMillis() - startTime;
                                log.info("Tempo de resposta do Keycloak: {}ms", duration);
                                log.info("Resposta final enviada ao cliente: {}", result);
                                return result;
                            });
                }).onErrorResume(e -> {
                    log.error("Erro ao buscar usuários no Keycloak: {}", e.getMessage());
                    return Mono.error(e);
                });
    }
}