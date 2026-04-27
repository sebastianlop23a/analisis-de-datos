package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.ReporteFinal;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import com.sivco.gestion_archivos.utilidades.GeneradorReporteFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/utilidades")
@CrossOrigin(origins = "*")
public class UtilidadesControlador {
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @Autowired
    private GeneradorReporteFinal generadorReporteFinal;
    
    @GetMapping("/reporte-completo/{ensayoId}")
    public ResponseEntity<?> obtenerReporteCompleto(@PathVariable Long ensayoId) {
        try {
            return ensayoServicio.obtenerEnsayo(ensayoId)
                .map(ensayo -> {
                    ReporteFinal reporte = generadorReporteFinal.construirReporte(ensayoId, ensayo);
                    return ResponseEntity.ok(reporte);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/salud")
    public ResponseEntity<Map<String, String>> salud() {
        return ResponseEntity.ok(Map.of(
            "estado", "OK",
            "mensaje", "Sistema de gestión de ensayos operativo",
            "version", "1.0.0"
        ));
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> obtenerInfo() {
        return ResponseEntity.ok(Map.of(
            "nombre", "Sistema de Gestión de Ensayos de Máquinas",
            "version", "1.0.0",
            "descripcion", "Gestiona ensayos, datos temporales, análisis y reportes",
            "desarrollado", "SIVCO",
            "endpoints", Map.of(
                "maquinas", "/api/maquinas",
                "ensayos", "/api/ensayos",
                "reportes", "/api/reportes",
                "analisis", "/api/analisis",
                "carga", "/api/carga",
                "exportar", "/api/exportar"
            )
        ));
    }
}
