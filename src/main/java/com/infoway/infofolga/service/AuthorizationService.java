package com.infoway.infofolga.service;

import com.infoway.infofolga.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = funcionarioRepository.findByMatricula(username);
        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com a matrícula: " + username);
        }
        return user;
    }
}