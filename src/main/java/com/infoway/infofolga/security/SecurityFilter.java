package com.infoway.infofolga.security;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final FuncionarioRepository funcionarioRepository;
    private final GerenteRepository gerenteRepository;

    public SecurityFilter(TokenService tokenService,
            FuncionarioRepository funcionarioRepository,
            GerenteRepository gerenteRepository) {
        this.tokenService = tokenService;
        this.funcionarioRepository = funcionarioRepository;
        this.gerenteRepository = gerenteRepository;
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
                    Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByCpf(cpfLimpo);
                    Optional<Gerente> gerenteOpt = gerenteRepository.findByCpf(cpfLimpo);

                    UserDetails usuarioLogado = null;

                    if (funcionarioOpt.isPresent()) {
                        usuarioLogado = funcionarioOpt.get();
                    } else if (gerenteOpt.isPresent()) {
                        usuarioLogado = gerenteOpt.get();
                    }

                    if (usuarioLogado != null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                usuarioLogado,
                                null,
                                usuarioLogado.getAuthorities());

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

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