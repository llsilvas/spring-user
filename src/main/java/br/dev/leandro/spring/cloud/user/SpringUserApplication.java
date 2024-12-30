package br.dev.leandro.spring.cloud.user;

import br.dev.leandro.spring.cloud.user.keycloak.KeycloakProperties;
import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Log
@EnableConfigurationProperties(KeycloakProperties.class)
@SpringBootApplication
public class SpringUserApplication {

    public static void main(String[] args) {

        log.info(":: Iniciando Spring-User ::");
        long startTime = System.currentTimeMillis(); // Captura o tempo de início

        SpringApplication.run(SpringUserApplication.class, args);
        long endTime = System.currentTimeMillis(); // Captura o tempo de fim
        long totalTime = endTime - startTime; // Calcula o tempo total em milissegundos
        log.info(":: Spring-User iniciado com sucesso :: - " + totalTime + " ms" );
    }

}
