package com.infoway.infofolga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { ManagementWebSecurityAutoConfiguration.class })
public class InfofolgaApiApplication {
    public static void main(String[] args) {
        System.out.println("######## APP NOVA SUBIU ########");
        SpringApplication.run(InfofolgaApiApplication.class, args);
    }
}