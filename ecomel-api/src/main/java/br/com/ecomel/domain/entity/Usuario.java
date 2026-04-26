package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import br.com.ecomel.domain.enums.StatusUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario extends BaseAuditavel {

    @Column(nullable = false)
    private String nome;

    @Column(unique = true) // Removido nullable=false
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Carteira carteira;

    // Relacionamento para perfis de acesso (Roles) futuramente no Security
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_perfis", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "perfil")
    private Set<String> perfis = new HashSet<>();
}
