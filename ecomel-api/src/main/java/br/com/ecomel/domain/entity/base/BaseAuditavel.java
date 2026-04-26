package br.com.ecomel.domain.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @CreatedBy
    @Column(updatable = false)
    private String criadoPor;

    @LastModifiedDate
    private LocalDateTime atualizadoEm;

    @LastModifiedBy
    private String atualizadoPor;

    private LocalDateTime desativadoEm;

    private String desativadoPor;

    @Column(nullable = false)
    private boolean ativo = true;
}
