package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Gerente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(length = 50)
    private String matricula;

    @Column(length = 50)
    private String cargo;

    @Column(length = 50)
    private String setor;

    @Column(unique = true, nullable = false)
    private String cpf;

    @Column(nullable = false)
    private String senha;

    @Column(length = 20)
    private String status = "ativo";

    @Column(nullable = false)
    private boolean isCeo = false;

    // NOVO CAMPO ADICIONADO AQUI 👇
    @Column(columnDefinition = "TEXT")
    private String foto;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.isCeo) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_CEO"),
                    new SimpleGrantedAuthority("ROLE_GERENTE"),
                    new SimpleGrantedAuthority("ROLE_FUNCIONARIO")
            );
        }
        return List.of(
                new SimpleGrantedAuthority("ROLE_GERENTE"),
                new SimpleGrantedAuthority("ROLE_FUNCIONARIO")
        );
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.cpf;
    }

    @Override
    public boolean isEnabled() {
        return "ativo".equalsIgnoreCase(this.status);
    }
}