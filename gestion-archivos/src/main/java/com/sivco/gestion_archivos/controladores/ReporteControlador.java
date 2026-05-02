package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.Reporte;
import com.sivco.gestion_archivos.modelos.TipoReporte;
import com.sivco.gestion_archivos.servicios.ReporteServicio;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteControlador {
    private static final Logger logger = LoggerFactory.getLogger(ReporteControlador.class);
    
    @Autowired
    private ReporteServicio reporteServicio;
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @Autowired
    private com.sivco.gestion_archivos.utilidades.GeneradorReporteFinal generadorReporteFinal;
    
    @PostMapping("/generar/{ensayoId}")
    public ResponseEntity<?> generarReporte(
            @PathVariable Long ensayoId,
            @RequestBody Map<String, String> datos) {
        try {
            String tipo = datos.getOrDefault("tipo", "PDF");
            String generadoPor = datos.getOrDefault("generadoPor", "SISTEMA");
            
            // Finalizar ensayo si está en progreso
            ensayoServicio.finalizarEnsayo(ensayoId);
            
            // Generar reporte
            Reporte reporte = reporteServicio.generarReporte(
                ensayoId,
                TipoReporte.valueOf(tipo),
                generadoPor
            );
            
            return ResponseEntity.ok(reporte);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Tipo de reporte inválido"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Compatibilidad con frontend: permitir POST a /api/reportes con cuerpo { ensayoId, tipo, generadoPor }
    @PostMapping
    public ResponseEntity<?> generarReporteBody(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("ensayoId")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Falta ensayoId en el cuerpo"));
            }

            Long ensayoId = Long.parseLong(body.get("ensayoId").toString());
            String tipo = body.getOrDefault("tipo", "PDF").toString();
            String generadoPor = body.getOrDefault("generadoPor", "SISTEMA").toString();

            // Finalizar ensayo si está en progreso
            ensayoServicio.finalizarEnsayo(ensayoId);

            Reporte reporte = reporteServicio.generarReporte(
                ensayoId,
                TipoReporte.valueOf(tipo),
                generadoPor
            );

            return ResponseEntity.ok(reporte);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de reporte inválido"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/ensayo/{ensayoId}")
    public ResponseEntity<?> obtenerReporte(@PathVariable Long ensayoId) {
        try {
            return ResponseEntity.ok(reporteServicio.obtenerReporte(ensayoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // List all reports (lightweight summaries) for frontend
    @GetMapping("")
    public ResponseEntity<?> listarReportes() {
        try {
            var reportes = reporteServicio.obtenerTodosLosReportes();
            return ResponseEntity.ok(reportes);
        } catch (Exception e) {
            logger.error("Error listing reports: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudieron listar los reportes"));
        }
    }

    // Download the generated report as an HTML file attachment
    @GetMapping("/{ensayoId}/download")
    public ResponseEntity<byte[]> descargarReporte(@PathVariable Long ensayoId) {
        try {
            logger.info("Download HTML request for ensayoId={}", ensayoId);
            Reporte reporte = reporteServicio.obtenerReporte(ensayoId);
            String contenido = reporte.getContenido();
            if (contenido == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new byte[0]);
            }

            byte[] data = contenido.getBytes(StandardCharsets.UTF_8);

            // Obtener nombre del ensayo para el archivo
            com.sivco.gestion_archivos.modelos.Ensayo ensayo = reporte.getEnsayo();
            String nombreArchivo = ensayo.getNombre().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-_]", "")
                                                      .replaceAll("\\s+", "_")
                                                      .trim();
            if (nombreArchivo.isEmpty()) {
                nombreArchivo = "ensayo_" + ensayoId;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(nombreArchivo + ".html")
                    .build();
            headers.setContentDisposition(cd);

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.warn("HTML report not found for ensayoId={}", ensayoId);
            return ResponseEntity.notFound().build();
        }
    }

    // Download report as PDF (generate from stored HTML or generate if missing)
    // Also supports inline viewing if Accept header includes text/html
    @GetMapping("/{ensayoId}/download/pdf")
    public ResponseEntity<byte[]> descargarReportePdf(@PathVariable Long ensayoId) {
        logger.info("PDF generation disabled request for ensayoId={}", ensayoId);
        String msg = "Generación de PDF deshabilitada en esta instancia.";
        byte[] payload = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(payload, headers, HttpStatus.GONE);
    }
}
