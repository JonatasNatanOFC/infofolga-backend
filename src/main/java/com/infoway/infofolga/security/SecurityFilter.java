package com.infoway.infofolga.security;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final FuncionarioRepository funcionarioRepository;

    public SecurityFilter(TokenService tokenService, FuncionarioRepository funcionarioRepository) {
        this.tokenService = tokenService;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = recuperarToken(request);

            System.out.println("\n=== SECURITY FILTER ===");
            System.out.println("PATH: " + request.getRequestURI());
            System.out.println("TOKEN PRESENTE: " + (token != null));

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String cpfLimpo = tokenService.validarToken(token);
                System.out.println("CPF TOKEN: " + cpfLimpo);

                if (cpfLimpo != null && !cpfLimpo.isBlank()) {
                    Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByCpf(cpfLimpo);

                    if (funcionarioOpt.isPresent()) {
                        Funcionario funcionario = funcionarioOpt.get();

                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                        if (funcionario.getRole() != null) {
                            authorities.add(new SimpleGrantedAuthority(funcionario.getRole().name()));

                            if ("ROLE_GERENTE".equals(funcionario.getRole().name())) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
                            }
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        funcionario,
                                        null,
                                        authorities
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        System.out.println("USUARIO: " + funcionario.getNome());
                        System.out.println("CPF BANCO: " + funcionario.getCpf());
                        System.out.println("ROLE BANCO: " + funcionario.getRole());
                        System.out.println("AUTHORITIES SETADAS: " + authorities);
                        System.out.println("STATUS: " + funcionario.getStatus());
                    } else {
                        System.out.println("USUARIO NAO ENCONTRADO NO BANCO");
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    System.out.println("TOKEN INVALIDO OU EXPIRADO");
                    SecurityContextHolder.clearContext();
                }
            }

            System.out.println("=======================\n");
        } catch (Exception e) {
            System.out.println("ERRO NO SECURITY FILTER: " + e.getMessage());
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