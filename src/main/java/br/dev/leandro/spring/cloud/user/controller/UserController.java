package br.dev.leandro.spring.cloud.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    @GetMapping("/admin/hello")
    public String adminHello() {
        return "Olá, Admin!";
    }

    @GetMapping("/user/hello")
    public String userHello() {
        return "Olá, Usuário!";
    }

    @GetMapping("/useradmin/hello")
    public String userAdminHello() {
        return "Olá, UserAdmin!";
    }
}
