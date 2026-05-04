package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.domain.entity.Transacao;
import br.com.ecomel.domain.enums.StatusTransacao;
import br.com.ecomel.domain.enums.TipoTransacao;
import br.com.ecomel.dto.request.TransferenciaRequest;
import br.com.ecomel.dto.response.TransacaoResponse;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import br.com.ecomel.repository.TransacaoRepository;
import br.com.ecomel.util.CalculoFinanceiroUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final IndiceGlobalRepository indiceRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoRepository transacaoRepository;
    private final DistribuicaoService distribuicaoService;
    private final CarteiraService carteiraService;

    @Transactional
    public void processarDeposito(Long usuarioId, BigDecimal valorReal, String requestKey) {

        verificarIdempotencia(requestKey);

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrueWithLock();
        Carteira carteira = carteiraService.buscarCarteira(usuarioId);

        validarCarteiraAtiva(carteira);

        distribuicaoService.aplicarRendimentoFavos(carteira);

        BigDecimal taxaTotal = valorReal.multiply(new BigDecimal("0.10"));
        BigDecimal valorLiquido = valorReal.subtract(taxaTotal);

        BigDecimal valorParaLastro = valorReal.multiply(new BigDecimal("0.05"));
        BigDecimal valorFavos = valorReal.multiply(new BigDecimal("0.0401"));

        BigDecimal indiceAtual = indice.getValor();
        BigDecimal liquidez = indice.getLiquidezTotal();
        BigDecimal totalEcm = obterTotalEcm();

        BigDecimal tokensComprados = valorLiquido.divide(indiceAtual, 18, RoundingMode.DOWN);
        tokensComprados = CalculoFinanceiroUtils.formatarEcm(tokensComprados);

        liquidez = liquidez.add(valorLiquido).add(valorParaLastro);
        indice.setLiquidezTotal(liquidez);

        carteira.creditarEcomel(tokensComprados);

        BigDecimal novoTotalEcm = totalEcm.add(tokensComprados);

        if (novoTotalEcm.compareTo(BigDecimal.ZERO) > 0) {
            indice.setValor(liquidez.divide(novoTotalEcm, 18, RoundingMode.DOWN));
        }

        BigDecimal favosBase = valorFavos.divide(indice.getValor(), 18, RoundingMode.DOWN);

        BigDecimal totalFavos = carteiraRepository.sumTotalFavos();

        if (totalFavos.compareTo(BigDecimal.ZERO) > 0) {
            distribuicaoService.distribuirTaxaFavos(favosBase);
        } else {
            indice.setLiquidezTotal(indice.getLiquidezTotal().add(valorFavos));
        }

        salvarTransacao(carteira, valorReal, valorLiquido, taxaTotal, TipoTransacao.DEPOSITO, requestKey);

        indiceRepository.save(indice);
        carteiraRepository.save(carteira);

        log.info("Depósito realizado. Usuario: {}, Valor: {}", usuarioId, valorReal);
    }

    @Transactional
    public void processarSaque(Long usuarioId, BigDecimal valorReal, String requestKey) {

        verificarIdempotencia(requestKey);

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrueWithLock();
        Carteira carteira = carteiraService.buscarCarteira(usuarioId);

        validarCarteiraAtiva(carteira);

        distribuicaoService.aplicarRendimentoFavos(carteira);

        BigDecimal indiceAtual = indice.getValor();

        BigDecimal tokensNecessarios = valorReal.divide(indiceAtual, 18, RoundingMode.DOWN);
        tokensNecessarios = CalculoFinanceiroUtils.formatarEcm(tokensNecessarios);

        if (!carteira.temSaldoEcomelSuficiente(tokensNecessarios)) {
            throw new BusinessException("Saldo insuficiente para saque.");
        }

        BigDecimal taxaTotal = valorReal.multiply(new BigDecimal("0.10"));
        BigDecimal valorLiquido = valorReal.subtract(taxaTotal);

        BigDecimal valorParaLastro = valorReal.multiply(new BigDecimal("0.05"));
        BigDecimal valorFavos = valorReal.multiply(new BigDecimal("0.0401"));

        BigDecimal liquidez = indice.getLiquidezTotal();
        BigDecimal totalEcm = obterTotalEcm();

        carteira.debitarEcomel(tokensNecessarios);

        liquidez = liquidez.subtract(valorReal).add(valorParaLastro);

        if (liquidez.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Liquidez insuficiente.");
        }

        indice.setLiquidezTotal(liquidez);

        BigDecimal novoTotalEcm = totalEcm.subtract(tokensNecessarios);

        if (novoTotalEcm.compareTo(BigDecimal.ZERO) > 0) {
            indice.setValor(liquidez.divide(novoTotalEcm, 18, RoundingMode.DOWN));
        } else {
            indice.setValor(BigDecimal.ONE);
        }

        BigDecimal favosBase = valorFavos.divide(indice.getValor(), 18, RoundingMode.DOWN);

        BigDecimal totalFavos = carteiraRepository.sumTotalFavos();

        if (totalFavos.compareTo(BigDecimal.ZERO) > 0) {
            distribuicaoService.distribuirTaxaFavos(favosBase);
        } else {
            indice.setLiquidezTotal(indice.getLiquidezTotal().add(valorFavos));
        }

        salvarTransacao(carteira, valorReal, valorLiquido, taxaTotal, TipoTransacao.SAQUE, requestKey);

        indiceRepository.save(indice);
        carteiraRepository.save(carteira);

        log.info("Saque realizado. Usuario: {}, Valor: {}", usuarioId, valorReal);
    }

    @Transactional
    public void transferirInterno(TransferenciaRequest request, String requestKey) {

        verificarIdempotencia(requestKey);

        if (request.valorReal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor inválido.");
        }

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrueWithLock();
        
        Carteira origem = carteiraRepository.findByUsuarioId(request.usuarioOrigemId())
                .orElseThrow(() -> new BusinessException("Carteira origem inválida."));

        Carteira destino = carteiraRepository.findByUsuarioId(request.usuarioDestinoId())
                .orElseThrow(() -> new BusinessException("Carteira destino inválida."));
        
        validarCarteiraAtiva(origem);
        validarCarteiraAtiva(destino);

        if (origem.getId().equals(destino.getId())) {
            throw new BusinessException("Transferência inválida.");
        }

        BigDecimal valorToken = request.valorReal()
                .divide(indice.getValor(), 18, RoundingMode.DOWN);

        valorToken = CalculoFinanceiroUtils.formatarEcm(valorToken);

        if (!origem.temSaldoEcomelSuficiente(valorToken)) {
            throw new BusinessException("Saldo insuficiente.");
        }

        origem.debitarEcomel(valorToken);
        destino.creditarEcomel(valorToken);

        salvarTransacaoInterna(origem, destino, request.valorReal(), requestKey);

        carteiraRepository.save(origem);
        carteiraRepository.save(destino);

        log.info("Transferência realizada: {} -> {}", origem.getId(), destino.getId());
    }

    public List<TransacaoResponse> listarTransacoesPorUsuario(Long usuarioId) {
        return transacaoRepository.findByCarteiraUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(t -> new TransacaoResponse(
                        t.getId(),
                        t.getTipo(),
                        t.getValorBruto(),
                        t.getValorLiquido(),
                        t.getTaxaTotal(),
                        t.getStatus(),
                        t.getCriadoEm(),
                        t.getCarteiraDestino() != null ? t.getCarteiraDestino().getCodigoCarteira() : null
                )).toList();
    }

    // =========================
    // 🔧 MÉTODOS AUXILIARES
    // =========================

    private void verificarIdempotencia(String requestKey) {
        if (requestKey != null && transacaoRepository.existsByRequestKey(requestKey)) {
            throw new BusinessException("Transação já processada.");
        }
    }

    private void validarCarteiraAtiva(Carteira carteira) {
        if (carteira == null || !carteira.isAtivo()) {
            throw new BusinessException("Carteira inválida.");
        }
    }

    private BigDecimal obterTotalEcm() {
        BigDecimal total = carteiraRepository.somarTotalEcomelAtivo();
        return (total == null || total.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ONE
                : total;
    }


    private void salvarTransacao(Carteira c, BigDecimal bruto, BigDecimal liq,
                                BigDecimal taxa, TipoTransacao tipo, String requestKey) {

        Transacao t = new Transacao();
        t.setCarteira(c);
        t.setTipo(tipo);
        t.setValorBruto(bruto);
        t.setValorLiquido(liq);
        t.setTaxaTotal(taxa);
        t.setStatus(StatusTransacao.CONCLUIDA);
        t.setRequestKey(requestKey);

        transacaoRepository.save(t);
    }

    private void salvarTransacaoInterna(Carteira o, Carteira d,
                                        BigDecimal valor, String requestKey) {

        Transacao t = new Transacao();
        t.setCarteira(o);
        t.setCarteiraDestino(d);
        t.setTipo(TipoTransacao.TRANSFERENCIA_INTERNA);
        t.setValorBruto(valor);
        t.setValorLiquido(valor);
        t.setTaxaTotal(BigDecimal.ZERO);
        t.setStatus(StatusTransacao.CONCLUIDA);
        t.setRequestKey(requestKey);

        transacaoRepository.save(t);
    }
}
