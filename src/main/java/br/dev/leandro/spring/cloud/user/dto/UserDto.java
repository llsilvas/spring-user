package br.dev.leandro.spring.cloud.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDto(

        @NotBlank(message = "O username não pode estar vazio.")
        String username,

        @Email(message = "O e-mail deve ser válido.")
        @NotBlank(message = "O e-mail não pode estar vazio.")
        String email,

        @NotBlank(message = "O primeiro nome não pode estar vazio.")
        String firstName,

        @NotBlank(message = "O último nome não pode estar vazio.")
        String lastName,

        @NotBlank(message = "Password não pode estar vazio.")
        String password,

        @NotBlank(message = "Role não pode estar vazio")
        String role,

        @NotBlank(message = "Nome da organização não pode ser vazio")
        String organizationName,

        @NotBlank(message = "Telefone não pode ser nulo")
        String contactPhone,

        @NotBlank(message = "Numero do documento nao pode ser nulo.")
        String documentNumber) {
}


