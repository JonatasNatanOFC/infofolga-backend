package com.infoway.infofolga;

import com.infoway.infofolga.model.Gerente;
import com.infoway.infofolga.repository.GerenteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootApplication
public class InfofolgaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfofolgaApiApplication.class, args);
    }

    @Bean
    CommandLineRunner corrigirSenha(GerenteRepository repository, PasswordEncoder encoder) {
        return args -> {
            // Busca o gerente no banco de dados pelo CPF que inserimos
            Optional<Gerente> gerenteOpt = repository.findByCpf("11122233344");

            if (gerenteOpt.isPresent()) {
                Gerente gerente = gerenteOpt.get();

                // Sobrescreve a senha quebrada com o Hash BCrypt real da senha "123456"
                gerente.setSenha(encoder.encode("123456"));
                repository.save(gerente);

                System.out.println("\n=======================================================");
                System.out.println("✅ SUCESSO: SENHA DO GERENTE ATUALIZADA COM HASH BCRYPT!");
                System.out.println("=======================================================\n");
            } else {
                System.out.println("\n⚠️ GERENTE NÃO ENCONTRADO NO BANCO DE DADOS.\n");
            }
        };
    }
}