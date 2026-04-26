package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import br.com.ecomel.repository.OrdemFavoRepository;
import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final IndiceGlobalRepository indiceRepository;
    private final OrdemFavoRepository ordemRepository;

    @Transactional(readOnly = true)
    public CarteiraResponse obterExtratoPorUsuario(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();

        // Cálculo de valores bloqueados em ordens abertas/parciais
        BigDecimal favosEmOrdem = ordemRepository.sumQuantidadeRestanteByCarteiraAndTipoAndStatus(
            carteira.getId(), TipoOrdem.VENDA, StatusOrdem.ABERTA);
            
        // No caso de compra, o que trava é o valor em ECM (Quantidade * Preço)
        BigDecimal ecmEmOrdem = ordemRepository.sumValorEcmBloqueado(
            carteira.getId(), TipoOrdem.COMPRA, StatusOrdem.ABERTA);

        return new CarteiraResponse(
            carteira.getCodigoEndereco(),
            carteira.getSaldoBase(),
            carteira.getSaldoReal(indice.getValor()),
            carteira.getSaldoFavos(),
            favosEmOrdem != null ? favosEmOrdem : BigDecimal.ZERO,
            ecmEmOrdem != null ? ecmEmOrdem : BigDecimal.ZERO,
            indice.getValor()
        );
    }
}
