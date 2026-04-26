package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.OrdemFavoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingEngineService {

    private final OrdemFavoRepository ordemRepository;
    private final CarteiraRepository carteiraRepository;

    @Transactional
    public void processarMatch(OrdemFavo novaOrdem) {
        // Busca ordens compatíveis no book
        List<OrdemFavo> compativeis = (novaOrdem.getTipo() == TipoOrdem.COMPRA) ?
            ordemRepository.findVendasCompativeis(novaOrdem.getPrecoUnitario()) :
            ordemRepository.findComprasCompativeis(novaOrdem.getPrecoUnitario());

        for (OrdemFavo aberta : compativeis) {
            if (novaOrdem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) <= 0) break;

            // Quantidade que será negociada agora
            BigDecimal qtdNegociada = novaOrdem.getQuantidadeRestante().min(aberta.getQuantidadeRestante());
            
            // Preço de execução (sempre o preço de quem já estava no book)
            BigDecimal precoExecucao = aberta.getPrecoUnitario();
            BigDecimal volumeEcm = qtdNegociada.multiply(precoExecucao);

            // Realizar a troca de ativos
            executarTroca(novaOrdem, aberta, qtdNegociada, volumeEcm);

            // Atualizar ordens
            novaOrdem.setQuantidadeRestante(novaOrdem.getQuantidadeRestante().subtract(qtdNegociada));
            aberta.setQuantidadeRestante(aberta.getQuantidadeRestante().subtract(qtdNegociada));

            atualizarStatus(aberta);
            ordemRepository.save(aberta);
        }
        atualizarStatus(novaOrdem);
        ordemRepository.save(novaOrdem);
    }

    private void executarTroca(OrdemFavo nova, OrdemFavo aberta, BigDecimal qtd, BigDecimal ecm) {
        Carteira comprador = (nova.getTipo() == TipoOrdem.COMPRA) ? nova.getCarteira() : aberta.getCarteira();
        Carteira vendedor = (nova.getTipo() == TipoOrdem.VENDA) ? nova.getCarteira() : aberta.getCarteira();

        // 1. Entrega FAVOS ao comprador
        comprador.setSaldoFavos(comprador.getSaldoFavos().add(qtd));
        
        // 2. Entrega ECM ao vendedor
        vendedor.setSaldoBase(vendedor.getSaldoBase().add(ecm));

        // 3. Se o comprador pagou MAIS BARATO do que reservou originalmente (Sobra de ECM)
        if (nova.getTipo() == TipoOrdem.COMPRA && nova.getPrecoUnitario().compareTo(aberta.getPrecoUnitario()) > 0) {
             BigDecimal estornoEcm = qtd.multiply(nova.getPrecoUnitario().subtract(aberta.getPrecoUnitario()));
             comprador.setSaldoBase(comprador.getSaldoBase().add(estornoEcm));
        }

        carteiraRepository.save(comprador);
        carteiraRepository.save(vendedor);
    }

    private void atualizarStatus(OrdemFavo o) {
        if (o.getQuantidadeRestante().compareTo(BigDecimal.ZERO) == 0) {
            o.setStatus(StatusOrdem.EXECUTADA);
        } else if (o.getQuantidadeRestante().compareTo(o.getQuantidadeOriginal()) < 0) {
            o.setStatus(StatusOrdem.PARCIAL);
        }
    }
}
