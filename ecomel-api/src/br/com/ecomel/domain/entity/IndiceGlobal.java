package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "indice_global")
@Getter
@Setter
@NoArgsConstructor
public class IndiceGlobal extends BaseAuditavel {

    @Column(nullable = false, precision = 20, scale = 18)
    private BigDecimal valor = BigDecimal.ONE; // Inicia em 1.0000...
}
