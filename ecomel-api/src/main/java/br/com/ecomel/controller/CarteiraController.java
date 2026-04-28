package br.com.ecomel.controller;

import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.dto.response.CarteiraDetalhadaResponse;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.dto.response.OrdemFavoResponse;
import br.com.ecomel.repository.OrdemFavoRepository;
import br.com.ecomel.service.CarteiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/carteiras")
@RequiredArgsConstructor
@Tag(name = "Carteiras", description = "Consulta de saldos e extratos")
@SecurityRequirement(name = "bearerAuth") // Exige JWT no Swagger
public class CarteiraController {

    private final CarteiraService service;
    private final OrdemFavoRepository ordemRepository;

    @GetMapping("/{usuarioId}")
    @Operation(summary = "Consultar saldo", description = "Retorna o saldo base e o saldo real (valorizado) do usuário")
    public ResponseEntity<CarteiraResponse> obterSaldo(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obterExtratoPorUsuario(usuarioId));
    }
    
    @GetMapping("/{usuarioId}/resumo")
    @Operation(summary = "Consulta de Saldo Simples", description = "Retorna saldos e índice")
    public ResponseEntity<CarteiraResponse> obterResumo(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obterExtratoPorUsuario(usuarioId));
    }

    @GetMapping("/{usuarioId}/detalhado")
    @Operation(summary = "Consulta Completa", description = "Retorna saldos + lista de ordens P2P do usuário")
    public ResponseEntity<CarteiraDetalhadaResponse> obterCompleto(@PathVariable Long usuarioId) {
        // 1. Obtém o resumo de saldos e sincroniza dividendos através do Service
        CarteiraResponse resumo = service.obterExtratoPorUsuario(usuarioId);
        
        // 2. Busca ordens ativas e realiza o mapeamento para OrdemFavoResponse
        List<OrdemFavoResponse> minhasOrdens = ordemRepository.findByCarteiraUsuarioIdAndStatusIn(
            usuarioId, List.of(StatusOrdem.ABERTA, StatusOrdem.PARCIAL))
            .stream()
            .map(o -> new OrdemFavoResponse(
                o.getId(),
                o.getCarteira().getCodigoEndereco(),
                o.getTipo(),
                o.getQuantidadeOriginal(),
                o.getQuantidadeRestante(),
                o.getPrecoUnitario(),
                o.getStatus(),
                o.getCriadoEm()
            ))
            .toList();

        // 3. Retorna o DTO composto
        return ResponseEntity.ok(new CarteiraDetalhadaResponse(resumo, minhasOrdens));
    }
}
