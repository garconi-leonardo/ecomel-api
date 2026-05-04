package br.com.ecomel.controller;

import br.com.ecomel.domain.enums.StatusOrdem;
import br.com.ecomel.domain.enums.TipoOrdem;
import br.com.ecomel.dto.response.BookResponse;
import br.com.ecomel.dto.response.OrdemFavoResponse;
import br.com.ecomel.repository.OrdemFavoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mercado-favos")
@RequiredArgsConstructor
@Tag(name = "Mercado de FAVOS")
public class MercadoFavoController {

    private final OrdemFavoRepository repository;

    @GetMapping("/book")
    @Operation(summary = "Livro de ofertas")
    public ResponseEntity<BookResponse<OrdemFavoResponse>> getBook(
            @RequestParam TipoOrdem tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Page<OrdemFavoResponse> result = repository
                .findByTipoAndStatus(tipo, StatusOrdem.ABERTA, PageRequest.of(page, size))
                .map(ordem -> new OrdemFavoResponse(
                        ordem.getId(),
                        ordem.getCarteira().getCodigoCarteira(),
                        ordem.getTipo(),
                        ordem.getQuantidadeOriginal(),
                        ordem.getQuantidadeRestante(),
                        ordem.getPrecoUnitario(),
                        ordem.getStatus(),
                        ordem.getCriadoEm()
                ));

        return ResponseEntity.ok(new BookResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        ));
    }
}
