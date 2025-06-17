package br.dev.leandro.spring.cloud.user.utils;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

public class SecurityUtils {

    public static Mono<Jwt> getJwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    var auth = ctx.getAuthentication();
                    if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
                        throw new IllegalStateException("Jwt não encontrado no contexto reativo.");
                    }
                    return (Jwt) auth.getPrincipal();
                });
    }

    public static Mono<String> getUser() {
        return getJwt()
                .map(jwt -> jwt.getClaim("preferred_username").toString());
    }

    public static Mono<String> getBearerToken() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> {
                    var auth = context.getAuthentication();
                    if (auth == null || !(auth.getCredentials() instanceof String)) {
                        throw new IllegalStateException("Token JWT não disponível no contexto.");
                    }
                    return (String) auth.getCredentials(); // normalmente o token está aqui
                });
    }
}

