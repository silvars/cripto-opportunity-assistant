package com.btcassistant.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

/**
 * Único ponto de tradução de erros de domínio para respostas HTTP (SDD §76 — validação de entrada
 * em todos os endpoints). Primeira fase com endpoints mutáveis (POST); fases anteriores só tinham
 * leitura e não precisavam disso.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("Arquivo excede o tamanho máximo permitido"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("Requisição inválida", details));
    }

    public record ApiErrorResponse(Instant timestamp, String message, List<String> details) {

        static ApiErrorResponse of(String message) {
            return new ApiErrorResponse(Instant.now(), message, List.of());
        }

        static ApiErrorResponse of(String message, List<String> details) {
            return new ApiErrorResponse(Instant.now(), message, details);
        }
    }
}
