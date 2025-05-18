package br.dev.leandro.spring.cloud.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrganizerCreateDto(
        @NotBlank String userId,
        @NotBlank String organizationName,
        @Email String contactEmail,
        @NotBlank String contactPhone,
        @NotBlank String documentNumber
) {
}
