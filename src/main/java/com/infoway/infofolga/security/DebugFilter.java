package com.infoway.infofolga.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DebugFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        log.info(">>> DEBUG FILTER HIT: {} {} from {}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
        chain.doFilter(request, response);
    }
}
