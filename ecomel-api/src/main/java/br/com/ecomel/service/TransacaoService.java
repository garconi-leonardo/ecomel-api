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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import para logs
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j // Habilita o objeto 'log'
@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final IndiceGlobalRepository indiceRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoRepository transacaoRepository;
    private final DistribuicaoService distribuicaoService;

    @Transactional
    public void processarDeposito(Long usuarioId, BigDecimal valorReal, String requestKey) {
        try {
            verificarIdempotencia(requestKey, TipoTransacao.DEPOSITO, valorReal);

            IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
            Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

            BigDecimal taxaTotalReal = valorReal.multiply(new BigDecimal("0.10"));
            BigDecimal valorLiquidoReal = valorReal.subtract(taxaTotalReal);

            BigDecimal fatorCrescimento = BigDecimal.ONE.add(new BigDecimal("0.05"));
            indice.setValor(indice.getValor().multiply(fatorCrescimento).setScale(18, RoundingMode.DOWN));

            BigDecimal incrementoBase = valorLiquidoReal.divide(indice.getValor(), 18, RoundingMode.DOWN);
            carteira.setSaldoBase(carteira.getSaldoBase().add(incrementoBase));

            BigDecimal montanteFavosBase = valorReal.multiply(new BigDecimal("0.0401"))
                                                   .divide(indice.getValor(), 18, RoundingMode.DOWN);
            distribuicaoService.distribuirTaxaFavos(montanteFavosBase);

            salvarTransacao(carteira, valorReal, valorLiquidoReal, taxaTotalReal, TipoTransacao.DEPOSITO, requestKey);

            indiceRepository.save(indice);
            carteiraRepository.save(carteira);
            
            log.info("Depósito processado com sucesso. Usuario: {}, Valor: {}", usuarioId, valorReal);
        } catch (Exception e) {
            log.error("Erro ao processar depósito para o usuário {}: {}", usuarioId, e.getMessage());
            throw e; // Relançar é fundamental para o Rollback do @Transactional
        }
    }

    @Transactional
    public void processarSaque(Long usuarioId, BigDecimal valorSaqueReal, String requestKey) {
        try {
            verificarIdempotencia(requestKey, TipoTransacao.SAQUE, valorSaqueReal);

            IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
            Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

            if (carteira.getSaldoReal(indice.getValor()).compareTo(valorSaqueReal) < 0) {
                throw new BusinessException("Saldo insuficiente para saque.");
            }

            BigDecimal taxaTotalReal = valorSaqueReal.multiply(new BigDecimal("0.10"));
            
            BigDecimal fatorCrescimento = BigDecimal.ONE.add(new BigDecimal("0.05"));
            indice.setValor(indice.getValor().multiply(fatorCrescimento).setScale(18, RoundingMode.DOWN));

            BigDecimal debitoBase = valorSaqueReal.divide(indice.getValor(), 18, RoundingMode.DOWN);
            carteira.setSaldoBase(carteira.getSaldoBase().subtract(debitoBase));

            BigDecimal montanteFavosBase = valorSaqueReal.multiply(new BigDecimal("0.0401"))
                                                   .divide(indice.getValor(), 18, RoundingMode.DOWN);
            distribuicaoService.distribuirTaxaFavos(montanteFavosBase);

            salvarTransacao(carteira, valorSaqueReal, valorSaqueReal.subtract(taxaTotalReal), taxaTotalReal, TipoTransacao.SAQUE, requestKey);

            indiceRepository.save(indice);
            carteiraRepository.save(carteira);
            
            log.info("Saque processado com sucesso. Usuario: {}, Valor: {}", usuarioId, valorSaqueReal);
        } catch (Exception e) {
            log.error("Erro ao processar saque para o usuário {}: {}", usuarioId, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void transferirInterno(TransferenciaRequest request, String requestKey) {
        try {
            verificarIdempotencia(requestKey, TipoTransacao.TRANSFERENCIA_INTERNA, request.valorReal());

            IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
            Carteira origem = carteiraRepository.findByUsuarioId(request.usuarioOrigemId());
            Carteira destino = carteiraRepository.findByCodigoEndereco(request.codigoDestino())
                    .orElseThrow(() -> new BusinessException("Carteira destino não encontrada."));

            BigDecimal valorBase = request.valorReal().divide(indice.getValor(), 18, RoundingMode.DOWN);

            if (origem.getSaldoBase().compareTo(valorBase) < 0) {
                throw new BusinessException("Saldo insuficiente para transferência.");
            }

            origem.setSaldoBase(origem.getSaldoBase().subtract(valorBase));
            destino.setSaldoBase(destino.getSaldoBase().add(valorBase));

            salvarTransacaoInterna(origem, destino, request.valorReal(), requestKey);

            carteiraRepository.save(origem);
            carteiraRepository.save(destino);
            
            log.info("Transferência interna realizada: {} -> {}, Valor Real: {}", 
                     request.usuarioOrigemId(), request.codigoDestino(), request.valorReal());
        } catch (Exception e) {
            log.error("Erro na transferência interna: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listarTransacoesPorUsuario(Long usuarioId) {
        try {
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
                            t.getCarteiraDestino() != null ? t.getCarteiraDestino().getCodigoEndereco() : null
                    )).toList();
        } catch (Exception e) {
            log.error("Erro ao listar transações para o usuário {}: {}", usuarioId, e.getMessage());
            throw e;
        }
    }

    /**
     * Garante idempotência por (requestKey + tipoTransacao + valorBruto).
     *
     * Objetivo: impedir que múltiplos cliques no mesmo botão criem duplicatas,
     * mas SEM bloquear operações sequenciais legítimas:
     *  - Depósito de R$500 + Saque de R$200 (tipos diferentes) => OK
     *  - Saque de R$100 + Saque de R$200 (mesmo tipo, valores diferentes) => OK
     *  - Depósito de R$500 + Depósito de R$500 com mesma chave => BLOQUEADO (duplo clique)
     */
    private void verificarIdempotencia(String requestKey, TipoTransacao tipo, BigDecimal valor) {
        if (requestKey != null
                && transacaoRepository.existsByRequestKeyAndTipoAndValorBruto(requestKey, tipo, valor)) {
            log.warn("Duplicidade detectada (múltiplos cliques) - RequestKey: {} | Tipo: {} | Valor: {}",
                    requestKey, tipo, valor);
            throw new BusinessException(
                    "Transação já processada (operação idêntica detectada — possível múltiplo clique).");
        }
    }

    private void salvarTransacao(Carteira c, BigDecimal bruto, BigDecimal liq, BigDecimal taxa, TipoTransacao tipo, String requestKey) {
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

    private void salvarTransacaoInterna(Carteira o, Carteira d, BigDecimal valor, String requestKey) {
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
