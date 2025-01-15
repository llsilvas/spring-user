package br.dev.leandro.spring.cloud.user.exception.handler;

import br.dev.leandro.spring.cloud.user.exception.ResourceNotFoundException;
import org.apache.http.auth.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

public class WebClientErrorHandler {

    public static <T> Mono<T> handleErrorStatus(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.createException().flatMap(ex -> {
            String errorMessage = "Erro interno no Keycloak. Status: " + status;
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
                return Mono.error(new RuntimeException(errorMessage));
            }
            return Mono.error(ex);  // Tratamento genérico para outros erros
        });
    }

}
