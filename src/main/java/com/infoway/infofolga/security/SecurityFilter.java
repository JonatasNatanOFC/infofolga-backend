package com.infoway.infofolga.security;

import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            var token = recoverToken(request);

            if (token != null) {
                var subject = tokenService.validarToken(token);
                UserDetails user = funcionarioRepository.findByMatricula(subject);

                if (user != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            // Aqui você evita que um token inválido derrube a aplicação inteira
            System.err.println("Erro ao validar token: " + e.getMessage());
        }

        // Continua o fluxo da requisição
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.replace("Bearer ", "");
    }
}