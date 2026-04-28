package br.com.ecomel.controller;

import br.com.ecomel.dto.request.LoginRequest;
import br.com.ecomel.dto.response.JwtAuthenticationResponse;
import br.com.ecomel.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoint para login e geração de Token JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Operation(summary = "Realizar login", description = "Retorna um token JWT caso as credenciais sejam válidas")
    @PostMapping("/login")
    public ResponseEntity<?> autenticar(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(), // Agora vindo do campo unificado
                        request.senha()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.gerarToken(authentication);
        
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

}