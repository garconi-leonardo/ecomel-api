package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import br.com.ecomel.util.CalculoFinanceiroUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final IndiceGlobalRepository indiceRepository;

    // =====================================================
    // 🔥 EXTRATO POR CÓDIGO DA CARTEIRA (PADRÃO CRIPTO)
    // =====================================================

    @Transactional
    public CarteiraResponse obterExtratoPorCodigoCarteira(String codigoCarteira) {

        Carteira carteira = carteiraRepository
                .findByCodigoCarteira(codigoCarteira)
                .orElseThrow(() -> new BusinessException("Carteira não encontrada."));

        if (!carteira.isAtivo()) {
            throw new BusinessException("Carteira inativa.");
        }

        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();
        BigDecimal indiceAtual = indice.getValor();

        // =========================================
        // 🔥 SINCRONIZA DIVIDENDOS (ANTES DE TUDO)
        // =========================================
        sincronizarDividendos(carteira, indice);

        // =========================================
        // 🔥 SALDOS
        // =========================================

        BigDecimal tokenTotal = carteira.getTokenEcomel();
        BigDecimal tokenDisponivel = carteira.getSaldoEcomelDisponivel();

        BigDecimal saldoFavosTotal = carteira.getSaldoFavos();
        BigDecimal saldoFavosDisponivel = carteira.getSaldoFavosDisponivel();

        // 🔥 VALOR REAL (ECM * índice)
        BigDecimal saldoReal = tokenTotal.multiply(indiceAtual)
                .setScale(8, RoundingMode.DOWN);

        // =========================================
        // 🔥 BLOQUEIOS (ORDENS)
        // =========================================

        BigDecimal favosEmOrdem = carteira.getSaldoFavosBloqueado();
        BigDecimal ecmEmOrdem = carteira.getTokenEcomelBloqueado();

        // =========================================
        // 🔥 RESPONSE FINAL
        // =========================================

        return new CarteiraResponse(
                carteira.getCodigoCarteira(),
                tokenTotal,
                tokenDisponivel,
                saldoReal,
                saldoFavosTotal,
                saldoFavosDisponivel,
                favosEmOrdem,
                ecmEmOrdem,
                indiceAtual
        );
    }

    // =====================================================
    // 🔥 EXTRATO POR USUÁRIO (USO INTERNO)
    // =====================================================

    @Transactional(readOnly = true)
    public CarteiraResponse obterExtratoPorUsuario(Long usuarioId) {

    	Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
    	        .orElseThrow(() -> new BusinessException("Carteira não encontrada."));

        if (!carteira.isAtivo()) {
            throw new BusinessException("Carteira não encontrada ou inativa.");
        }

        return obterExtratoPorCodigoCarteira(carteira.getCodigoCarteira());
    }

    // =====================================================
    // 🔥 DIVIDENDOS (FAVOS → ECM)
    // =====================================================

    /**
     * 🔥 APLICA RENDIMENTO DE FAVOS (DIVIDENDOS PASSIVOS)
     *
     * Fórmula:
     * lucro = (indiceGlobal - ultimoIndiceUsuario) * saldoFavos
     */
    private void sincronizarDividendos(Carteira carteira, IndiceGlobal indiceGlobal) {

        BigDecimal indiceAtual = indiceGlobal.getIndiceFavoAcumulado();
        BigDecimal ultimoIndiceUsuario = carteira.getUltimoIndiceFavo();

        if (indiceAtual.compareTo(ultimoIndiceUsuario) > 0 &&
            carteira.getSaldoFavos().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal diferencaIndice = indiceAtual.subtract(ultimoIndiceUsuario);

            BigDecimal dividendosEcm = carteira.getSaldoFavos()
                    .multiply(diferencaIndice)
                    .setScale(18, RoundingMode.DOWN);

            BigDecimal dividendosToken = CalculoFinanceiroUtils.formatarEcm(dividendosEcm);

            carteira.setTokenEcomel(
                    carteira.getTokenEcomel().add(dividendosToken)
            );
        }

        // 🔥 checkpoint SEMPRE atualizado
        carteira.setUltimoIndiceFavo(indiceAtual);
    }
    
    public Carteira buscarCarteira(Long usuarioId) {
        return carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Carteira não encontrada."));

    }
}
