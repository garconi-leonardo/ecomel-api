package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.dto.request.OrdemFavoRequest;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.OrdemFavoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class OrdemFavoService {

    private final OrdemFavoRepository repository;
    private final CarteiraRepository carteiraRepository;
    private final MatchingEngineService matchingEngine;

    @Transactional
    public void criarEProcessarOrdem(OrdemFavoRequest request) {
        Carteira carteira = carteiraRepository.findByUsuarioId(request.usuarioId());
        
        // 1. Validar e Reservar Saldo
        if (request.tipo() == TipoOrdem.VENDA) {
            if (carteira.getSaldoFavos().compareTo(request.quantidade()) < 0) {
                throw new BusinessException("Saldo de FAVOS insuficiente para venda.");
            }
            // Retira do disponível (fica 'preso' na ordem)
            carteira.setSaldoFavos(carteira.getSaldoFavos().subtract(request.quantidade()));
        } else {
            // Compra: Reserva ECM (Base). Valor = Qtd * Preço
            BigDecimal valorNecessarioEcm = request.quantidade().multiply(request.precoUnitario());
            if (carteira.getSaldoBase().compareTo(valorNecessarioEcm) < 0) {
                throw new BusinessException("Saldo ECM insuficiente para compra.");
            }
            carteira.setSaldoBase(carteira.getSaldoBase().subtract(valorNecessarioEcm));
        }

        // 2. Criar Entidade
        OrdemFavo ordem = new OrdemFavo();
        ordem.setCarteira(carteira);
        ordem.setTipo(request.tipo());
        ordem.setQuantidadeOriginal(request.quantidade());
        ordem.setQuantidadeRestante(request.quantidade());
        ordem.setPrecoUnitario(request.precoUnitario());
        ordem.setStatus(StatusOrdem.ABERTA);

        repository.save(ordem);
        carteiraRepository.save(carteira);

        // 3. Disparar Matching Engine
        matchingEngine.processarMatch(ordem);
    }
    
    @Transactional
    public void cancelarOrdem(Long usuarioId, Long ordemId) {
        OrdemFavo ordem = repository.findById(ordemId)
                .orElseThrow(() -> new BusinessException("Ordem não encontrada."));

        // 1. Validar se a ordem pertence ao usuário
        if (!ordem.getCarteira().getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para cancelar esta ordem.");
        }

        // 2. Validar se a ordem ainda está passível de cancelamento
        if (ordem.getStatus() == StatusOrdem.EXECUTADA || ordem.getStatus() == StatusOrdem.CANCELADA) {
            throw new BusinessException("Esta ordem já foi finalizada ou cancelada.");
        }

        Carteira carteira = ordem.getCarteira();

        // 3. Devolver Saldos Bloqueados
        if (ordem.getTipo() == TipoOrdem.VENDA) {
            // Devolve a quantidade de FAVOS que ainda não foi vendida
            carteira.setSaldoFavos(carteira.getSaldoFavos().add(ordem.getQuantidadeRestante()));
        } else {
            // Devolve o montante em ECM (Quantidade * Preço) que ainda não foi usado
            BigDecimal valorDevolucaoEcm = ordem.getQuantidadeRestante().multiply(ordem.getPrecoUnitario());
            carteira.setSaldoBase(carteira.getSaldoBase().add(valorDevolucaoEcm));
        }

        // 4. Atualizar Status
        ordem.setStatus(StatusOrdem.CANCELADA);
        
        repository.save(ordem);
        carteiraRepository.save(carteira);
    }

}