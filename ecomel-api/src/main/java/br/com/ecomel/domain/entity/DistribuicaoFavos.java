package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "distribuicao_favos")
@Getter @Setter
public class DistribuicaoFavos extends BaseAuditavel {
    
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal montanteEcmTotal; // Valor total em ECM distribuído nesta rodada
    
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal ecmPorFavo; // Quanto cada 1.00000000 FAVO recebeu
}