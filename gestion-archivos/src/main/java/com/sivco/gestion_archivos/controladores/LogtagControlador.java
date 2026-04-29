package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.LogtagDocumento;
import com.sivco.gestion_archivos.servicios.LogtagDocumentoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para documentos Logtag
 */
@RestController
@RequestMapping("/api/logtag")
@CrossOrigin(origins = "*")
public class LogtagControlador {
    
    private static final Logger logger = LoggerFactory.getLogger(LogtagControlador.class);
    
    @Autowired
    private LogtagDocumentoServicio servicio;
    
    /**
     * POST /api/logtag - Subir documento con categoría
     * 
     * Parámetros:
     * - archivo: El archivo a subir (requerido)
     * - ensayoId: ID del ensayo (opcional)
     * - categoria: LOGTAG o SENSORES (requerido)
     * - descripcion: Descripción del documento (opcional)
     * - subidoPor: Nombre de quien sube (opcional)
     */
    @PostMapping
    public ResponseEntity<?> subirDocumento(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "ensayoId", required = false) Long ensayoId,
            @RequestParam("categoria") String categoria,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "subidoPor", required = false) String subidoPor) {
        
        try {
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
            }
            
            // Validar categoría
            if (!categoria.equals(LogtagDocumentoServicio.CATEGORIA_LOGTAG) && 
                !categoria.equals(LogtagDocumentoServicio.CATEGORIA_SENSORES)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Categoría inválida. Use LOGTAG o SENSORES"));
            }
            
            LogtagDocumento documento = servicio.subirDocumento(
                archivo, 
                ensayoId, 
                categoria,
                descripcion, 
                subidoPor != null ? subidoPor : "Sistema"
            );
            
            logger.info("Documento subido exitosamente: ID={}, Categoría={}", 
                       documento.getId(), documento.getCategoria());
            
            return ResponseEntity.ok(documento);
            
        } catch (Exception e) {
            logger.error("Error al subir documento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/logtag - Listar todos los documentos
     */
    @GetMapping
    public ResponseEntity<List<LogtagDocumento>> listarTodos() {
        return ResponseEntity.ok(servicio.listarTodos());
    }
    
    /**
     * GET /api/logtag/categoria/{categoria} - Listar por categoría
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<?> listarPorCategoria(@PathVariable String categoria) {
        try {
            List<LogtagDocumento> documentos = servicio.listarPorCategoria(categoria);
            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/logtag/ensayo/{ensayoId} - Listar documentos de un ensayo
     */
    @GetMapping("/ensayo/{ensayoId}")
    public ResponseEntity<?> listarPorEnsayo(@PathVariable Long ensayoId) {
        try {
            List<LogtagDocumento> documentos = servicio.listarPorEnsayo(ensayoId);
            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/logtag/logtag - Listar solo documentos Logtag
     */
    @GetMapping("/logtag")
    public ResponseEntity<List<LogtagDocumento>> listarLogtags() {
        return ResponseEntity.ok(servicio.listarLogtags());
    }
    
    /**
     * GET /api/logtag/sensores - Listar solo documentos de sensores
     */
    @GetMapping("/sensores")
    public ResponseEntity<List<LogtagDocumento>> listarSensores() {
        return ResponseEntity.ok(servicio.listarSensores());
    }
    
    /**
     * GET /api/logtag/{id} - Obtener documento por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return servicio.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * DELETE /api/logtag/{id} - Eliminar documento
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            servicio.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Documento eliminado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/logtag/estadisticas - Obtener estadísticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Long>> obtenerEstadisticas() {
        return ResponseEntity.ok(Map.of(
            "total", (long) servicio.listarTodos().size(),
            "logtags", servicio.contarLogtags(),
            "sensores", servicio.contarSensores()
        ));
    }
}