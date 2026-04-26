package br.com.ecomel.controller;

import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.dto.response.OrdemFavoResponse;
import br.com.ecomel.repository.OrdemFavoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mercado-favos")
@RequiredArgsConstructor
@Tag(name = "Mercado de FAVOS")
public class MercadoFavoController {

    private final OrdemFavoRepository ordemRepository;

    @GetMapping("/book/vendas")
    @Operation(summary = "Ver quem está vendendo", description = "Lista ordens de venda abertas")
    public ResponseEntity<List<OrdemFavoResponse>> listarOfertasVenda() {
        List<OrdemFavoResponse> ofertas = ordemRepository.findByTipoAndStatusOrderByPrecoUnitarioAsc(TipoOrdem.VENDA, StatusOrdem.ABERTA)
                .stream()
                .map(ordem -> new OrdemFavoResponse(
                        ordem.getId(),
                        ordem.getCarteira().getCodigoEndereco(),
                        ordem.getTipo(),
                        ordem.getQuantidadeOriginal(),
                        ordem.getQuantidadeRestante(),
                        ordem.getPrecoUnitario(),
                        ordem.getStatus(),
                        ordem.getCriadoEm()
                )).toList();
        
        return ResponseEntity.ok(ofertas);
    }

    @GetMapping("/book/compras")
    @Operation(summary = "Ver quem quer comprar", description = "Lista ordens de compra abertas")
    public ResponseEntity<List<OrdemFavoResponse>> listarOfertasCompra() {
        List<OrdemFavoResponse> ofertas = ordemRepository.findByTipoAndStatusOrderByPrecoUnitarioDesc(TipoOrdem.COMPRA, StatusOrdem.ABERTA)
                .stream()
                .map(ordem -> new OrdemFavoResponse(
                        ordem.getId(),
                        ordem.getCarteira().getCodigoEndereco(),
                        ordem.getTipo(),
                        ordem.getQuantidadeOriginal(),
                        ordem.getQuantidadeRestante(),
                        ordem.getPrecoUnitario(),
                        ordem.getStatus(),
                        ordem.getCriadoEm()
                )).toList();
        
        return ResponseEntity.ok(ofertas);
    }
}
