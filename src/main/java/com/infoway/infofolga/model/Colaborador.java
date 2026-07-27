package com.infoway.infofolga.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Table(name = "colaboradores")
@Entity(name = "Colaborador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Colaborador implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String cpf;

    @Column(unique = true)
    private String email;

    private String senha;

    private String cargo;
    private String setor;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "colaborador")
    private List<Solicitacao> minhasSolicitacoes;
    private String status;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == Role.CEO) {
            return List.of(new SimpleGrantedAuthority("ROLE_CEO"), new SimpleGrantedAuthority("ROLE_GERENTE"),
                    new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        } else if (this.role == Role.GERENTE) {
            return List.of(new SimpleGrantedAuthority("ROLE_GERENTE"), new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        }
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return cpf;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !"inativo".equalsIgnoreCase(this.status);
    }
}