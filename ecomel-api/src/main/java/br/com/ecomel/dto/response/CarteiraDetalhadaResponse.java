package br.com.ecomel.dto.response;

import java.util.List;

/**
 * DTO para Consulta Completa.
 * Combina o resumo de saldos com a listagem de ordens ativas do usuário.
 */
public record CarteiraDetalhadaResponse(
    CarteiraResponse resumo,                // Saldos (Base, Real, Favos, Bloqueados)
    List<OrdemFavoResponse> minhasOrdens    // Lista de ordens ABERTAS ou PARCIAIS do usuário
) {
    // Construtor explícito para clareza
    public CarteiraDetalhadaResponse(CarteiraResponse resumo, List<OrdemFavoResponse> minhasOrdens) {
        this.resumo = resumo;
        this.minhasOrdens = minhasOrdens;
    }
}