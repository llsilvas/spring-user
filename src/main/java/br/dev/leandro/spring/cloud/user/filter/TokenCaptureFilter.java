package br.dev.leandro.spring.cloud.user.filter;

import br.dev.leandro.spring.cloud.user.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TokenCaptureFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("Token capturado no filtro: {}...", token != null ? token.substring(0, 10) : "null");
        if(token != null && token.startsWith("Bearer ")){
            token = token.substring(7);
            return   chain.filter(exchange).contextWrite(TokenUtils.withToken(token));
        }
        return chain.filter(exchange);
    }
}
