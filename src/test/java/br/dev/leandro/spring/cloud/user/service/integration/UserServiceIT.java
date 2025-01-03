package br.dev.leandro.spring.cloud.user.service.integration;

import br.dev.leandro.spring.cloud.user.config.WebClientTestConfig;
import br.dev.leandro.spring.cloud.user.exception.ResourceNotFoundException;
import br.dev.leandro.spring.cloud.user.dto.UserDto;
import br.dev.leandro.spring.cloud.user.service.UserService;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.tomcat.websocket.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test", classes = WebClientTestConfig.class)
@WireMockTest(httpPort = 8081) // Configura WireMock na porta 8081
class UserServiceIT {

    @Autowired
    private UserService userService;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        // Configuração de stubs para WireMock
        //Mock para obtenção do Token
        stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"mocked-token\"}")));

        //Mock para obtenção do realm
        stubFor(post(urlPathEqualTo("/admin/realms/mocked-realm/users/123/role-mappings/realm"))
                .willReturn(aResponse()
                        .withStatus(200)
                        ));

        // Mock para obtenção de roles
        stubFor(get(urlPathEqualTo("/admin/realms/mocked-realm/roles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\": \"role123\", \"name\": \"role\"}]")));

        //Mock userDto
        userDto = getUserDto();

    }

    @AfterEach
    void tearDown() {
        // Reseta as configurações do WireMock
        WireMock.reset();
    }


    @Test
    void testCreateUser_Success() {
        // Configuração de stub para criação de usuário
        stubFor(post(urlPathEqualTo("/admin/realms/mocked-realm/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Location", "/admin/realms/mocked-realm/users/123")));
        // Configuração do DTO do usuário
        UserDto userDto = new UserDto("test_user", "test@example.com", "Test", "User", "password123", "role");

        // Chamada do método que será testada
        Mono<Void> result = userService.createUser(userDto);

        // Verificação do resultado com StepVerifier
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        // Verificação de que o WireMock recebeu as requisições esperadas
        verify(postRequestedFor(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token")));
        verify(postRequestedFor(urlPathEqualTo("/admin/realms/mocked-realm/users"))
                .withHeader("Authorization", equalTo("Bearer mocked-token")));
    }

    @Test
    void updateUser_ShouldUpdateSuccessfully() {
        stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Usuário atualizado com sucesso!")));

        stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456/reset-password"))
                .willReturn(aResponse()
                        .withStatus(204))); // Use 204 porque reset de senha geralmente não retorna corpo

        assertDoesNotThrow(() -> {
            userService.updateUser("123456", userDto).block();
        });

        verify(putRequestedFor(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                .withHeader("Authorization", equalTo("Bearer mocked-token"))
                .withRequestBody(matchingJsonPath("$.username", equalTo("test_user")))
                .withRequestBody(matchingJsonPath("$.email", equalTo("test@example.com"))));
    }

    @Test
    void updateUser_ShouldHandleUserNotFound() {
        // Configura o WireMock para retornar 404
        stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                .willReturn(aResponse().withStatus(404)));

        // Act & Assert: Verifica se a exceção encapsulada contém a exceção esperada
        Exception exception = assertThrows(Exception.class, () -> {
            userService.updateUser("123456", userDto).block();
        });

        // Verifica se a causa da exceção é a esperada
        assertInstanceOf(ResourceNotFoundException.class, exception);
        assertEquals("Usuário não encontrado.", exception.getMessage());
    }

    @Test
    void updateUser_ShouldHandleAccessDenied() {
        // Arrange: Configuração do WireMock para retornar 403 Forbidden
        stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("Acesso negado")));

        // Act & Assert: Verifica se a exceção encapsulada contém a exceção esperada
        Exception exception = assertThrows(Exception.class, () -> {
            userService.updateUser("123456", userDto).block();
        });

        // Verifica se a causa da exceção é a esperada
        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertInstanceOf(AuthenticationException.class, cause);
        assertEquals("Permissão negada para atualizar o usuário.", cause.getMessage());
    }

    @Test
    void updateUser_ShouldHandleUnexpectedError() {
        // Simula um erro inesperado no WireMock
        stubFor(put(urlPathEqualTo("/admin/realms/mocked-realm/users/123456"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser("123456", userDto).block();
        });

        assertFalse(exception.getMessage().isEmpty());
    }

    @NotNull
    private static UserDto getUserDto() {
        return new UserDto("test_user", "test@example.com", "Test", "User", "password123", null);
    }


}
