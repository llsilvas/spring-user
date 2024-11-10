package br.dev.leandro.spring.cloud.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {

    private String id;
    private String name;
    private String email;
    private String cpf;

}
