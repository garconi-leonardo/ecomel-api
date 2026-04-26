package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import br.com.ecomel.domain.enums.TipoTransacao;
import br.com.ecomel.domain.enums.StatusTransacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "transacoes")
@Getter
@Setter
public class Transacao extends BaseAuditavel {

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_destino_id")
    private Carteira carteiraDestino; // Preenchido apenas em TRANSFERENCIA_INTERNA
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo;

    @Column(nullable = false, precision = 20, scale = 18)
    private BigDecimal valorBruto;

    @Column(nullable = false, precision = 20, scale = 18)
    private BigDecimal valorLiquido;

    @Column(nullable = false, precision = 20, scale = 18)
    private BigDecimal taxaTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransacao status = StatusTransacao.PENDENTE;

}
