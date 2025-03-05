package br.dev.leandro.spring.cloud.user.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.version}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("Documentação da API utilizando Springdoc-OpenAPI")
                        .version(appVersion)
                        .contact(new Contact()
                                .name("Suporte API")
                                .email("suporte@empresa.com")));
    }
}
