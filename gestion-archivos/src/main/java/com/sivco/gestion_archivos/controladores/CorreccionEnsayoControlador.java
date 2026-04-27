package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.CorreccionEnsayo;
import com.sivco.gestion_archivos.servicios.CorreccionEnsayoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/correcciones")
@CrossOrigin(origins = "*")
public class CorreccionEnsayoControlador {
    
    @Autowired
    private CorreccionEnsayoServicio correccionServicio;
    
    /**
     * Subir archivo de correcciones para un ensayo
     * POST /api/correcciones/ensayo/{ensayoId}
     */
    @PostMapping("/ensayo/{ensayoId}")
    public ResponseEntity<CorreccionEnsayo> subirCorreccion(
            @PathVariable Long ensayoId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "subidoPor", required = false, defaultValue = "Sistema") String subidoPor) {
        // Deprecated: uploading corrections per-ensayo is disallowed. Use sensor-level recalibration endpoint:
        // POST /api/sensores/{sensorId}/recalibrar
        return ResponseEntity.status(org.springframework.http.HttpStatus.GONE).build();
    }
    
    /**
     * Obtener todas las correcciones de un ensayo
     * GET /api/correcciones/ensayo/{ensayoId}
     */
    @GetMapping("/ensayo/{ensayoId}")
    public ResponseEntity<List<CorreccionEnsayo>> obtenerCorreccionesPorEnsayo(@PathVariable Long ensayoId) {
        // Deprecated: test-level corrections no longer used. Return empty list.
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }
    
    /**
     * Obtener corrección por ID
     * GET /api/correcciones/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CorreccionEnsayo> obtenerCorreccion(@PathVariable Long id) {
        return correccionServicio.obtenerCorreccion(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Descargar archivo de corrección
     * GET /api/correcciones/{id}/download
     */
    @GetMapping("/{id}/download")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id) {
        try {
            CorreccionEnsayo correccion = correccionServicio.obtenerCorreccion(id)
                .orElse(null);
            
            if (correccion == null) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] contenido = correccionServicio.descargarArchivo(id);
            org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + correccion.getNombreArchivo() + "\"")
                .contentType(mediaType)
                .body(contenido);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Eliminar corrección
     * DELETE /api/correcciones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCorreccion(@PathVariable Long id) {
        try {
            correccionServicio.eliminarCorreccion(id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtener correcciones por tipo
     * GET /api/correcciones/tipo/{tipo}
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<CorreccionEnsayo>> obtenerCorreccionesPorTipo(@PathVariable String tipo) {
        List<CorreccionEnsayo> correcciones = correccionServicio.obtenerCorreccionesPorTipo(tipo.toUpperCase());
        return ResponseEntity.ok(correcciones);
    }
}
