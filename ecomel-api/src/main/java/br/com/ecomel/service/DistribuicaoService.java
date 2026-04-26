package br.com.ecomel.service;

import br.com.ecomel.repository.CarteiraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

/*
 * A Lógica dos 4,01% dos FAVOS
	Esta classe cuidará de premiar quem possui FAVOS.
 * */

@Service
@RequiredArgsConstructor
public class DistribuicaoService {

    private final CarteiraRepository carteiraRepository;

    @Transactional
    public void distribuirTaxaFavos(BigDecimal valorTotalTransacao) {
        // 1. Calcular montante para FAVOS (4,01%)
        BigDecimal montanteFavos = valorTotalTransacao.multiply(new BigDecimal("0.0401"));
        
        // 2. Obter total de FAVOS no mundo para cálculo proporcional
        // (Simulação: buscar a soma de todos os saldoFavos)
        BigDecimal totalFavosCirculantes = new BigDecimal("1000000"); // Exemplo fixo
        
        // 3. A regra diz: participacao = (favos_usuario / total_favos) * valor_favos
        // Em um sistema real, aqui usaremos um processamento em lote ou 
        // um Índice de Distribuição similar ao Índice Global para evitar loops O(n).
    }
}
