package com.infoway.infofolga.service;

import com.infoway.infofolga.dto.LoginRequestDto;
import com.infoway.infofolga.dto.LoginResponseDto;
import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.util.CpfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public LoginResponseDto login(LoginRequestDto data) {
        String cpfLimpo = CpfUtils.limpar(data.cpf());
        var usernamePassword = new UsernamePasswordAuthenticationToken(cpfLimpo, data.senha());

        try {
            log.info("[AuthService] Tentativa de login");
            var auth = authenticationManager.authenticate(usernamePassword);
            var funcionario = (Funcionario) auth.getPrincipal();
            var token = tokenService.gerarToken(funcionario);

            log.info("[AuthService] Login realizado com sucesso para usuário id={}", funcionario.getId());

            return new LoginResponseDto(token, funcionario.getNome(), funcionario.getRole());
        } catch (AuthenticationException e) {
            log.warn("[AuthService] Falha de autenticação: {}", e.getClass().getSimpleName());
            throw e;
        }
    }
}