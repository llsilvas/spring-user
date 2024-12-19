package br.dev.leandro.spring.cloud.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "spring.keycloak.admin")
public class KeycloakProperties {

    private String clientId;
    private String clientSecret;
    private String authServerUrl;
    private String realm;

}

