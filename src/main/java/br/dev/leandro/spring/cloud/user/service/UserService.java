package br.dev.leandro.spring.cloud.user.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@RefreshScope
@Service
@Getter
public class UserService {

    @Value("${app.mensagem.usuario}")
    private String msgUsuario;
}
