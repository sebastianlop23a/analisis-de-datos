package com.sivco.gestion_archivos.exceptiones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorExcepciones {
    
    private static final Logger logger = LoggerFactory.getLogger(ManejadorExcepciones.class);
    
    @ExceptionHandler(EnsayoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarEnsayoNoEncontrado(EnsayoNoEncontradoException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("tipo", "EnsayoNoEncontrado");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(MaquinaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> manejarMaquinaNoEncontrada(MaquinaNoEncontradaException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("tipo", "MaquinaNoEncontrada");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoIlegal(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("tipo", "ArgumentoIlegal");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(ConstraintViolationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error de validación: " + ex.getMessage());
        error.put("tipo", "ErrorValidacion");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarExcepcionGeneral(Exception ex) {
        logger.error("Error interno del servidor no manejado", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error interno del servidor: " + ex.getMessage());
        error.put("tipo", "ErrorInterno");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
