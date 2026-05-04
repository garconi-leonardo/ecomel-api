package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.OrdemExecucao;
import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.OrdemExecucaoRepository;
import br.com.ecomel.repository.OrdemFavoRepository;
import br.com.ecomel.util.CalculoFinanceiroUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingEngineService {

    private final OrdemFavoRepository ordemRepository;
    private final CarteiraRepository carteiraRepository;
    private final OrdemExecucaoRepository execucaoRepository;

    @Transactional
    public void processarMatch(OrdemFavo novaOrdem) {

        List<OrdemFavo> compativeis = (novaOrdem.getTipo() == TipoOrdem.COMPRA)
                ? ordemRepository.findVendasParaMatch(novaOrdem.getPrecoUnitario())
                : ordemRepository.findComprasParaMatch(novaOrdem.getPrecoUnitario());

        for (OrdemFavo aberta : compativeis) {

            if (novaOrdem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) <= 0) break;

            if (!isOrdemValidaParaMatch(aberta)) continue;

            BigDecimal qtdNegociada = novaOrdem.getQuantidadeRestante()
                    .min(aberta.getQuantidadeRestante());

            BigDecimal precoExecucao = aberta.getPrecoUnitario();
            BigDecimal volumeEcm = qtdNegociada.multiply(precoExecucao);

            executarTroca(novaOrdem, aberta, qtdNegociada, precoExecucao, volumeEcm);

            // Atualiza quantidades
            novaOrdem.setQuantidadeRestante(
                    novaOrdem.getQuantidadeRestante().subtract(qtdNegociada)
            );

            aberta.setQuantidadeRestante(
                    aberta.getQuantidadeRestante().subtract(qtdNegociada)
            );

            atualizarStatus(aberta);
            ordemRepository.save(aberta);

            // 🔥 REGISTRO CORRIGIDO
            registrarExecucao(novaOrdem, aberta, qtdNegociada, precoExecucao);
        }

        liberarSaldoRestante(novaOrdem);

        atualizarStatus(novaOrdem);
        ordemRepository.save(novaOrdem);
    }

    private void executarTroca(
            OrdemFavo nova,
            OrdemFavo aberta,
            BigDecimal qtd,
            BigDecimal precoExecucao,
            BigDecimal volumeEcm
    ) {

        Carteira comprador = (nova.getTipo() == TipoOrdem.COMPRA)
                ? nova.getCarteira()
                : aberta.getCarteira();

        Carteira vendedor = (nova.getTipo() == TipoOrdem.VENDA)
                ? nova.getCarteira()
                : aberta.getCarteira();

        if (comprador.getId().equals(vendedor.getId())) {
            throw new BusinessException("Self-trade não permitido.");
        }

        // FAVOS
        comprador.setSaldoFavos(comprador.getSaldoFavos().add(qtd));
        vendedor.setSaldoFavosBloqueado(
                vendedor.getSaldoFavosBloqueado().subtract(qtd)
        );

        // ECM
        BigDecimal ecmTokens = CalculoFinanceiroUtils.formatarEcm(volumeEcm);

        vendedor.setTokenEcomel(vendedor.getTokenEcomel().add(ecmTokens));
        comprador.setTokenEcomelBloqueado(
                comprador.getTokenEcomelBloqueado().subtract(ecmTokens)
        );

        // Estorno
        if (nova.getTipo() == TipoOrdem.COMPRA &&
                nova.getPrecoUnitario().compareTo(precoExecucao) > 0) {

            BigDecimal diferenca = nova.getPrecoUnitario().subtract(precoExecucao);
            BigDecimal estorno = qtd.multiply(diferenca);
            BigDecimal estornoToken = CalculoFinanceiroUtils.formatarEcm(estorno);

            comprador.setTokenEcomel(
                    comprador.getTokenEcomel().add(estornoToken)
            );

            comprador.setTokenEcomelBloqueado(
                    comprador.getTokenEcomelBloqueado().subtract(estornoToken)
            );
        }

        carteiraRepository.save(comprador);
        carteiraRepository.save(vendedor);
    }

    /**
     * 🔥 CORREÇÃO PRINCIPAL AQUI
     */
    private void registrarExecucao(
            OrdemFavo nova,
            OrdemFavo aberta,
            BigDecimal qtd,
            BigDecimal precoExecucao
    ) {

        OrdemExecucao execucao = new OrdemExecucao();

        OrdemFavo ordemCompra = (nova.getTipo() == TipoOrdem.COMPRA) ? nova : aberta;
        OrdemFavo ordemVenda  = (nova.getTipo() == TipoOrdem.VENDA) ? nova : aberta;

        execucao.setOrdemCompra(ordemCompra); // ✅ agora é objeto
        execucao.setOrdemVenda(ordemVenda);   // ✅ agora é objeto
        execucao.setQuantidade(qtd);
        execucao.setPrecoExecucao(precoExecucao);
        execucao.setCriadoEm(LocalDateTime.now());

        execucaoRepository.save(execucao);
    }

    private void liberarSaldoRestante(OrdemFavo ordem) {

        if (ordem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) <= 0) return;

        Carteira carteira = ordem.getCarteira();

        if (ordem.getTipo() == TipoOrdem.VENDA) {

            carteira.setSaldoFavos(
                    carteira.getSaldoFavos().add(ordem.getQuantidadeRestante())
            );

            carteira.setSaldoFavosBloqueado(
                    carteira.getSaldoFavosBloqueado().subtract(ordem.getQuantidadeRestante())
            );

        } else {

            BigDecimal valorRestante = ordem.getQuantidadeRestante()
                    .multiply(ordem.getPrecoUnitario());

            BigDecimal tokensRestantes = CalculoFinanceiroUtils.formatarEcm(valorRestante);

            carteira.setTokenEcomel(
                    carteira.getTokenEcomel().add(tokensRestantes)
            );

            carteira.setTokenEcomelBloqueado(
                    carteira.getTokenEcomelBloqueado().subtract(tokensRestantes)
            );
        }

        carteiraRepository.save(carteira);
    }

    private boolean isOrdemValidaParaMatch(OrdemFavo ordem) {
        return ordem.getStatus() == StatusOrdem.ABERTA
                || ordem.getStatus() == StatusOrdem.PARCIALMENTE_EXECUTADA;
    }

    private void atualizarStatus(OrdemFavo ordem) {

        if (ordem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) == 0) {
            ordem.setStatus(StatusOrdem.EXECUTADA);

        } else if (ordem.getQuantidadeRestante()
                .compareTo(ordem.getQuantidadeOriginal()) < 0) {
            ordem.setStatus(StatusOrdem.PARCIALMENTE_EXECUTADA);
        }
    }
}
