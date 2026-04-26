package br.com.ecomel.service;

import br.com.ecomel.domain.entity.OrdemFavo;
import br.com.ecomel.dto.request.OrdemFavoRequest;
import br.com.ecomel.repository.OrdemFavoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrdemFavoService {

    private final OrdemFavoRepository repository;
    private final MatchingEngineService matchingEngine;

    @Transactional
    public void criarEProcessarOrdem(OrdemFavoRequest request) {
        // 1. Converter DTO para Entity (idealmente usar MapStruct aqui)
        OrdemFavo novaOrdem = new OrdemFavo();
        novaOrdem.setTipo(request.tipo());
        novaOrdem.setQuantidadeOriginal(request.quantidade());
        novaOrdem.setQuantidadeRestante(request.quantidade());
        novaOrdem.setPrecoUnitario(request.precoUnitario());
        
        // 2. Persistir Ordem
        repository.save(novaOrdem);

        // 3. Chamar o motor de cruzamento
        matchingEngine.processarMatch(novaOrdem);
    }
}
