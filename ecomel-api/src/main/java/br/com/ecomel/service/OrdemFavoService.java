package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.dto.request.OrdemFavoRequest;
import br.com.ecomel.dto.response.OrdemFavoResponse;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.OrdemFavoRepository;
import br.com.ecomel.util.CalculoFinanceiroUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdemFavoService {

    private final OrdemFavoRepository repository;
    private final CarteiraRepository carteiraRepository;
    private final MatchingEngineService matchingEngine;
    private final CarteiraService carteiraService;

    private static final int LIMITE_ORDENS = 50;

    // =========================================
    // 🔥 CRIAR ORDEM
    // =========================================
    @Transactional
    public OrdemFavoResponse criarEProcessarOrdem(OrdemFavoRequest request) {

        validarRequest(request);

        // 🔥 IDEMPOTÊNCIA
        if (repository.existsByRequestKey(request.requestKey())) {
            throw new BusinessException("Ordem já processada.");
        }

        Carteira carteira = carteiraService.buscarCarteira(request.usuarioId());

        validarLimiteOrdens(carteira.getId());

        OrdemFavo ordem = criarOrdem(request, carteira);

        bloquearSaldo(carteira, ordem);

        repository.save(ordem);
        carteiraRepository.save(carteira);

        matchingEngine.processarMatch(ordem);

        return toResponse(ordem);
    }

    // =========================================
    // ❌ CANCELAR ORDEM
    // =========================================
    @Transactional
    public void cancelarOrdem(Long usuarioId, Long ordemId) {

        Carteira carteira = carteiraService.buscarCarteira(usuarioId);
        OrdemFavo ordem = buscarOrdem(ordemId);

        validarOrdemPertenceCarteira(ordem, carteira);
        validarOrdemCancelavel(ordem);

        liberarSaldo(carteira, ordem);

        ordem.setStatus(StatusOrdem.CANCELADA);

        repository.save(ordem);
        carteiraRepository.save(carteira);
    }

    // =========================================
    // 📊 LISTAR ORDENS ABERTAS
    // =========================================
    @Transactional(readOnly = true)
    public List<OrdemFavoResponse> listarOrdensAbertas(long usuarioId) {
    	
        Carteira carteira = carteiraService.buscarCarteira(usuarioId);

        return repository
                .findByCarteiraIdAndStatusIn(
                        carteira.getId(),
                        List.of(StatusOrdem.ABERTA, StatusOrdem.PARCIALMENTE_EXECUTADA)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OrdemFavoResponse> listarOrdensPorCarteira(Long usuarioId) {

        Carteira carteira = carteiraService.buscarCarteira(usuarioId);

        List<OrdemFavo> ordens = repository.findByCarteiraIdAndStatusIn(
                carteira.getId(),
                List.of(
                        StatusOrdem.ABERTA,
                        StatusOrdem.PARCIALMENTE_EXECUTADA,
                        StatusOrdem.EXECUTADA,
                        StatusOrdem.CANCELADA
                )
        );

        return ordens.stream()
                .map(this::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OrdemFavoResponse> listarOrdensPorUsuario(Long usuarioId) {

        if (usuarioId == null) {
            throw new BusinessException("Usuário inválido.");
        }

        // 🔹 Busca apenas ordens relevantes (ativas)
        List<OrdemFavo> ordens = repository.findByCarteiraIdAndStatusIn(
                usuarioId,
                List.of(
                        StatusOrdem.ABERTA,
                        StatusOrdem.PARCIALMENTE_EXECUTADA,
                        StatusOrdem.EXECUTADA // opcional (histórico leve)
                )
        );

        // 🔹 Mapeamento seguro para DTO
        return ordens.stream()
                .map(o -> new OrdemFavoResponse(
                        o.getId(),
                        o.getCarteira().getCodigoCarteira(),
                        o.getTipo(),
                        o.getQuantidadeOriginal(),
                        o.getQuantidadeRestante(),
                        o.getPrecoUnitario(),
                        o.getStatus(),
                        o.getCriadoEm()
                ))
                .toList();
    }



    // =========================================
    // 📜 HISTÓRICO
    // =========================================
    @Transactional(readOnly = true)
    public List<OrdemFavoResponse> listarHistorico(Long usuarioId) {

        Carteira carteira = carteiraService.buscarCarteira(usuarioId);

        return repository
                .findByCarteiraIdAndStatusIn(
                        carteira.getId(),
                        List.of(StatusOrdem.EXECUTADA, StatusOrdem.CANCELADA)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================
    // 🔒 BLOQUEIO
    // =========================================
    private void bloquearSaldo(Carteira carteira, OrdemFavo ordem) {

        if (ordem.getTipo() == TipoOrdem.VENDA) {

            if (!carteira.temSaldoFavosSuficiente(ordem.getQuantidadeOriginal())) {
                throw new BusinessException("Saldo de FAVOS insuficiente.");
            }

            carteira.bloquearFavos(ordem.getQuantidadeOriginal());

        } else {

            BigDecimal valor = ordem.getQuantidadeOriginal()
                    .multiply(ordem.getPrecoUnitario());

            BigDecimal tokens = CalculoFinanceiroUtils.formatarEcm(valor);

            if (!carteira.temSaldoEcomelSuficiente(tokens)) {
                throw new BusinessException("Saldo ECM insuficiente.");
            }

            carteira.bloquearEcomel(tokens);
        }
    }

    // =========================================
    // 🔓 LIBERAÇÃO
    // =========================================
    private void liberarSaldo(Carteira carteira, OrdemFavo ordem) {

        if (ordem.getQuantidadeRestante().compareTo(BigDecimal.ZERO) <= 0) return;

        if (ordem.getTipo() == TipoOrdem.VENDA) {

            carteira.liberarFavos(ordem.getQuantidadeRestante());

        } else {

            BigDecimal valor = ordem.getQuantidadeRestante()
                    .multiply(ordem.getPrecoUnitario());

            BigDecimal tokens = CalculoFinanceiroUtils.formatarEcm(valor);

            carteira.liberarEcomel(tokens);
        }
    }

    // =========================================
    // 🧠 CRIAÇÃO DE ORDEM
    // =========================================
    private OrdemFavo criarOrdem(OrdemFavoRequest request, Carteira carteira) {

        OrdemFavo ordem = new OrdemFavo();
        ordem.setCarteira(carteira);
        ordem.setTipo(request.tipo());
        ordem.setQuantidadeOriginal(request.quantidade());
        ordem.setQuantidadeRestante(request.quantidade());
        ordem.setPrecoUnitario(request.precoUnitario());
        ordem.setStatus(StatusOrdem.ABERTA);
        ordem.setRequestKey(request.requestKey());

        return ordem;
    }

    // =========================================
    // 🔍 BUSCAS
    // =========================================

    private OrdemFavo buscarOrdem(Long ordemId) {
        return repository.findById(ordemId)
                .orElseThrow(() -> new BusinessException("Ordem não encontrada."));
    }

    // =========================================
    // 🔐 VALIDAÇÕES
    // =========================================
    private void validarRequest(OrdemFavoRequest request) {

        if (request.quantidade().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantidade inválida.");
        }

        if (request.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Preço inválido.");
        }
    }

    private void validarLimiteOrdens(Long carteiraId) {

        long abertas = repository.countByCarteiraIdAndStatusIn(
                carteiraId,
                List.of(StatusOrdem.ABERTA, StatusOrdem.PARCIALMENTE_EXECUTADA)
        );

        if (abertas >= LIMITE_ORDENS) {
            throw new BusinessException("Limite de ordens abertas atingido.");
        }
    }

    private void validarOrdemPertenceCarteira(OrdemFavo ordem, Carteira carteira) {
        if (!ordem.getCarteira().getId().equals(carteira.getId())) {
            throw new BusinessException("Ordem não pertence à carteira.");
        }
    }

    private void validarOrdemCancelavel(OrdemFavo ordem) {
        if (ordem.getStatus() == StatusOrdem.EXECUTADA ||
            ordem.getStatus() == StatusOrdem.CANCELADA) {
            throw new BusinessException("Ordem já finalizada.");
        }
    }

    // =========================================
    // 🔄 MAPPER
    // =========================================
    private OrdemFavoResponse toResponse(OrdemFavo ordem) {
        return new OrdemFavoResponse(
                ordem.getId(),
                ordem.getCarteira().getCodigoCarteira(),
                ordem.getTipo(),
                ordem.getQuantidadeOriginal(),
                ordem.getQuantidadeRestante(),
                ordem.getPrecoUnitario(),
                ordem.getStatus(),
                ordem.getCriadoEm()
        );
    }
}