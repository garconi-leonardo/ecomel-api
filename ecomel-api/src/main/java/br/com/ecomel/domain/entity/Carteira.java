package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "carteiras")
@Getter
@Setter
public class Carteira extends BaseAuditavel {

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Saldo base para cálculo da ECM
    @Column(nullable = false, precision = 20, scale = 18)
    private BigDecimal saldoBase = BigDecimal.ZERO;

    // Saldo de FAVOS (Ativo negociável)
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal saldoFavos = BigDecimal.ZERO;

    /**
     * Calcula o saldo real em ECM.
     * saldoReal = saldoBase * indiceGlobal
     */
    public BigDecimal getSaldoReal(BigDecimal valorIndiceGlobal) {
        return this.saldoBase.multiply(valorIndiceGlobal);
    }
}
