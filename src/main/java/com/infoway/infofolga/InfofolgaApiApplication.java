package com.infoway.infofolga;

import com.infoway.infofolga.repository.GerenteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class InfofolgaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfofolgaApiApplication.class, args);
    }

    @Bean
    CommandLineRunner resetSenha(GerenteRepository repository, PasswordEncoder encoder) {
        return args -> {
            repository.findByCpf("00011122233").ifPresent(g -> {
                g.setSenha(encoder.encode("123456"));
                repository.save(g);
                System.out.println("✅ Senha do gerente redefinida para 123456");
            });
        };
    }
}