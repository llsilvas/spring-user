package br.dev.leandro.spring.cloud.user.utils;


import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class TokenUtils {

    public static final String KEY = "jwtToken";

    private TokenUtils() {
    }

    /**
     * Cria um Context contendo o token JWT.
     * @param token JWT do usuário autenticado
     * @return Context contendo o token
     */
    public static Context withToken(String token){
        return Context.of(KEY, token);
    }

    public static String getToken(ContextView context){
        return context.getOrDefault(KEY, null);
    }
}
