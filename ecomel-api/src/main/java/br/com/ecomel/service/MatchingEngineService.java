package br.com.ecomel.service;

import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
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

    @Transactional
    public void processarMatch(OrdemFavo novaOrdem) {
        // Se for COMPRA, busca VENDAS com preço <= ao meu. Se for VENDA, busca COMPRAS com preço >= ao meu.
        List<OrdemFavo> ordensCompativeis = (novaOrdem.getTipo() == TipoOrdem.COMPRA) ?
            ordemRepository.findVendasCompativeis(novaOrdem.getPrecoUnitario()) :
            ordemRepository.findComprasCompativeis(novaOrdem.getPrecoUnitario());

        for (OrdemFavo ordemAberta : ordensCompativeis) {
            if (novaOrdem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal quantidadeNegociada = novaOrdem.getQuantidadeRestante().min(ordemAberta.getQuantidadeRestante());

            // Execução do Trade
            executarTrade(novaOrdem, ordemAberta, quantidadeNegociada);

            // Atualiza saldos restantes
            novaOrdem.setQuantidadeRestante(novaOrdem.getQuantidadeRestante().subtract(quantidadeNegociada));
            ordemAberta.setQuantidadeRestante(ordemAberta.getQuantidadeRestante().subtract(quantidadeNegociada));

            atualizarStatus(ordemAberta);
        }
        atualizarStatus(novaOrdem);
        ordemRepository.save(novaOrdem);
    }

    private void executarTrade(OrdemFavo nova, OrdemFavo aberta, BigDecimal qtd) {
        // Aqui entra a lógica de transferir ECM de um e FAVOS de outro
        // Usaremos o precoUnitario da ordem que já estava no book (aberta)
    }

    private void atualizarStatus(OrdemFavo ordem) {
        if (ordem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) == 0) {
            ordem.setStatus(StatusOrdem.EXECUTADA);
        } else {
            ordem.setStatus(StatusOrdem.PARCIAL);
        }
    }
}