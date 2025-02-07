package br.dev.leandro.spring.cloud.user.controller;

import br.dev.leandro.spring.cloud.user.dto.UserDto;
import br.dev.leandro.spring.cloud.user.dto.UserUpdateDto;
import br.dev.leandro.spring.cloud.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/admin/create")
    public Mono<ResponseEntity<String>> createUser(@Valid @RequestBody UserDto request) {
        return userService.createUser(request)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com sucesso.")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Erro ao criar usuário: " + e.getMessage())));
    }

    @PutMapping("/admin/{id}")
    public Mono<ResponseEntity<String>> updateUser(@PathVariable("id") String id, @Valid @RequestBody UserUpdateDto userDto) {
        return userService.updateUser(id, userDto)
                .then(Mono.just(ResponseEntity.status(HttpStatus.OK).body("Usuário atualizado com sucesso!"))
                        .onErrorResume(RuntimeException.class, e ->
                                Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro: " + e.getMessage()))
                        )
                        .onErrorResume(Exception.class, e ->
                                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage()))));
    }

    @DeleteMapping("/admin/{id}")
    public Mono<ResponseEntity<String>> deleteUser(@PathVariable("id") String id) {
        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.status(HttpStatus.OK).body("Usuário deletado com sucesso!"))
                        .onErrorResume(RuntimeException.class, e ->
                                Mono.just(ResponseEntity.status(500).body("Erro: " + e.getMessage()))
                        )
                        .onErrorResume(Exception.class, e ->
                                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage()))));
    }

    @GetMapping("/admin/{id}")
    public Mono<ResponseEntity<UserDto>> getUserById(@PathVariable("id") String id) {

        return userService.findUserById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is4xxClientError()) {
                        return Mono.just(ResponseEntity.status(e.getStatusCode()).body(null));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
                    }
                });
    }

    @GetMapping("/admin")
    public Mono<ResponseEntity<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String email,
            @RequestParam(defaultValue = "0") Integer first,
            @RequestParam(defaultValue = "10") Integer max,
            @RequestParam(defaultValue = "username") String orderBy,
            @RequestParam(defaultValue = "true") Boolean ascending) {

        return userService.findAllUsers(username, email, first, max, orderBy, ascending)
                .map(ResponseEntity::ok)
                .onErrorResume(WebClientResponseException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Erro ao buscar usuários"))));
    }


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
