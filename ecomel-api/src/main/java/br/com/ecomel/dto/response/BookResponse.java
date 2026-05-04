package br.com.ecomel.dto.response;

import java.util.List;

public record BookResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
