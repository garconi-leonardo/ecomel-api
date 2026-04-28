package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import br.com.ecomel.domain.enums.TipoOrdem; // COMPRA, VENDA
import br.com.ecomel.domain.enums.StatusOrdem; // ABERTA, PARCIAL, EXECUTADA, CANCELADA
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ordens_favos")
@Getter
@Setter
public class OrdemFavo extends BaseAuditavel {

    @ManyToOne(fetch = FetchType.LAZY)
    private Carteira carteira;

    @Enumerated(EnumType.STRING)
    private TipoOrdem tipo;

    private BigDecimal quantidadeOriginal;
    private BigDecimal quantidadeRestante;
    
    @Column(precision = 38, scale = 18)
    private BigDecimal precoUnitario; // Preço em ECM por 1 FAVO

    @Enumerated(EnumType.STRING)
    private StatusOrdem status = StatusOrdem.ABERTA;
}
