package br.dev.leandro.spring.cloud.user.filter;

//import io.micrometer.tracing.Span;
//import io.micrometer.tracing.Tracer;
//import jakarta.servlet.*;
//import org.slf4j.MDC;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//public class TracingLogFilter implements Filter {
//
//    private final Tracer tracer;
//
//    public TracingLogFilter(Tracer tracer) {
//        this.tracer = tracer;
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//        try {
//            // Obtém o traceId atual do contexto do Micrometer Tracing
//            Span currentSpan = tracer.currentSpan();
//            if (currentSpan != null) {
//                MDC.put("traceId", currentSpan.context().traceId());
//                MDC.put("spanId", currentSpan.context().spanId());
//            }
//            chain.doFilter(request, response);
//        } finally {
//            MDC.clear();
//        }
//    }
//}


