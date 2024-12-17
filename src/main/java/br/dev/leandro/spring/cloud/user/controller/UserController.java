package br.dev.leandro.spring.cloud.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // Endpoint Público
    @GetMapping("/public/info")
    public ResponseEntity<String> getPublicInfo() {
        return ResponseEntity.ok("Informação pública acessível a todos.");
    }

    // Endpoint Protegido para Administradores
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAllUsers() {
        return ResponseEntity.ok("Lista de todos os usuários (Apenas admin).");
    }

    // Endpoint Protegido para Organizadores
    @GetMapping("/organizador/events")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<String> getOrganizadorEvents() {
        return ResponseEntity.ok("Eventos organizados (Apenas organizadores).");
    }

    // Endpoint Protegido para Participantes
    @GetMapping("/participante/profile")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    public ResponseEntity<String> getParticipantProfile() {
        return ResponseEntity.ok("Perfil do participante (Apenas participantes).");
    }

    // Endpoint Protegido para Autenticados
    @GetMapping("/profile")
    public ResponseEntity<String> getUserProfile() {
        return ResponseEntity.ok("Perfil do usuário autenticado.");
    }
}
