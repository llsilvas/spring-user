package br.dev.leandro.spring.cloud.user.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, jakarta.servlet.ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        log.info("Request URI: {}", httpServletRequest.getRequestURI());
//        log.info("Request Headers:");
//        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
//
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            log.info(headerName + ": " + httpServletRequest.getHeader(headerName));
//        }

        chain.doFilter(request, response);
    }
}

