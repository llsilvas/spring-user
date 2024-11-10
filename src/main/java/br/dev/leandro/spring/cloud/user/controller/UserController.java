package br.dev.leandro.spring.cloud.user.controller;

import br.dev.leandro.spring.cloud.user.service.UserService;
import br.dev.leandro.spring.cloud.user.model.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/admin/hello")
    public String adminHello() {
        return "Olá, Admin!";
    }

    @GetMapping("/user/hello")
    public String userHello() {
        return userService.getMsgUsuario();
    }

    @GetMapping("/useradmin/hello")
    public String userAdminHello() {
        return "Olá, UserAdmin!";
    }

    @PostMapping("/user/usuario")
    public String getUsuario(@RequestBody UserDto user){
        return "O nome do usuário é: " + user.name();
    }
}
