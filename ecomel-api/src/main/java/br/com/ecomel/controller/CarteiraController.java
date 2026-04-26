package br.com.ecomel.controller;

import br.com.ecomel.dto.response.CarteiraResponse;
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

@RestController
@RequestMapping("/api/v1/carteiras")
@RequiredArgsConstructor
@Tag(name = "Carteiras", description = "Consulta de saldos e extratos")
@SecurityRequirement(name = "bearerAuth") // Exige JWT no Swagger
public class CarteiraController {

    private final CarteiraService service;

    @GetMapping("/{usuarioId}")
    @Operation(summary = "Consultar saldo", description = "Retorna o saldo base e o saldo real (valorizado) do usuário")
    public ResponseEntity<CarteiraResponse> obterSaldo(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obterExtratoPorUsuario(usuarioId));
    }
}