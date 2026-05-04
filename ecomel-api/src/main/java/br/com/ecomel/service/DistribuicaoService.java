package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
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

        BigDecimal totalFavosGlobal = carteiraRepository.sumTotalFavos();

        if (totalFavosGlobal.compareTo(BigDecimal.ZERO) <= 0) return;

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrueWithLock();

        BigDecimal incrementoPorFavo = valorTotalTransacaoEcm
                .divide(totalFavosGlobal, 18, RoundingMode.DOWN);

        indice.setIndiceFavoAcumulado(
                indice.getIndiceFavoAcumulado().add(incrementoPorFavo)
        );

        indiceRepository.save(indice);
    }
    
    @Transactional
    public void aplicarRendimentoFavos(Carteira carteira) {

        if (carteira.getSaldoFavos().compareTo(BigDecimal.ZERO) <= 0) return;

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrueWithLock();

        BigDecimal indiceAtual = indice.getIndiceFavoAcumulado();
        BigDecimal ultimoIndice = carteira.getUltimoIndiceFavo();

        BigDecimal diferenca = indiceAtual.subtract(ultimoIndice);

        if (diferenca.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal ganho = diferenca.multiply(carteira.getSaldoFavos());

        //crédito em ECOMEL (mel virando token)
        carteira.setTokenEcomel(
            carteira.getTokenEcomel().add(ganho)
        );

        //atualizar checkpoint
        carteira.setUltimoIndiceFavo(indiceAtual);
    }


}
