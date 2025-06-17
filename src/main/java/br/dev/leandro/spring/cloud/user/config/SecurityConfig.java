package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.converter.CustomJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for the application.
 * Uses CustomJwtAuthenticationConverter to extract roles from JWT tokens.
 */
@Slf4j
@Configuration
@Profile("!test")
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
         return http

                 
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        // Endpoints públicos
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/user/swagger-ui/**").permitAll()
                        .pathMatchers("/user/swagger-ui.html").permitAll()
                        .pathMatchers("/user-docs/**").permitAll()
                        .pathMatchers("/users/public/**").permitAll()
                        // Endpoints administrativos (somente ADMIN)
                        .pathMatchers("/users/admin/**").hasRole("ADMIN")
                        // Endpoints organizadores (somente ORGANIZADOR)
                        .pathMatchers("/users/organizador/**").hasRole("ORGANIZADOR")
                        // Endpoints participantes (somente PARTICIPANTE)
                        .pathMatchers("/users/participante/**").hasRole("PARTICIPANTE")
                        // Endpoints acessíveis ao usuário autenticado
                        .pathMatchers("/users/me/**").authenticated()
                        .anyExchange().authenticated() // Qualquer outra requisição deve estar autenticada
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Configuração JWT
                        )
                ).build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(customJwtAuthenticationConverter);
        log.debug("Configured JWT authentication converter with custom authorities converter");
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
