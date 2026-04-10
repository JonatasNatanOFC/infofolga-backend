package com.infoway.infofolga.security;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.service.TokenService;
import com.infoway.infofolga.util.CpfUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Preflight CORS requests (OPTIONS) must pass through without token validation
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = recoverToken(request);
            String path = request.getRequestURI();

            System.out.println("\n[DEBUG] REQUEST PATH: " + path);
            System.out.println("[DEBUG] AUTH HEADER: " + request.getHeader("Authorization"));

            if (token != null) {
                String subject = tokenService.validarToken(token);

                if (subject == null) {
                    System.err.printf("[SecurityFilter] PATH=%s | Token inválido ou expirado%n", path);
                    writeErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado");
                    return;
                }

                String cpfFormatado = CpfUtils.formatar(subject);
                Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByCpfExato(subject, cpfFormatado);

                if (funcionarioOpt.isEmpty()) {
                    System.err.printf("[SecurityFilter] PATH=%s | Usuário não encontrado para CPF=%s%n", path, subject);
                    writeErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Usuário do token não encontrado");
                    return;
                }

                Funcionario user = funcionarioOpt.get();

                System.out.printf(
                        "[SecurityFilter] PATH=%s | CPF=%s | ROLE=%s | AUTHORITIES=%s%n",
                        path,
                        user.getCpf(),
                        user.getRole(),
                        user.getAuthorities());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                        user.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                System.err.printf("[SecurityFilter] PATH=%s | Token ausente%n", path);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            System.err.println("[SecurityFilter] Erro ao validar token: " + e.getMessage());
            writeErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Falha na autenticação");
        }
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }

    /**
     * Writes an error response with CORS headers so that browsers can read the body
     * even when the request is blocked at the security filter level.
     */
    private void writeErrorResponse(HttpServletRequest request,
                                    HttpServletResponse response,
                                    int status,
                                    String mensagem) throws IOException {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}