package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.converter.CustomJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 * Uses CustomJwtAuthenticationConverter to extract roles from JWT tokens.
 */
@Slf4j
@Configuration
@Profile("!test")
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Desabilitar CSRF para APIs
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/user/swagger-ui/**").permitAll()
                        .requestMatchers("/user/swagger-ui.html").permitAll()
                        .requestMatchers("/user-docs/**").permitAll()
                        .requestMatchers("/users/public/**").permitAll()
                        // Endpoints administrativos (somente ADMIN)
                        .requestMatchers("/users/admin/**").hasRole("ADMIN")
                        // Endpoints organizadores (somente ORGANIZADOR)
                        .requestMatchers("/users/organizador/**").hasRole("ORGANIZADOR")
                        // Endpoints participantes (somente PARTICIPANTE)
                        .requestMatchers("/users/participante/**").hasRole("PARTICIPANTE")
                        // Endpoints acessíveis ao usuário autenticado
                        .requestMatchers("/users/me/**").authenticated()
                        .anyRequest().authenticated() // Qualquer outra requisição deve estar autenticada
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Configuração JWT
                        )
                );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(customJwtAuthenticationConverter);
        log.debug("Configured JWT authentication converter with custom authorities converter");
        return converter;
    }
}
