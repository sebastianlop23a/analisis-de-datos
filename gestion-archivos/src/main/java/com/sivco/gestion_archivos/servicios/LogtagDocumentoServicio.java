package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.LogtagDocumento;
import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.repositorios.LogtagDocumentoRepositorio;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * Servicio para gestionar documentos Logtag
 * Procesa PDFs con formato diferente a sensores estándar
 */
@Service
public class LogtagDocumentoServicio {
    
    private static final Logger logger = LoggerFactory.getLogger(LogtagDocumentoServicio.class);
    
    @Autowired
    private LogtagDocumentoRepositorio repositorio;
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @Autowired
    private CargaDatosServicio cargaDatosServicio;
    
    /**
     * Categorías disponibles
     */
    public static final String CATEGORIA_LOGTAG = "LOGTAG";
    public static final String CATEGORIA_SENSORES = "SENSORES";
    
    /**
     * Subir un documento logtag y procesarlo como datos de ensayo
     */
    @Transactional
        public LogtagDocumento subirDocumento(
            MultipartFile archivo,
            Long ensayoId,
            String categoriaSeleccionada,
            String descripcion,
            String subidoPor,
            String lote) throws IOException {
        
        logger.info("Subiendo documento: {} - Categoría: {} - Ensayo: {}", 
                archivo.getOriginalFilename(), categoriaSeleccionada, ensayoId);
        
        // Extraer texto del PDF para análisis
        String contenidoTexto = null;
        Set<String> sensoresDetectados = new HashSet<>();
        int datosProcesados = 0;

        // Crear y guardar entidad documento primero para poder marcar los datos con su id
        LogtagDocumento documento = new LogtagDocumento();
        documento.setNombreArchivo(archivo.getOriginalFilename());
        documento.setTipoDocumento(obtenerTipoDesdeNombre(archivo.getOriginalFilename()));
        documento.setCategoria(categoriaSeleccionada);
        documento.setFechaSubida(LocalDateTime.now());
        documento.setDescripcion(descripcion);
        documento.setSubidoPor(subidoPor);
        documento.setProcesado(false);
        documento.setTamanioBytes(archivo.getSize());
        documento.setLote(lote);
        if (ensayoId != null) {
            Optional<Ensayo> ensayoOpt = ensayoServicio.obtenerEnsayo(ensayoId);
            ensayoOpt.ifPresent(documento::setEnsayo);
        }
        // Guardar documento inicial (marcado no procesado)
        documento = repositorio.save(documento);
        
        if (archivo.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            try {
                contenidoTexto = extraerTextoPDF(archivo);
                sensoresDetectados = detectarSensoresEnTexto(contenidoTexto);
                // Intentar detectar sensor también desde el nombre del archivo (ej: "sensor 1.pdf")
                try {
                    String sensorFromName = detectarSensorDesdeNombreArchivo(archivo.getOriginalFilename());
                    if (sensorFromName != null && !sensorFromName.isEmpty()) {
                        sensoresDetectados.add(sensorFromName);
                        logger.info("Sensor detectado desde nombre de archivo: {}", sensorFromName);
                    }
                } catch (Exception e) {
                    logger.debug("No se pudo detectar sensor desde el nombre del archivo: {}", e.getMessage());
                }
                logger.info("Sensores detectados en documento: {}", sensoresDetectados);
                
                // Si es LOGTAG y tiene ensayo, procesar los datos
                    if (CATEGORIA_LOGTAG.equals(categoriaSeleccionada) && ensayoId != null) {
                    logger.info("Procesando datos del PDF logtag para ensayo: {}", ensayoId);
                    try {
                        // Usar el mismo método de carga de PDF pero para formato logtag
                            List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosPdfSivcoLogger(archivo, ensayoId);
                            if (datos != null && !datos.isEmpty()) {
                                datosProcesados = datos.size();
                                // Marcar cada dato con referencia al documento para poder filtrarlo después
                                for (DatoEnsayoTemporal d : datos) {
                                    d.setFuente("DOC:" + documento.getId() + "|" + documento.getNombreArchivo());
                                }
                                // Guardar los datos en el ensayo
                                cargaDatosServicio.guardarDatosTemporalesBatch(ensayoId, datos);
                                logger.info("Datos procesados y guardados: {} registros", datosProcesados);
                            }
                    } catch (Exception e) {
                        logger.warn("No se pudieron procesar los datos del PDF: {}", e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                logger.warn("No se pudo extraer texto del PDF: {}", e.getMessage());
            }
        }
        else if (archivo.getOriginalFilename().toLowerCase().endsWith(".xlsx") ||
                 archivo.getOriginalFilename().toLowerCase().endsWith(".xls") ||
                 archivo.getOriginalFilename().toLowerCase().endsWith(".xlsm")) {
            // Procesar Excel: delegar a CargaDatosServicio (nuevo soporte Excel)
            try {
                List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatos(archivo, ensayoId, new HashSet<>());
                if (datos != null && !datos.isEmpty()) {
                    datosProcesados = datos.size();
                    // extraer sensores detectados desde los datos
                    for (DatoEnsayoTemporal d : datos) {
                        if (d.getSensor() != null && !d.getSensor().isEmpty()) {
                            sensoresDetectados.add(d.getSensor().toLowerCase());
                        }
                        // Marcar fuente con documento
                        d.setFuente("DOC:" + documento.getId() + "|" + documento.getNombreArchivo());
                    }
                    // Intentar detectar sensor también desde el nombre del archivo (ej: "sensor 1.xlsx")
                    try {
                        String sensorFromName = detectarSensorDesdeNombreArchivo(archivo.getOriginalFilename());
                        if (sensorFromName != null && !sensorFromName.isEmpty()) {
                            sensoresDetectados.add(sensorFromName);
                            logger.info("Sensor detectado desde nombre de archivo (Excel): {}", sensorFromName);
                        }
                    } catch (Exception e) {
                        logger.debug("No se pudo detectar sensor desde el nombre del archivo Excel: {}", e.getMessage());
                    }
                    logger.info("Sensores detectados desde Excel: {}", sensoresDetectados);
                    // guardar datos
                    cargaDatosServicio.guardarDatosTemporalesBatch(ensayoId, datos);
                    logger.info("Datos EXCEL procesados y guardados: {} registros", datosProcesados);
                }
            } catch (Exception e) {
                logger.warn("No se pudieron procesar los datos del Excel: {}", e.getMessage());
            }
        }
        
        // Determinar categoría final basada en verificación
        String categoriaFinal = categoriaSeleccionada;
        String mensajeVerificacion = "";
        
        if (CATEGORIA_SENSORES.equals(categoriaSeleccionada)) {
            if (sensoresDetectados.isEmpty()) {
                categoriaFinal = CATEGORIA_LOGTAG;
                mensajeVerificacion = "ADVERTENCIA: No se detectaron sensores. Se categorizó como LOGTAG.";
            }
        } else if (CATEGORIA_LOGTAG.equals(categoriaSeleccionada)) {
            if (!sensoresDetectados.isEmpty()) {
                mensajeVerificacion = "NOTA: Se detectaron sensores: " + sensoresDetectados;
            }
        }
        
        // Agregar info de procesamiento al mensaje
        if (datosProcesados > 0) {
            mensajeVerificacion += " | Datos procesados: " + datosProcesados;
        }

        // Actualizar documento con datos finales
        documento.setCategoria(categoriaFinal);
        documento.setContenidoTexto(contenidoTexto);
        documento.setNotas(mensajeVerificacion);
        documento.setSensoresDetectados(sensoresDetectados.isEmpty() ? null : String.join(",", sensoresDetectados));
        documento.setProcesado(true);

        return repositorio.save(documento);
    }
    
    /**
     * Extraer texto de un PDF
     */
    private String extraerTextoPDF(MultipartFile archivo) throws IOException {
        try (PDDocument document = PDDocument.load(archivo.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Detectar sensores en el texto del PDF
     * Busca patrones como t1, t2, t12, sensor_1, etc.
     */
    private Set<String> detectarSensoresEnTexto(String texto) {
        Set<String> sensores = new HashSet<>();
        
        if (texto == null || texto.isEmpty()) {
            return sensores;
        }
        
        // Patrones comunes de sensores en PDFs SIVCO
        // t1, t2, ... t12, T1, T2, ... T12
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?i)(t\\d{1,2}|sensor[_\\s]?\\d+|s\\d{1,2})",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = pattern.matcher(texto);
        while (matcher.find()) {
            String sensor = matcher.group().toLowerCase();
            sensores.add(sensor);
        }
        
        return sensores;
    }

    /**
     * Detecta un sensor dentro del nombre del archivo (p. ej. "sensor 1", "t2", "T3").
     */
    private String detectarSensorDesdeNombreArchivo(String nombre) {
        if (nombre == null) return null;
        String lower = nombre.toLowerCase();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)(t\\d{1,2}|sensor[_\\s]?\\d+|s\\d{1,2})");
        java.util.regex.Matcher matcher = pattern.matcher(lower);
        if (matcher.find()) {
            String raw = matcher.group();
            // Normalizar: sensor 1 -> sensor_1, t2 -> sensor_2
            raw = raw.replaceAll("\\s+", "_");
            if (raw.matches("(?i)t\\d+|s\\d+")) {
                // extraer número y formatear
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(raw);
                if (m.find()) {
                    return "sensor_" + m.group(1);
                }
            }
            return raw;
        }
        return null;
    }
    
    /**
     * Obtener tipo de documento desde el nombre
     */
    private String obtenerTipoDesdeNombre(String nombre) {
        if (nombre == null) return "DESCONOCIDO";
        String lower = nombre.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".csv")) return "CSV";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "EXCEL";
        if (lower.endsWith(".txt")) return "TXT";
        return "DESCONOCIDO";
    }
    
    // === CRUD Básico ===
    
    public List<LogtagDocumento> listarTodos() {
        return repositorio.findAll();
    }

    /**
     * Obtener datos temporales asociados a un documento (buscando por marcador en campo 'fuente')
     */
    public List<com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal> obtenerDatosPorDocumento(Long ensayoId, Long documentoId) {
        String marcador = "DOC:" + documentoId;
        return cargaDatosServicio.obtenerDatosPorEnsayoYFuente(ensayoId, marcador);
    }
    
    public Optional<LogtagDocumento> buscarPorId(Long id) {
        return repositorio.findById(id);
    }
    
    public List<LogtagDocumento> listarPorEnsayo(Long ensayoId) {
        return repositorio.findByEnsayoId(ensayoId);
    }
    
    public List<LogtagDocumento> listarPorCategoria(String categoria) {
        return repositorio.findByCategoria(categoria);
    }
    
    public List<LogtagDocumento> listarLogtags() {
        return repositorio.findByCategoria(CATEGORIA_LOGTAG);
    }
    
    public List<LogtagDocumento> listarSensores() {
        return repositorio.findByCategoria(CATEGORIA_SENSORES);
    }
    
    @Transactional
    public void eliminar(Long id) {
        repositorio.deleteById(id);
    }
    
    public long contarPorCategoria(String categoria) {
        return repositorio.countByCategoria(categoria);
    }
    
    public long contarLogtags() {
        return repositorio.countByCategoria(CATEGORIA_LOGTAG);
    }
    
    public long contarSensores() {
        return repositorio.countByCategoria(CATEGORIA_SENSORES);
    }
}