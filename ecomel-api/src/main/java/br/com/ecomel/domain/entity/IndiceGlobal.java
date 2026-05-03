package br.com.ecomel.domain.entity;

import br.com.ecomel.domain.entity.base.BaseAuditavel;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "indice_global")
@Getter // Garante o getIndiceFavoAcumulado() e getValor()
@Setter
@NoArgsConstructor
public class IndiceGlobal extends BaseAuditavel {

	@Version
	@Column(name = "versao")
	private Long versao;
	
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal valor = BigDecimal.ONE;

    // Novo campo para o Índice de Distribuição de Favos
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal indiceFavoAcumulado = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal liquidezTotal;

}
