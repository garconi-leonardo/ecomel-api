package br.com.ecomel.controller;

import br.com.ecomel.dto.response.CarteiraDetalhadaResponse;
import br.com.ecomel.dto.response.CarteiraResponse;
import br.com.ecomel.dto.response.OrdemFavoResponse;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.service.CarteiraService;
import br.com.ecomel.service.OrdemFavoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/carteiras")
@RequiredArgsConstructor
@Tag(name = "Carteiras", description = "Consulta de saldos e extratos")
@SecurityRequirement(name = "bearerAuth")
public class CarteiraController {

    private final CarteiraService carteiraService;
    private final OrdemFavoService ordemFavoService;

    // =====================================================
    // 🔐 CONSULTA COMPLETA (USUÁRIO LOGADO)
    // =====================================================

    @GetMapping("/me/detalhado/{usuarioId}")
    @Operation(
        summary = "Consulta completa da carteira",
        description = "Retorna saldos + ordens do usuário autenticado"
    )
    public ResponseEntity<CarteiraDetalhadaResponse> obterCompleto(@PathVariable Long usuarioId) {

        // 🔹 1. Extrato da carteira (via ID)
        CarteiraResponse resumo = carteiraService
                .obterExtratoPorUsuario(usuarioId);

        // 🔹 2. Ordens da carteira (via ID)
        List<OrdemFavoResponse> ordens = ordemFavoService
                .listarOrdensPorUsuario(usuarioId);

        // 🔹 3. Resposta final
        return ResponseEntity.ok(
                new CarteiraDetalhadaResponse(resumo, ordens)
        );
    }

    // =====================================================
    // 🔍 CONSULTA POR ID (ADMIN / INTERNA)
    // =====================================================

    @GetMapping("/{usuarioId}")
    @Operation(
        summary = "Consulta de carteira por ID",
        description = "Retorna o extrato da carteira pelo ID do usuário"
    )
    public ResponseEntity<CarteiraResponse> obterPorUsuarioId(
            @PathVariable Long usuarioId) {

        CarteiraResponse response = carteiraService
                .obterExtratoPorUsuario(usuarioId);

        return ResponseEntity.ok(response);
    }
}
