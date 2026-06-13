package com.infoway.infofolga.service;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.repository.FuncionarioRepository;
import com.infoway.infofolga.repository.GerenteRepository;
import com.infoway.infofolga.util.CpfUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthorizationService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;
    private final GerenteRepository gerenteRepository;

    public AuthorizationService(FuncionarioRepository funcionarioRepository, GerenteRepository gerenteRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.gerenteRepository = gerenteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cpfLimpo = CpfUtils.limpar(username);

        Optional<Funcionario> funcionario = funcionarioRepository.findByCpf(cpfLimpo);
        if (funcionario.isPresent()) {
            return funcionario.get();
        }

        Optional<Gerente> gerente = gerenteRepository.findByCpf(cpfLimpo);
        if (gerente.isPresent()) {
            return gerente.get();
        }

        throw new UsernameNotFoundException("Usuário não encontrado com o CPF informado.");
    }
}