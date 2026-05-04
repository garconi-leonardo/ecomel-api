package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Entity
@Table(name = "carteiras")
public class Carteira extends BaseAuditavel {

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "codigo_carteira", nullable = false, unique = true, length = 20)
    private String codigoCarteira;

    @Column(name = "token_ecomel", nullable = false, precision = 38, scale = 8)
    private BigDecimal tokenEcomel = BigDecimal.ZERO;

    @Column(name = "token_ecomel_bloqueado", nullable = false, precision = 38, scale = 8)
    private BigDecimal tokenEcomelBloqueado = BigDecimal.ZERO;

    @Column(name = "saldo_favos", nullable = false, precision = 38, scale = 8)
    private BigDecimal saldoFavos = BigDecimal.ZERO;

    @Column(name = "saldo_favos_bloqueado", nullable = false, precision = 38, scale = 8)
    private BigDecimal saldoFavosBloqueado = BigDecimal.ZERO;

    @Column(name = "ultimo_indice_favo", nullable = false, precision = 38, scale = 18)
    private BigDecimal ultimoIndiceFavo = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // =========================================
    // 🔥 SALDOS DERIVADOS
    // =========================================

    public BigDecimal getSaldoEcomelDisponivel() {
        return tokenEcomel.subtract(tokenEcomelBloqueado);
    }

    public BigDecimal getSaldoFavosDisponivel() {
        return saldoFavos.subtract(saldoFavosBloqueado);
    }

    /**
     * 🔥 SALDO REAL (DERIVADO DO ÍNDICE)
     */
    public BigDecimal getSaldoReal(BigDecimal indiceAtual) {
        if (indiceAtual == null || indiceAtual.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return tokenEcomel
                .multiply(indiceAtual)
                .setScale(2, RoundingMode.DOWN);
    }

    // =========================================
    // 🔥 VALIDAÇÕES
    // =========================================

    public boolean temSaldoEcomelSuficiente(BigDecimal valor) {
        return getSaldoEcomelDisponivel().compareTo(valor) >= 0;
    }

    public boolean temSaldoFavosSuficiente(BigDecimal quantidade) {
        return getSaldoFavosDisponivel().compareTo(quantidade) >= 0;
    }

    // =========================================
    // 🔥 BLOQUEIO
    // =========================================

    public void bloquearEcomel(BigDecimal valor) {
        if (!temSaldoEcomelSuficiente(valor)) {
            throw new RuntimeException("Saldo ECM insuficiente para bloqueio.");
        }
        this.tokenEcomelBloqueado = this.tokenEcomelBloqueado.add(valor);
    }

    public void liberarEcomel(BigDecimal valor) {
        this.tokenEcomelBloqueado = this.tokenEcomelBloqueado.subtract(valor);
    }

    public void bloquearFavos(BigDecimal quantidade) {
        if (!temSaldoFavosSuficiente(quantidade)) {
            throw new RuntimeException("Saldo de FAVOS insuficiente para bloqueio.");
        }
        this.saldoFavosBloqueado = this.saldoFavosBloqueado.add(quantidade);
    }

    public void liberarFavos(BigDecimal quantidade) {
        this.saldoFavosBloqueado = this.saldoFavosBloqueado.subtract(quantidade);
    }

    // =========================================
    // 🔥 MOVIMENTAÇÃO
    // =========================================

    public void creditarEcomel(BigDecimal valor) {
        this.tokenEcomel = this.tokenEcomel.add(valor);
    }

    public void debitarEcomel(BigDecimal valor) {
        if (tokenEcomel.compareTo(valor) < 0) {
            throw new RuntimeException("Saldo ECM insuficiente.");
        }
        this.tokenEcomel = this.tokenEcomel.subtract(valor);
    }

    public void creditarFavos(BigDecimal quantidade) {
        this.saldoFavos = this.saldoFavos.add(quantidade);
    }

    public void debitarFavos(BigDecimal quantidade) {
        if (saldoFavos.compareTo(quantidade) < 0) {
            throw new RuntimeException("Saldo de FAVOS insuficiente.");
        }
        this.saldoFavos = this.saldoFavos.subtract(quantidade);
    }
}
