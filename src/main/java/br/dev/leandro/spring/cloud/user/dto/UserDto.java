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
        String password,
        String role) {
}


