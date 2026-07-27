package com.infoway.infofolga.security;

import com.infoway.infofolga.model.Colaborador;
import com.infoway.infofolga.repository.ColaboradorRepository;
import com.infoway.infofolga.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

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

        try {
            String token = recuperarToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String cpfLimpo = tokenService.validarToken(token);

                if (cpfLimpo != null && !cpfLimpo.isBlank()) {
                    Optional<Colaborador> colaboradorOpt = colaboradorRepository.findByCpf(cpfLimpo);

                    if (colaboradorOpt.isPresent()) {
                        Colaborador colaborador = colaboradorOpt.get();

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                colaborador,
                                null,
                                colaborador.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}