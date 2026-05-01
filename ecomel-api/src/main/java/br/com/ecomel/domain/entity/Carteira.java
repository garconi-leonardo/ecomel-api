package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Table(name = "carteiras")
@Getter
@Setter
public class Carteira extends BaseAuditavel {

    @Column(nullable = false, unique = true, length = 20)
    private String codigoEndereco; // Identificador AAA001

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Quantidade de tokens ECOMEL da carteira.
     * SEMPRE inteiro, com arredondamento para menos em qualquer conversão.
     * Substitui o antigo "saldoBase".
     */
    @Column(name = "token_ecomel", nullable = false, precision = 38, scale = 8)
    private BigDecimal tokenEcomel = BigDecimal.ZERO;

    // Saldo de FAVOS (Ativo negociável)
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal saldoFavos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal ultimoIndiceFavo = BigDecimal.ZERO;

    /**
     * Calcula o saldo real (valorizado) em ECM.
     * saldoReal = tokenEcomel * indiceGlobal
     */
    public BigDecimal getSaldoReal(BigDecimal valorIndiceGlobal) {
        return this.tokenEcomel.multiply(valorIndiceGlobal);
    }
}
