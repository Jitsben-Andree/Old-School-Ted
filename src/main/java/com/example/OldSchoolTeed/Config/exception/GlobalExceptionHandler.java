package com.example.OldSchoolTeed.Config.exception;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // apturar errores de "No encontrado" (404)

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {

        // Enviamos a Sentry
        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("tipo_error", "not_found");
            Sentry.captureException(ex);
        });

        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false));
    }

    // Capturar Credenciales Incorrectas (Login) - (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex, WebRequest request) {


        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.ERROR); // <--- Rojo chillón
            scope.setTag("tipo_error", "login_fail");
            // Agregamos el usuario al mensaje para diferenciarlo visualmente
            scope.setContexts("usuario", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonimo");
            Sentry.captureException(ex);
        });

        log.warn("Intento de login fallido: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", request.getDescription(false));
    }

    // Capturar errores de validación (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.INFO);
            scope.setTag("tipo_error", "validacion");
            Sentry.captureException(ex);
        });

        log.warn("Error de validación: {}", ex.getBindingResult().getTarget());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de Validación");
        body.put("detalles", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    //  Capturar errores generales Críticos (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {

        // Reportar a Sentry como ERROR
        Sentry.captureException(ex);

        log.error("ERROR CRÍTICO NO CONTROLADO: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno inesperado", request.getDescription(false));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return new ResponseEntity<>(body, status);
    }
}