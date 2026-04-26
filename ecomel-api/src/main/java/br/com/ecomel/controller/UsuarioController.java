package br.com.ecomel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.dto.request.UsuarioRequest;
import br.com.ecomel.dto.response.UsuarioResponse;
import br.com.ecomel.service.CarteiraService;
import br.com.ecomel.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@PathVariable Long id, @RequestBody @Valid UsuarioRequest request) {
        service.editar(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar Usuário", description = "Desativa a conta apenas se os saldos forem zero")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping
    @Operation(summary = "Cadastrar usuário", description = "Cria um usuário e sua carteira AAA000 vinculada")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid UsuarioRequest request) {
        // O service agora retorna o UsuarioResponse completo (com o objeto carteira dentro)
        UsuarioResponse response = service.salvar(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
