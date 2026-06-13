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
                    Optional<Gerente> gerenteOpt = gerenteRepository.findByCpf(cpfLimpo);

                    UserDetails usuarioLogado = null;
                    String tipoConta = "DESCONHECIDO";

                    if (funcionarioOpt.isPresent()) {
                        usuarioLogado = funcionarioOpt.get();
                        tipoConta = "FUNCIONARIO";
                    } else if (gerenteOpt.isPresent()) {
                        usuarioLogado = gerenteOpt.get();
                        tipoConta = "GERENTE";
                    }

                    if (usuarioLogado != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        usuarioLogado,
                                        null,
                                        usuarioLogado.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        System.out.println("TIPO DA CONTA: " + tipoConta);
                        System.out.println("CPF BANCO: " + usuarioLogado.getUsername());
                        System.out.println("AUTHORITIES SETADAS: " + usuarioLogado.getAuthorities());
                        System.out.println("STATUS: " + (usuarioLogado.isEnabled() ? "ATIVO" : "INATIVO"));
                    } else {
                        System.out.println("USUARIO NAO ENCONTRADO EM NENHUMA TABELA");
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