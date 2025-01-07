package br.dev.leandro.spring.cloud.user.service;

import br.dev.leandro.spring.cloud.user.dto.UserDto;
import br.dev.leandro.spring.cloud.user.dto.UserUpdateDto;
import br.dev.leandro.spring.cloud.user.exception.ResourceNotFoundException;
import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import br.dev.leandro.spring.cloud.user.utils.WebClientUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@RefreshScope
@Service
@Getter
public class UserService {

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
                                return handleErrorStatus(response);
                            });
                })
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga exceções conhecidas sem encapsulá-las novamente
                    }
                    log.error("Erro inesperado ao atualizar usuário", e);
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

//    public Mono<Void> updateUser(String id, UserUpdateDto userUpdateDto) {
//        return webClientUtils.getAdminAccessToken()
//                .flatMap(token -> {
//                    Map<String, Object> payload = buildUpdateUserPayload(userUpdateDto);
//
//                    return webClientUtils.createPutRequest(token, "/admin/realms/{realm}/users/{id}", payload, Map.of("id", id))
//                            .retrieve()
//                            .onStatus(HttpStatus.NOT_FOUND::equals, response ->
//                                    Mono.error(new ResourceNotFoundException("Usuário não encontrado.")))
//                            .onStatus(HttpStatus.FORBIDDEN::equals, response ->
//                                    Mono.error(new AuthenticationException("Permissão negada para atualizar o usuário.")))
//                            .onStatus(HttpStatus.UNAUTHORIZED::equals, response ->
//                                    Mono.error(new RuntimeException("Token inválido ou expirado.")))
//                            .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, response ->
//                                    Mono.error(new RuntimeException("Erro interno no Keycloak.")))
//                            .bodyToMono(Void.class)
//                            .then(handlePasswordUpdate(userUpdateDto.password(), id, token))
//                            .onErrorResume(e -> {
//                                if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
//                                    return Mono.error(e);
//                                }
//                                log.error("Erro inesperado ao atualizar usuário: {}", e.getMessage(), e);
//                                return Mono.error(new RuntimeException("Exception: " + e.getMessage(), e));
//                            });
//                });
//    }

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
                                return handleErrorStatus(response);
                            });
                })
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga exceções conhecidas sem encapsulá-las novamente
                    }
                    log.error("Erro inesperado ao atualizar usuário", e);
                    return Mono.error(e); // Propaga a exceção original sem adicionar prefixos adicionais
                });

    }



    private <T> Mono<T> handleErrorStatus(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.createException().flatMap(ex -> {
            if (status == HttpStatus.NOT_FOUND) {
                return Mono.error(new ResourceNotFoundException("Usuário não encontrado."));
            }
            if (status == HttpStatus.FORBIDDEN) {
                return Mono.error(new AuthenticationException("Acesso negado."));
            }
            if (status == HttpStatus.UNAUTHORIZED) {
                return Mono.error(new RuntimeException("Token inválido ou expirado."));
            }
            if (status.is5xxServerError()) {
                return Mono.error(new RuntimeException("Erro interno no Keycloak."));
            }
            return Mono.error(ex);  // Tratamento genérico para outros erros
        });
    }




    private Mono<Void> handlePasswordUpdate(Optional<String> passwordOpt, String id, String token) {
        return passwordOpt.map(password -> setUserPassword(id, password, token))
                .orElseGet(() -> {
                    log.warn("Password não informado para o Usuário com ID: {}", id);
                    return Mono.empty();
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
//                                                if (response.statusCode().is5xxServerError()) {
//                                                    log.error("Erro ao atribuir role ao usuário: {}", response.statusCode());
//                                                    return Mono.error(new RuntimeException("Erro interno ao atribuir role"));
//                                                }
                                                // Em caso de erro, usa o tratamento centralizado
                                                return handleErrorStatus(response);
                                            });
                                })
                )
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException || e instanceof AuthenticationException) {
                        return Mono.error(e); // Propaga exceções conhecidas sem encapsulá-las novamente
                    }
                    log.error("Erro inesperado ao atualizar usuário", e);
                    return Mono.error(e); // Propaga a exceção original sem adicionar prefixos adicionais
                });

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


}
