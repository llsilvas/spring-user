package br.dev.leandro.spring.cloud.user.service.integration;

import br.dev.leandro.spring.cloud.user.config.WebClientTestConfig;
import br.dev.leandro.spring.cloud.user.dto.UserDto;
import br.dev.leandro.spring.cloud.user.dto.UserUpdateDto;
import br.dev.leandro.spring.cloud.user.exception.AssignRoleException;
import br.dev.leandro.spring.cloud.user.exception.ResourceNotFoundException;
import br.dev.leandro.spring.cloud.user.service.UserService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.enabled=false",
        classes = {WebClientTestConfig.class})
@WireMockTest
class UserServiceIT {

    @Autowired
    private UserService userService;
    private UserDto userDto;
    private UserUpdateDto userUpdateDto;

    @Autowired
    private Environment environment;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUpWireMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WireMock.configureFor("localhost", wireMockServer.port());
        System.out.println("WireMock iniciado na URL: " + wireMockServer.baseUrl());

        // Listar todos os stubs configurados
        System.out.println("Stubs configurados:");
        wireMockServer.getStubMappings().forEach(mapping -> {
            System.out.println(mapping.toString());
        });
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String wireMockUrl = wireMockServer.baseUrl(); // Sem necessidade de incluir `localhost`
        registry.add("spring.keycloak.admin.auth-server-url", () -> wireMockUrl);
    }

    @AfterAll
    static void tearDownWireMockServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        System.out.println("WireMock rodando na porta: " + wireMockServer.port());
        System.out.println("Stub configurado para: " + wireMockServer.baseUrl() + "/realms/mocked-realm/protocol/openid-connect/token");

        // Mock para obtenção de roles
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/mocked-realm/roles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\": \"role123\", \"name\": \"role\"}]")));

        //Mock userDto
        userDto = getUserDto();
        userUpdateDto = getUserUpdateDto();

        WireMock.configureFor("localhost", wireMockServer.port());

        wireMockServer.resetAll();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.findAllUnmatchedRequests().forEach(request -> {
            System.out.println("Requisição não correspondida:");
            System.out.println("Método: " + request.getMethod());
            System.out.println("URL: " + request.getUrl());
            System.out.println("Cabeçalhos: " + request.getHeaders());
            System.out.println("Corpo: " + request.getBodyAsString());
        });
    }

    @Test
    void activeProfileShouldBeTest() {
        String[] activeProfiles = environment.getActiveProfiles();
        assertTrue(Arrays.asList(activeProfiles).contains("test"));
    }


    @Nested
    class CreateUserTests {

        @Test
        void createUser_shouldCreateUserSuccessfully() {
            // Mock do endpoint para obter o token de acesso.
            wireMockServer.stubFor(post(urlEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}")));

            wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/mocked-realm/roles"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\": \"role123\", \"name\": \"role\"}]")));

            wireMockServer.stubFor(post(urlEqualTo("/admin/realms/mocked-realm/users"))
                    .withHeader("Authorization", equalTo("Bearer mocked-token"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(equalToJson("{"
                            + "\"username\":\"test_user\","
                            + "\"email\":\"test@example.com\","
                            + "\"firstName\":\"Test\","
                            + "\"lastName\":\"User\","
                            + "\"enabled\":true,"
                            + "\"credentials\":[{\"type\":\"password\",\"value\":\"password123\",\"temporary\":false}]"
                            + "}"))
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.CREATED.value())
                            .withHeader(HttpHeaders.LOCATION, "/admin/realms/mocked-realm/users/123")));


            // Mock do endpoint de atribuição de roles ao usuário.
            wireMockServer.stubFor(post(urlEqualTo("/admin/realms/mocked-realm/users/123/role-mappings/realm"))
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.NO_CONTENT.value())));


            Mono<Void> result = userService.createUser(userDto);

            // Verificar o comportamento usando StepVerifier.
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            // Verificar chamadas nos mocks.
            verify(postRequestedFor(urlEqualTo("/admin/realms/mocked-realm/users"))
                    .withHeader("Authorization", equalTo("Bearer mocked-token")));
            verify(postRequestedFor(urlEqualTo("/admin/realms/mocked-realm/users/123/role-mappings/realm")));
        }


        @Test
        void testCreateUser_UserCreationFails() {

            wireMockServer.stubFor(post(urlEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}")));

            // Configuração do stub para criar usuário com erro
            wireMockServer.stubFor(post(urlEqualTo("/admin/realms/mocked-realm/users"))
                    .willReturn(aResponse()
                            .withStatus(500)));

            userDto = new UserDto("test_user", "test@example.com", "Test", "User", "password123", "test-role", "Teste Organização", "11 3333-3333", "1122334455-45");

            // Chamada do método
            Mono<Void> result = userService.createUser(userDto);

            // Verificação com StepVerifier
            StepVerifier.create(result)
                    .expectErrorSatisfies(throwable -> {
                        assertInstanceOf(RuntimeException.class, throwable);
                        // Ajuste para verificar a mensagem de erro atual
                        assertTrue(throwable.getMessage().contains("Erro interno no Keycloak"));
                    })
                    .verify();
        }


        @Test
        void testCreateUser_InvalidToken() {
            // Configuração do stub para obter o token com erro
            wireMockServer.stubFor(post(urlEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(401)));

            userDto = new UserDto("test_user", "test@example.com", "Test", "User", "password123", "test-role", "Teste Organização", "11 3333-3333", "1122334455-45");

            // Chamada do método
            Mono<Void> result = userService.createUser(userDto);

            // Verificação com StepVerifier
            StepVerifier.create(result)
                    .expectErrorSatisfies(throwable -> {
                        assertInstanceOf(BadCredentialsException.class, throwable);
                        assertEquals("Token inválido ou expirado.", throwable.getMessage());
                    })
                    .verify();
        }

        @Test
        void testAssignRoleToUser_RoleNotFound() {

            wireMockServer.stubFor(post(urlEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}")));
            // Configura o WireMock para não retornar roles
            wireMockServer.stubFor(get(urlEqualTo("/admin/realms/mocked-realm/roles"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[]")));

            Mono<Void> result = userService.assignRoleToUser("123", "nonexistent-role");

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(ResourceNotFoundException.class, error);
                        assertEquals("Role não encontrada: nonexistent-role", error.getMessage());
                    })
                    .verify();
        }

        @Test
        void testCreateUser_RoleAssignmentFails() {
            // Configuração do stub para obter o token
            wireMockServer.stubFor(post(urlEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            // Configuração do stub para criar usuário
            wireMockServer.stubFor(post(urlEqualTo("/admin/realms/mocked-realm/users"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Location", "/admin/realms/mocked-realm/users/123")));

            // Configuração do stub para retornar a role existente
            wireMockServer.stubFor(get(urlEqualTo("/admin/realms/mocked-realm/roles"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\": \"role123\", \"name\": \"test-role\"}]")));

            // Configuração do stub para atribuir role com erro
            wireMockServer.stubFor(post(urlEqualTo("/admin/realms/mocked-realm/users/123/role-mappings/realm"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Erro interno ao atribuir role")));

            userDto = new UserDto("test_user", "test@example.com", "Test", "User", "password123", "test-role", "Teste Organização", "11 3333-3333", "1122334455-45");

            // Chamada do método
            Mono<Void> result = userService.createUser(userDto);

            // Verificação com StepVerifier
            StepVerifier.create(result)
                    .expectErrorSatisfies(throwable -> {
                        assertInstanceOf(AssignRoleException.class, throwable);
                        assertTrue(throwable.getMessage().contains("Não foi possível atribuir o papel"));
                    })
                    .verify();

        }
    }

    @Nested
    class UpdateUsersTests{

        @Test
        void updateUser_ShouldUpdateSuccessfully() {

            // Configuração do stub para obter o token
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .withHeader("Authorization", equalTo("Bearer mocked-token"))
                    .withRequestBody(matchingJsonPath("$.username", equalTo("test_user")))
                    .withRequestBody(matchingJsonPath("$.email", equalTo("test@example.com")))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("Usuário atualizado com sucesso!")));


            wireMockServer.stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456/reset-password"))
                    .willReturn(aResponse()
                            .withStatus(204))); // Use 204 porque reset de senha geralmente não retorna corpo

            assertDoesNotThrow(() -> {
                userService.updateUser("123456", userUpdateDto).block();
            });

            verify(putRequestedFor(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .withHeader("Authorization", equalTo("Bearer mocked-token"))
                    .withRequestBody(matchingJsonPath("$.username", equalTo("test_user")))
                    .withRequestBody(matchingJsonPath("$.email", equalTo("test@example.com"))));

        }

        @Test
        void updateUser_ShouldHandleUserNotFound() {
            // Configuração do stub para obter o token
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));
            // Configura o WireMock para retornar 404
            wireMockServer.stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse().withStatus(404)));

            // Act & Assert: Verifica se a exceção encapsulada contém a exceção esperada
            Exception exception = assertThrows(Exception.class, () -> {
                userService.updateUser("123456", userUpdateDto).block();
            });

            // Verifica se a causa da exceção é a esperada
            assertInstanceOf(ResourceNotFoundException.class, exception);
            assertEquals("Usuário não encontrado.", exception.getMessage());
        }

        @Test
        void updateUser_ShouldHandleAccessDenied() {
            // Configuração do stub para obter o token
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));
            // Arrange: Configuração do WireMock para retornar 403 Forbidden
            wireMockServer.stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse()
                            .withStatus(403)
                            .withBody("Acesso negado")));

            // Act & Assert: Verifica se a exceção encapsulada contém a exceção esperada
            Exception exception = assertThrows(Exception.class, () -> {
                userService.updateUser("123456", userUpdateDto).block();
            });

            // Verifica se a causa da exceção é a esperada
            Throwable cause = exception.getCause();
            assertNotNull(cause);
            assertEquals("Acesso negado.", cause.getMessage());
        }

        @Test
        void updateUser_ShouldHandleUnexpectedError() {
            // Simula um erro inesperado no WireMock
            wireMockServer.stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                userService.updateUser("123456", userUpdateDto).block();
            });

            assertFalse(exception.getMessage().isEmpty());
        }

        @Test
        void updateUser_ShouldHandleInvalidPassword() {
            wireMockServer.stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("Usuário atualizado com sucesso!")));

            UserUpdateDto updateDto = new UserUpdateDto("test_user", Optional.of("test@example.com"), Optional.of("Test"), Optional.of("User"), Optional.empty());

            Exception exception = assertThrows(Exception.class, () -> {
                userService.updateUser("123456", updateDto).block();
            });

            assertFalse(exception.getMessage().isEmpty());
        }
    }

    @Nested
    class DeleteUserTests{

        @Test
        void testeDeleteUser_Sucess() {
            // Configuração do stub para obter o token
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(delete(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse()
                            .withStatus(204)));

            Mono<Void> result = userService.deleteUser("123456");

            StepVerifier.create(result)
                    .expectComplete()
                    .verify();

            verify(deleteRequestedFor(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .withHeader("Authorization", equalTo("Bearer mocked-token")));
        }

        @Test
        void testDeleteUser_UserNotFound() {
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(delete(urlPathEqualTo("/admin/realms/mocked-realm/users/nonexistent-id"))
                    .willReturn(aResponse()
                            .withStatus(404)));

            Mono<Void> result = userService.deleteUser("nonexistent-id");

            StepVerifier.create(result)
                    .expectErrorMatches(ex -> ex instanceof ResourceNotFoundException
                            && ex.getMessage().contains("Usuário não encontrado")) // Adaptar conforme mensagem
                    .verify();
        }
    }

    @Nested
    class GetUsersTest{

        @Test
        void testGetUsers_Sucess() {
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse()
                            .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\": \"" + "123456" + "\", \"username\": \"testuser\"}")));

            StepVerifier.create(userService.findUserById("123456"))
                    .expectNextMatches(user -> user.username().equals("testuser"))
                    .verifyComplete();
        }

        @Test
        void testGetUsers_UserNotFound() {

            wireMockServer.stubFor(get(urlPathEqualTo("/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse()
                    .withStatus(404)));

            StepVerifier.create(userService.findUserById("123456"))
                    .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException.NotFound)
                    .verify();
        }

        @Test
        void testGetUser_WhenServerError() {

            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(get(urlEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse().withStatus(500)));


            StepVerifier.create(userService.findUserById("123456"))
                    .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException.InternalServerError)
                    .verify();
        }

        @Test
        void findUserById_ShouldReturnError_WhenUnauthorized() {
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(get(urlEqualTo("/admin/realms/mocked-realm/users/123456"))
                    .willReturn(aResponse().withStatus(401)));

            StepVerifier.create(userService.findUserById("123456"))
                    .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException.Unauthorized)
                    .verify();
        }

        @Test
        void testFindAllUsers_WithSearchParam_ShouldReturnFilteredUsers() {
            wireMockServer.stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mocked-token\"}")));

            wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/mocked-realm/users"))
                    .withQueryParam("search", equalTo("leandro"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                            [
                                {
                                    "id": "1",
                                    "username": "leandro",
                                    "email": "leandro@email.com"
                                }
                            ]
                        """)));

            wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/mocked-realm/users/count"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("1")));

            StepVerifier.create(userService.findAllUsers("leandro", 0, 10))
                    .expectNextMatches(response -> {
                        List<?> users = (List<?>) response.get("users");
                        return users.size() == 1 && users.getFirst().toString().contains("leandro");
                    })
                    .verifyComplete();
        }

    }

    @NotNull
    private static UserDto getUserDto() {
        return new UserDto("test_user", "test@example.com", "Test", "User", "password123", "role", "Teste Organização", "11 3333-3333", "1122334455-45");
    }

    private static UserUpdateDto getUserUpdateDto() {
        return new UserUpdateDto("test_user", Optional.of("test@example.com"), Optional.of("Test"), Optional.of("User"), Optional.of("password123"));
    }
}

