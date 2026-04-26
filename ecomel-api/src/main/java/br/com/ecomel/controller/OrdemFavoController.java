package br.com.ecomel.controller;

import br.com.ecomel.dto.request.OrdemFavoRequest;
import br.com.ecomel.service.OrdemFavoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ordens-favos")
@RequiredArgsConstructor
@Tag(name = "Ordens de FAVOS", description = "Endpoint para compra e venda de ativos participativos")
public class OrdemFavoController {

    private final OrdemFavoService ordemFavoService;

    @PostMapping
    @Operation(summary = "Criar nova ordem", description = "Envia uma ordem de compra ou venda para o Matching Engine")
    public ResponseEntity<Void> criarOrdem(@RequestBody @Valid OrdemFavoRequest request) {
        ordemFavoService.criarEProcessarOrdem(request);
        return ResponseEntity.ok().build();
    }
}
