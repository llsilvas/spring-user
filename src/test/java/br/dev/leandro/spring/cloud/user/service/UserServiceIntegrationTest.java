package br.dev.leandro.spring.cloud.user.service;

import br.dev.leandro.spring.cloud.user.config.WebClientTestConfig;
import br.dev.leandro.spring.cloud.user.model.UserDto;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(properties = "spring.profiles.active=test", classes = WebClientTestConfig.class)
@WireMockTest(httpPort = 8081) // Configura WireMock na porta 8081
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Configuração de stub para o endpoint de geração de token
        stubFor(post(urlPathEqualTo("/realms/mocked-realm/protocol/openid-connect/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"mocked-token\"}")));

        // Configuração de stub para criação de usuário
        stubFor(post(urlPathEqualTo("/admin/realms/mocked-realm/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Location", "/admin/realms/mocked-realm/users/123")));

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
    }

    @AfterEach
    void tearDown() {
        WireMock.reset(); // Reseta as configurações do WireMock
    }


    @Test
    void testCreateUser_Success() {
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
}
