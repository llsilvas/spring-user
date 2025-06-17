package br.dev.leandro.spring.cloud.user.exception;

public class AssignRoleException extends RuntimeException {
    public AssignRoleException(String message) {
        super(message);
    }
}

