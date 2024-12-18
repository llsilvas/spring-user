package br.dev.leandro.spring.cloud.user.model;

public record UserDto(
        String username,
        String email,
        String firstName,
        String lastName,
        String password,
        String role) {
}
