package br.com.ecomel.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Trata erros de lógica de negócio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        logger.error("Erro de negócio: {}", ex.getMessage()); // Log de aviso
        
        ApiError error = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            LocalDateTime.now(),
            null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Trata erros de validação (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.toList());

        logger.error("Erro de validação: {}", errors);

        ApiError error = new ApiError(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Erro de validação nos campos: " + ex.getMessage(), // getMessage() incluído aqui
            LocalDateTime.now(),
            errors
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    // Trata erros genéricos (Runtime)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        // O segundo parâmetro 'ex' no logger.error imprime o Stack Trace completo no console
        logger.error("Erro interno não esperado: ", ex); 

        ApiError error = new ApiError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Erro interno: " + ex.getMessage(), // getMessage() incluído aqui
            LocalDateTime.now(),
            null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }


	 @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	 public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
	     Map<String, Object> body = new HashMap<>();
	     body.put("timestamp", LocalDateTime.now());
	     body.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
	     body.put("error", "Método HTTP não suportado");
	     body.put("message", ex.getMessage());
	     
	     return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
	 }

}
