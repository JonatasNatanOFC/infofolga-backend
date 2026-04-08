package com.infoway.infofolga.service;

import com.infoway.infofolga.repository.FuncionarioRepository;
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
        UserDetails user = funcionarioRepository.findByCpf(username);
        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com o CPF: " + username);
        }
        return user;
    }
}