package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import br.com.ecomel.domain.enums.TipoOrdem; // COMPRA, VENDA
import br.com.ecomel.domain.enums.StatusOrdem; // ABERTA, PARCIAL, EXECUTADA, CANCELADA
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ordens_favos")
public class OrdemFavo extends BaseAuditavel {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao")
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;
    
    @Column(name = "request_key", unique = true, length = 64)
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOrdem tipo;

    @Column(name = "quantidade_original", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantidadeOriginal;

    @Column(name = "quantidade_restante", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantidadeRestante;

    @Column(name = "preco_unitario", nullable = false, precision = 20, scale = 18)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOrdem status;

}
