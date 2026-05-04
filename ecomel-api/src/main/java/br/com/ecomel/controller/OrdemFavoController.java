package br.com.ecomel.controller;

import br.com.ecomel.dto.request.OrdemFavoRequest;
import br.com.ecomel.dto.response.OrdemFavoResponse;
import br.com.ecomel.service.OrdemFavoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de FAVOS", description = "Execução de ordens de compra e venda")
public class OrdemFavoController {

    private final OrdemFavoService service;

    /**
     * 🔥 Criar ordem (COMPRA ou VENDA)
     */
    @PostMapping
    @Operation(summary = "Criar ordem", description = "Envia uma ordem para o matching engine")
    public ResponseEntity<Void> criarOrdem(
            @RequestBody @Valid OrdemFavoRequest request
    ) {
        service.criarEProcessarOrdem(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 🔥 Cancelar ordem
     */
    @DeleteMapping("/{ordemId}")
    @Operation(summary = "Cancelar ordem", description = "Cancela uma ordem aberta")
    public ResponseEntity<Void> cancelarOrdem(
            @PathVariable Long ordemId,
            @RequestParam Long usuarioId
    ) {
        service.cancelarOrdem(usuarioId, ordemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔥 Minhas ordens (abertas + parciais)
     */
    @GetMapping("/minhas")
    @Operation(summary = "Listar minhas ordens")
    public ResponseEntity<List<OrdemFavoResponse>> listarMinhasOrdens(
            @RequestParam Long usuarioId
    ) {
        return ResponseEntity.ok(service.listarOrdensPorCarteira(usuarioId));
    }
}
