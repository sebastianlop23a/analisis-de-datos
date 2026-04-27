package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.servicios.CargaDatosServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carga")
@CrossOrigin(origins = "*")
public class CargaDatosControlador {
    
    private static final Logger logger = LoggerFactory.getLogger(CargaDatosControlador.class);
    
    @Autowired
    private CargaDatosServicio cargaDatosServicio;
    
    @PostMapping("/csv/{ensayoId}")
    public ResponseEntity<?> cargarCSV(
            @PathVariable Long ensayoId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "pdf", required = false) MultipartFile pdf) {
        try {
            logger.info("Iniciando carga de CSV para ensayo: " + ensayoId);
            
            if (archivo.isEmpty()) {
                logger.warn("Archivo vacío");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
            }
            
            String filename = archivo.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                logger.warn("Archivo no es CSV: " + filename);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo debe ser CSV"));
            }
            
            java.util.Set<String> sensoresDetectados = java.util.Collections.emptySet();
            if (pdf != null && !pdf.isEmpty()) {
                try {
                    sensoresDetectados = cargaDatosServicio.detectarSensoresEnPDF(pdf, ensayoId);
                } catch (Exception e) {
                    logger.warn("No se pudieron detectar sensores en PDF adjunto: " + e.getMessage());
                }
            }

            List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosCSV(archivo, ensayoId, sensoresDetectados);
            logger.info("CSV cargado exitosamente: " + datos.size() + " registros");
            
            cargaDatosServicio.guardarDatosTemporalesBatch(ensayoId, datos);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Datos cargados correctamente",
                "totalRegistros", datos.size()
            ));
        } catch (IOException e) {
            logger.error("Error de IO al cargar CSV", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al leer el archivo: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error general al cargar CSV", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error general: " + e.getMessage()));
        }
    }
    
    @PostMapping("/txt/{ensayoId}")
    public ResponseEntity<?> cargarTXT(
            @PathVariable Long ensayoId,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
            }
            
            String filename = archivo.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo debe ser TXT"));
            }
            
            List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosTxt(archivo, ensayoId);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Datos cargados correctamente",
                "totalRegistros", datos.size()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al leer el archivo: " + e.getMessage()));
        }
    }
    
    @PostMapping("/json/{ensayoId}")
    public ResponseEntity<?> cargarJSON(
            @PathVariable Long ensayoId,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
            }
            
            List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosJSON(archivo, ensayoId);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Datos cargados correctamente",
                "totalRegistros", datos.size()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al leer el archivo: " + e.getMessage()));
        }
    }

    @PostMapping("/pdf/{ensayoId}")
    public ResponseEntity<?> cargarPDF(
            @PathVariable Long ensayoId,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            logger.info("Iniciando método cargarPDF para ensayo: {}", ensayoId);
            
            if (archivo.isEmpty()) {
                logger.warn("Archivo vacío detectado");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
            }
            logger.info("Archivo no está vacío, tamaño: {} bytes", archivo.getSize());

            String filename = archivo.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                logger.warn("Archivo no es PDF: {}", filename);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo debe ser PDF"));
            }
            logger.info("Archivo es PDF válido: {}", filename);

            logger.info("Cargando PDF SIVCO-LOGGER para ensayo: {}", ensayoId);
            
            // Verificar que el servicio esté inyectado
            if (cargaDatosServicio == null) {
                logger.error("cargaDatosServicio es null - problema de inyección de dependencias");
                return ResponseEntity.status(500)
                    .body(Map.of("error", "Error interno del servidor: servicio no disponible"));
            }
            logger.info("Servicio cargaDatosServicio está disponible");
            
            // Cargar datos del PDF SIVCO-LOGGER
            logger.info("Llamando a cargaDatosServicio.cargarDatosPdfSivcoLogger");
            List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosPdfSivcoLogger(archivo, ensayoId);
            logger.info("Método cargarDatosPdfSivcoLogger completado, obtenidos {} registros", datos.size());
            
            // Guardar todos los datos en un solo lote
            logger.info("Guardando datos temporales en batch");
            cargaDatosServicio.guardarDatosTemporalesBatch(ensayoId, datos);
            logger.info("Datos guardados exitosamente");
            
            logger.info("PDF SIVCO-LOGGER cargado exitosamente: {} registros", datos.size());

            return ResponseEntity.ok(Map.of(
                "mensaje", "Datos PDF SIVCO-LOGGER cargados correctamente",
                "totalRegistros", datos.size(),
                "tipoArchivo", "PDF_SIVCO_LOGGER"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al procesar PDF", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            logger.error("Error al procesar PDF", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al procesar el PDF: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error general al procesar PDF", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error general: " + e.getMessage()));
        }
    }

    @PostMapping("/pdf/validar/{ensayoId}")
    public ResponseEntity<?> validarSensoresPDF(
            @PathVariable Long ensayoId,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
            }

            String filename = archivo.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo debe ser PDF"));
            }

            Map<String, Object> resultado = cargaDatosServicio.compararSensoresPdfConRegistrados(archivo);
            return ResponseEntity.ok(resultado);
        } catch (IOException e) {
            logger.error("Error al validar sensores PDF", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al validar el PDF: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error general al validar sensores PDF", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error general: " + e.getMessage()));
        }
    }
}
