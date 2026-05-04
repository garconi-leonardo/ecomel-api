package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ordens_execucoes",
       indexes = {
           @Index(name = "idx_execucao_compra", columnList = "ordem_compra_id"),
           @Index(name = "idx_execucao_venda", columnList = "ordem_venda_id"),
           @Index(name = "idx_execucao_criado_em", columnList = "criadoEm")
       }
)
public class OrdemExecucao extends BaseAuditavel {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ordem_compra_id")
	private OrdemFavo ordemCompra;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ordem_venda_id")
	private OrdemFavo ordemVenda;

    /**
     * 🔹 Quantidade de FAVOS negociada
     */
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantidade;

    /**
     * 🔹 Preço unitário da execução (em ECM)
     */
    @Column(name = "preco_execucao", nullable = false, precision = 20, scale = 18)
    private BigDecimal precoExecucao;
}
