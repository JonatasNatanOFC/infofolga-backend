package com.infoway.infofolga.service;

import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;

    public AuthorizationService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cpfLimpo = CpfUtils.limpar(username);

        return funcionarioRepository.findByCpf(cpfLimpo)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário não encontrado com o CPF informado."));
    }
}