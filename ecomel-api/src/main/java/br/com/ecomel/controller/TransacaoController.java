package br.com.ecomel.controller;

import br.com.ecomel.dto.request.DepositoRequest;
import br.com.ecomel.dto.request.SaqueRequest;
import br.com.ecomel.dto.request.TransferenciaRequest;
import br.com.ecomel.dto.response.TransacaoResponse;
import br.com.ecomel.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transacoes")
@RequiredArgsConstructor
@Tag(name = "Transações", description = "Operações de Depósito, Saque e Transferência")
@SecurityRequirement(name = "bearerAuth")
public class TransacaoController {

    private final TransacaoService service;

    @PostMapping("/deposito")
    @Operation(summary = "Realizar depósito", description = "Injeta capital e valoriza o Índice Global em 5%")
    public ResponseEntity<Void> depositar(@RequestBody @Valid DepositoRequest request) {
        // Adicionado o parâmetro requestKey para idempotência
        service.processarDeposito(request.usuarioId(), request.valor(), request.requestKey());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/saque")
    @Operation(summary = "Realizar saque", description = "Retira capital e valoriza o Índice Global em 5%")
    public ResponseEntity<Void> sacar(@RequestBody @Valid SaqueRequest request) {
        // Corrigido para SaqueRequest e adicionado requestKey
        service.processarSaque(request.usuarioId(), request.valor(), request.requestKey());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/extrato/{usuarioId}")
    @Operation(summary = "Obter extrato completo", description = "Lista depósitos, saques e transferências")
    public ResponseEntity<List<TransacaoResponse>> obterExtrato(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarTransacoesPorUsuario(usuarioId));
    }
    
    @PostMapping("/transferencia")
    @Operation(summary = "Transferência interna", description = "Envia ECM para outra carteira usando o código AAA111")
    public ResponseEntity<Void> transferir(@RequestBody @Valid TransferenciaRequest request) {
        // Adicionado o parâmetro requestKey para idempotência
        service.transferirInterno(request, request.requestKey());
        return ResponseEntity.ok().build();
    }
}
