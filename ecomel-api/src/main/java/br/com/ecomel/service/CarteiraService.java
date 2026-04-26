package br.com.ecomel.service;

import br.com.ecomel.domain.entity.Carteira;
import br.com.ecomel.domain.entity.IndiceGlobal;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.repository.CarteiraRepository;
import br.com.ecomel.repository.IndiceGlobalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final IndiceGlobalRepository indiceRepository;

    @Transactional(readOnly = true)
    public CarteiraResponse obterExtratoPorUsuario(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId);
        IndiceGlobal indice = indiceRepository.findFirstByAtivoTrue();

        return new CarteiraResponse(
            usuarioId,
            carteira.getSaldoBase(),
            carteira.getSaldoReal(indice.getValor()),
            carteira.getSaldoFavos(),
            indice.getValor()
        );
    }
}