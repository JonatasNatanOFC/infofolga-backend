package com.infoway.infofolga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Exclui a auto-configuração de segurança do Actuator que criava um segundo
// SecurityFilterChain com CSRF habilitado, conflitando com nossas regras customizadas
@SpringBootApplication(exclude = { ManagementWebSecurityAutoConfiguration.class })
public class InfofolgaApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InfofolgaApiApplication.class, args);
    }
}