package com.infoway.infofolga.config;

import com.infoway.infofolga.model.Funcionario;
import com.infoway.infofolga.model.Role;
import com.infoway.infofolga.repository.FuncionarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(FuncionarioRepository funcionarioRepository, PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (funcionarioRepository.count() == 0) {
            System.out.println("Carregando dados iniciais para teste...");

            Funcionario gerente = new Funcionario();
            gerente.setNome("Natan Gerente");
            gerente.setMatricula("1001");
            gerente.setCargo("Gerente de TI");
            gerente.setSetor("Tecnologia");
            gerente.setCpf("11122233344"); // <--- CPF ADICIONADO AQUI!
            gerente.setSenha(passwordEncoder.encode("admin123"));
            gerente.setRole(Role.ROLE_GERENTE);
            funcionarioRepository.save(gerente);

            Funcionario funcionario = new Funcionario();
            funcionario.setNome("Ana Funcionaria");
            funcionario.setMatricula("2002");
            funcionario.setCargo("Desenvolvedora");
            funcionario.setSetor("Tecnologia");
            funcionario.setCpf("55566677788"); // <--- CPF ADICIONADO AQUI!
            funcionario.setSenha(passwordEncoder.encode("user123"));
            funcionario.setRole(Role.ROLE_FUNCIONARIO);
            funcionarioRepository.save(funcionario);

            System.out.println("Dados iniciais carregados.");
        }
    }
}