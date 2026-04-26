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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final IndiceGlobalRepository indiceRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoRepository transacaoRepository;
    private final DistribuicaoService distribuicaoService;

    @Transactional
    public void processarDeposito(Long usuarioId, BigDecimal valorReal) {
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

        BigDecimal taxaTotalReal = valorReal.multiply(new BigDecimal("0.10"));
        BigDecimal valorLiquidoReal = valorReal.subtract(taxaTotalReal);

        // Valorização do Índice (5%)
        BigDecimal fatorCrescimento = BigDecimal.ONE.add(new BigDecimal("0.05"));
        indice.setValor(indice.getValor().multiply(fatorCrescimento));

        // Crédito no Saldo Base
        BigDecimal incrementoBase = valorLiquidoReal.divide(indice.getValor(), 18, RoundingMode.HALF_UP);
        carteira.setSaldoBase(carteira.getSaldoBase().add(incrementoBase));

        // Distribuição FAVOS (4,01% do bruto convertido em base)
        BigDecimal montanteFavosBase = valorReal.multiply(new BigDecimal("0.0401"))
                                               .divide(indice.getValor(), 18, RoundingMode.HALF_UP);
        distribuicaoService.distribuirTaxaFavos(montanteFavosBase);

        salvarTransacao(carteira, valorReal, valorLiquidoReal, taxaTotalReal, TipoTransacao.DEPOSITO);

        indiceRepository.save(indice);
        carteiraRepository.save(carteira);
    }

    @Transactional
    public void processarSaque(Long usuarioId, BigDecimal valorSaqueReal) {
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);

        // Validação de saldo real
        if (carteira.getSaldoReal(indice.getValor()).compareTo(valorSaqueReal) < 0) {
            throw new BusinessException("Saldo insuficiente para saque.");
        }

        BigDecimal taxaTotalReal = valorSaqueReal.multiply(new BigDecimal("0.10"));
        
        // Valorização do Índice (5% também no saque por ser transação externa)
        BigDecimal fatorCrescimento = BigDecimal.ONE.add(new BigDecimal("0.05"));
        indice.setValor(indice.getValor().multiply(fatorCrescimento));

        // Débito no Saldo Base (Retira o valor bruto do saque do usuário)
        BigDecimal debitoBase = valorSaqueReal.divide(indice.getValor(), 18, RoundingMode.HALF_UP);
        carteira.setSaldoBase(carteira.getSaldoBase().subtract(debitoBase));

        // Distribuição FAVOS (4,01% do saque convertido em base)
        BigDecimal montanteFavosBase = valorSaqueReal.multiply(new BigDecimal("0.0401"))
                                               .divide(indice.getValor(), 18, RoundingMode.HALF_UP);
        distribuicaoService.distribuirTaxaFavos(montanteFavosBase);

        salvarTransacao(carteira, valorSaqueReal, valorSaqueReal.subtract(taxaTotalReal), taxaTotalReal, TipoTransacao.SAQUE);

        indiceRepository.save(indice);
        carteiraRepository.save(carteira);
    }

    @Transactional
    public void transferirInterno(TransferenciaRequest request) {
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        Carteira origem = carteiraRepository.findByUsuarioId(request.usuarioOrigemId());
        Carteira destino = carteiraRepository.findByCodigoEndereco(request.codigoDestino())
                .orElseThrow(() -> new BusinessException("Carteira destino não encontrada."));

        BigDecimal valorBase = request.valorReal().divide(indice.getValor(), 18, RoundingMode.HALF_UP);

        if (origem.getSaldoBase().compareTo(valorBase) < 0) {
            throw new BusinessException("Saldo insuficiente para transferência.");
        }

        origem.setSaldoBase(origem.getSaldoBase().subtract(valorBase));
        destino.setSaldoBase(destino.getSaldoBase().add(valorBase));

        salvarTransacaoInterna(origem, destino, request.valorReal());

        carteiraRepository.save(origem);
        carteiraRepository.save(destino);
    }

    @Transactional(readOnly = true)
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
                        t.getCarteiraDestino() != null ? t.getCarteiraDestino().getCodigoEndereco() : null
                )).toList();
    }

    private void salvarTransacao(Carteira c, BigDecimal bruto, BigDecimal liq, BigDecimal taxa, TipoTransacao tipo) {
        Transacao t = new Transacao();
        t.setCarteira(c);
        t.setTipo(tipo);
        t.setValorBruto(bruto);
        t.setValorLiquido(liq);
        t.setTaxaTotal(taxa);
        t.setStatus(StatusTransacao.CONCLUIDA);
        transacaoRepository.save(t);
    }

    private void salvarTransacaoInterna(Carteira o, Carteira d, BigDecimal valor) {
        Transacao t = new Transacao();
        t.setCarteira(o);
        t.setCarteiraDestino(d);
        t.setTipo(TipoTransacao.TRANSFERENCIA_INTERNA);
        t.setValorBruto(valor);
        t.setValorLiquido(valor);
        t.setTaxaTotal(BigDecimal.ZERO);
        t.setStatus(StatusTransacao.CONCLUIDA);
        transacaoRepository.save(t);
    }
}
