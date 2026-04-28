package br.com.ecomel.service;

import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DistribuicaoService {

    private final CarteiraRepository carteiraRepository;
    private final IndiceGlobalRepository indiceRepository;

    @Transactional
    public void distribuirTaxaFavos(BigDecimal valorTotalTransacaoEcm) {
        // 1. Calcular montante de 4,01% da transação (em ECM)
        BigDecimal montanteEcmParaFavos = valorTotalTransacaoEcm.multiply(new BigDecimal("0.0401"));
        
        // 2. Obter total de FAVOS circulantes no sistema
        BigDecimal totalFavosGlobal = carteiraRepository.sumTotalFavos();
        
        if (totalFavosGlobal.compareTo(BigDecimal.ZERO) <= 0) return;

        // 3. Atualizar o Indice Global de Favos (Acumulador)
        // NovoIndice = AntigoIndice + (MontanteDistribuicao / TotalFavos)
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        
        BigDecimal incrementoPorFavo = montanteEcmParaFavos.divide(totalFavosGlobal, 18, RoundingMode.HALF_UP);
        indice.setIndiceFavoAcumulado(indice.getIndiceFavoAcumulado().add(incrementoPorFavo));
        
        indiceRepository.save(indice);
    }
}
