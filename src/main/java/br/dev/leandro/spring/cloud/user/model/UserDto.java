package br.dev.leandro.spring.cloud.user.model;

public record UserDto(
        String name,
        String email,
        String cpf ) {
}
