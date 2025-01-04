package br.dev.leandro.spring.cloud.user.config;

import br.dev.leandro.spring.cloud.user.converter.CustomJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Desabilitar CSRF para APIs
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/users/public/**").permitAll()
                        // Endpoints administrativos (somente ADMIN)
                        .requestMatchers("/api/users/admin/**").hasRole("ADMIN")
                        // Endpoints organizadores (somente ORGANIZADOR)
                        .requestMatchers("/api/users/organizador/**").hasRole("ORGANIZADOR")
                        // Endpoints participantes (somente PARTICIPANTE)
                        .requestMatchers("/api/users/participante/**").hasRole("PARTICIPANTE")
                        // Endpoints acessíveis ao usuário autenticado
                        .requestMatchers("/api/users/me/**").authenticated()
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
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtAuthenticationConverter());
        return converter;
    }
}
