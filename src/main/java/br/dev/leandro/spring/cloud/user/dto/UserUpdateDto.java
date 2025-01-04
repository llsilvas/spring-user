package br.dev.leandro.spring.cloud.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public record UserUpdateDto(
        @NotBlank(message = "O username não pode estar vazio.")
        String username,

        @Email(message = "O e-mail deve ser válido.")
        Optional<String> email,

        Optional<String> firstName,

        Optional<String> lastName,

        Optional<String> password) {
}


