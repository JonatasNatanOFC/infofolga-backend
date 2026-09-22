package com.infoway.infofolga.security;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final ColaboradorRepository colaboradorRepository;

    public SecurityFilter(TokenService tokenService, ColaboradorRepository colaboradorRepository) {
        this.tokenService = tokenService;
        this.colaboradorRepository = colaboradorRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = recuperarToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String cpf = tokenService.validarToken(token);

                if (cpf != null && !cpf.isBlank()) {
                    colaboradorRepository.findByCpf(cpf)
                            // Colaboradores inativados perdem o acesso imediatamente, mesmo com token válido
                            .filter(Colaborador::isEnabled)
                            .ifPresent(colaborador -> {
                                var authentication = new UsernamePasswordAuthenticationToken(
                                        colaborador, null, colaborador.getAuthorities());
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
                }
            } catch (Exception e) {
                log.warn("Falha ao autenticar token: {}", e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
