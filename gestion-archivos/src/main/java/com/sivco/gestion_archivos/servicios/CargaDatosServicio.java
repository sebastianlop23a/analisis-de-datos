package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.CoeficientesCorreccion;
import com.sivco.gestion_archivos.modelos.CorreccionEnsayo;
import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.Maquina;
import com.sivco.gestion_archivos.modelos.CalibrationCorrection;
import com.sivco.gestion_archivos.servicios.calibration.CalibrationManagementService;
import com.sivco.gestion_archivos.servicios.SivcoLoggerPdfService;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import com.sivco.gestion_archivos.modelos.calibration.RegressionModel;
import com.sivco.gestion_archivos.modelos.calibration.RegressionModelType;

@Service
public class CargaDatosServicio {
    
    private static final Logger logger = LoggerFactory.getLogger(CargaDatosServicio.class);
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @Autowired
    private CalibrationCorrectionServicio calibrationServicio;
    
    @Autowired
    private CalibrationManagementService calibrationManagementService;
    
    @Autowired
    private SensorServicio sensorServicio;
    
    @Autowired
    private com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio calibrationCorrectionRepositorio;

    @Autowired
    private PdfParsingService pdfParsingService;
    
    @Autowired
    private SivcoLoggerPdfService sivcoLoggerPdfService;

    public void guardarDatosTemporalesBatch(Long ensayoId, List<DatoEnsayoTemporal> datos) {
        ensayoServicio.guardarDatosTemporalesBatch(ensayoId, datos);
    }
    
    /**
     * Método principal que detecta el tipo de archivo y carga los datos correspondientes
     */
    public List<DatoEnsayoTemporal> cargarDatos(MultipartFile archivo, Long ensayoId, Set<String> sensoresPermitidos) throws IOException {
        String filename = archivo.getOriginalFilename().toLowerCase();
        
        // Si es PDF, intentar cargar como SIVCO-LOGGER
        if (filename.endsWith(".pdf")) {
            logger.info("Detectado archivo PDF: {}", archivo.getOriginalFilename());
            try {
                return cargarDatosPdfSivcoLogger(archivo, ensayoId);
            } catch (Exception e) {
                logger.warn("No se pudo cargar como PDF SIVCO-LOGGER, integrando registro de error: {}", e.getMessage());
                throw new IOException("El archivo PDF no tiene el formato esperado de SIVCO-LOGGER: " + e.getMessage(), e);
            }
        }
        // Si es CSV, usar el método existente
        else if (filename.endsWith(".csv")) {
            logger.info("Detectado archivo CSV: {}", archivo.getOriginalFilename());
            return cargarDatosCSV(archivo, ensayoId, sensoresPermitidos);
        }
        // Si es Excel, usar POI para parsear
        else if (filename.endsWith(".xlsx") || filename.endsWith(".xls") || filename.endsWith(".xlsm")) {
            logger.info("Detectado archivo Excel: {}", archivo.getOriginalFilename());
            return cargarDatosExcel(archivo, ensayoId);
        }
        
        throw new IOException("Formato de archivo no soportado. Use CSV, Excel o PDF SIVCO-LOGGER");
    }

    /**
     * Cargar datos desde un archivo Excel (.xlsx o .xls).
     * Intentamos detectar columnas de fecha/hora y columnas numéricas de sensores.
     */
    public List<DatoEnsayoTemporal> cargarDatosExcel(MultipartFile archivo, Long ensayoId) throws IOException {
        logger.info("Iniciando carga de Excel para ensayo: {} - archivo: {}", ensayoId, archivo.getOriginalFilename());

        List<DatoEnsayoTemporal> resultado = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IOException("El archivo Excel no contiene hojas");
            }

            // Detectar dinámicamente la fila de encabezado (buscar 'fecha'/'hora'/'medida'/'%HR')
            int headerRowIndex = sheet.getFirstRowNum();
            boolean headerFound = false;
            int scanLimit = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 10);
            for (int i = sheet.getFirstRowNum(); i <= scanLimit; i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                for (Cell c : r) {
                    String hv = formatter.formatCellValue(c).trim().toLowerCase();
                    if (hv.contains("fecha") || hv.contains("hora") || hv.contains("%hr") || hv.contains("medida")) {
                        headerRowIndex = i;
                        headerFound = true;
                        break;
                    }
                }
                if (headerFound) break;
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                throw new IOException("Hoja de Excel vacía o sin encabezado");
            }

            int firstRow = headerRowIndex + 1;
            int lastRow = sheet.getLastRowNum();

            // Mapear índices de columnas relevantes
            Integer idxFecha = null;
            Integer idxHora = null;
            Map<Integer, String> sensorColumns = new HashMap<>();

            for (Cell cell : headerRow) {
                String headRaw = formatter.formatCellValue(cell).trim();
                String head = headRaw.toLowerCase();
                int col = cell.getColumnIndex();
                if (head.contains("fecha") || head.contains("date")) idxFecha = col;
                else if (head.contains("hora") || head.contains("time")) idxHora = col;
                else if (head.contains("medida") || head.matches("^#?\\d+$")) {
                    // 'Medida' es índice/serie, ignorar
                }
                else if (head.contains("transcurrido") || head.contains("transcurrido") || head.contains("eventos") || head.contains("comentario")) {
                    // columnas de texto/meta, ignorar
                }
                else if (head.matches(".*(t\\d+|sensor|s\\d+).*")) {
                    sensorColumns.put(col, headRaw);
                }
                else if (head.contains("%hr") || head.contains("% hr") || head.equals("%hr") || head.equals("hr") || head.contains("humedad") ) {
                    sensorColumns.put(col, "%HR");
                }
                else if (head.contains("°c") || head.contains("celsius") || head.contains("temperatura") || head.equals("c") ) {
                    sensorColumns.put(col, "°C");
                }
                else if (head.matches(".*(temp|temperatura|t[mp]).*")) {
                    sensorColumns.put(col, headRaw);
                }
                else {
                    // columnas numéricas sin nombre claro: se evaluará en la fila de datos
                }
            }

            // Si no se detectaron columnas de sensor explícitas, intentar detectar columnas numéricas en la segunda fila
            if (sensorColumns.isEmpty() && firstRow <= lastRow) {
                Row second = sheet.getRow(firstRow);
                if (second != null) {
                    for (Cell cell : second) {
                        int col = cell.getColumnIndex();
                        String val = formatter.formatCellValue(cell).trim();
                        try {
                            Double.parseDouble(val.replace(',', '.'));
                            // nombrar como columna_{col}
                            sensorColumns.put(col, "col_" + (col+1));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            DateTimeFormatter dateFmt1 = DateTimeFormatter.ofPattern("d/M/yyyy");
            DateTimeFormatter dateFmt2 = DateTimeFormatter.ofPattern("yyyy-M-d");
            DateTimeFormatter timeFmt1 = DateTimeFormatter.ofPattern("H:mm:ss");
            DateTimeFormatter timeFmt2 = DateTimeFormatter.ofPattern("H:mm");

            int seq = 0;
            for (int r = firstRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // construir timestamp
                LocalDate date = null;
                LocalTime time = null;
                try {
                    if (idxFecha != null) {
                        String fechaStr = formatter.formatCellValue(row.getCell(idxFecha)).trim();
                        if (!fechaStr.isEmpty()) {
                            try { date = LocalDate.parse(fechaStr, dateFmt1); } catch (DateTimeParseException ex1) {
                                try { date = LocalDate.parse(fechaStr, dateFmt2); } catch (DateTimeParseException ex2) { date = null; }
                            }
                        }
                    }
                    if (idxHora != null) {
                        String horaStr = formatter.formatCellValue(row.getCell(idxHora)).trim();
                        if (!horaStr.isEmpty()) {
                            try { time = LocalTime.parse(horaStr, timeFmt1); } catch (DateTimeParseException ex1) {
                                try { time = LocalTime.parse(horaStr, timeFmt2); } catch (DateTimeParseException ex2) { time = null; }
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignorar parseo de fecha/hora por fila
                }

                // si no hay fecha/hora, usar secuencia incremental con fecha hoy
                java.time.LocalDateTime timestamp = null;
                if (date != null) {
                    if (time == null) time = LocalTime.MIDNIGHT;
                    timestamp = java.time.LocalDateTime.of(date, time);
                } else {
                    timestamp = java.time.LocalDateTime.now().plusSeconds(seq);
                }

                // por cada columna de sensor, intentar parsear valor
                for (Map.Entry<Integer,String> entry : sensorColumns.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    if (cell == null) continue;
                    String raw = formatter.formatCellValue(cell).trim();
                    if (raw.isEmpty()) continue;
                    try {
                        double valor = Double.parseDouble(raw.replace(',', '.'));
                        DatoEnsayoTemporal d = new DatoEnsayoTemporal();
                        d.setEnsayoId(ensayoId);
                        d.setTimestamp(timestamp);
                        d.setValor(valor);
                        d.setFuente("EXCEL");
                        d.setNumeroSecuencia(seq);
                        d.setSensor(entry.getValue());
                        resultado.add(d);
                    } catch (NumberFormatException nfe) {
                        // ignorar valores no numéricos
                    }
                }

                seq++;
            }

        } catch (Exception e) {
            logger.error("Error procesando Excel: {}", e.getMessage(), e);
            throw new IOException("Error procesando archivo Excel: " + e.getMessage(), e);
        }

        logger.info("Excel procesado, registros obtenidos: {}", resultado.size());
        return resultado;
    }

    private RegressionModelType mapStringToModelType(String tipo) {
        if (tipo == null) return null;
        String t = tipo.trim().toLowerCase();
        if (t.isEmpty()) return null;
        if (t.contains("line") || t.contains("lineal") || t.equals("linear")) return RegressionModelType.LINEAR;
        if (t.contains("cuadr") || t.contains("quad") || t.equals("quadratic") || t.equals("cuadratico")) return RegressionModelType.QUADRATIC;
        if (t.contains("cub") || t.contains("cubic") || t.equals("cubica") || t.equals("cubic")) return RegressionModelType.CUBIC;
        try {
            return RegressionModelType.valueOf(t.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Carga datos desde un PDF SIVCO-LOGGER
     */
    public List<DatoEnsayoTemporal> cargarDatosPdfSivcoLogger(MultipartFile archivo, Long ensayoId) throws IOException {
        logger.info("Iniciando carga de PDF SIVCO-LOGGER para ensayo: {} - archivo: {}", ensayoId, archivo.getOriginalFilename());
        
        // Validar tamaño del archivo para evitar problemas de memoria
        long fileSize = archivo.getSize();
        if (fileSize > 50 * 1024 * 1024) { // 50MB límite
            throw new IOException("El archivo PDF es demasiado grande: " + fileSize + " bytes. Máximo permitido: 50MB");
        }
        logger.info("Tamaño del archivo PDF: {} bytes", fileSize);
        
        // Obtener el ensayo para acceder a su máquina y rangos/límites
        Optional<Ensayo> ensayoOpt = ensayoServicio.obtenerEnsayo(ensayoId);
        Ensayo ensayo = ensayoOpt.orElseThrow(() -> new IllegalArgumentException("Ensayo no encontrado con ID: " + ensayoId));
        final Maquina maquina = ensayo.getMaquina();
        if (maquina != null) {
            logger.info("Máquina del ensayo: {}", maquina.getNombre());
            logger.info("Rangos de validación: [{}, {}]", maquina.getLimiteInferior(), maquina.getLimiteSuperior());
        }
        
        // Usar ExecutorService con timeout para evitar que el procesamiento se cuelgue
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<DatoEnsayoTemporal>> future = executor.submit(new Callable<List<DatoEnsayoTemporal>>() {
            @Override
            public List<DatoEnsayoTemporal> call() throws Exception {
                return procesarPdfSivcoLogger(archivo, ensayoId, maquina);
            }
        });
        
        try {
            // Timeout de 120 segundos para el procesamiento completo (PDFs grandes pueden tardar)
            List<DatoEnsayoTemporal> resultado = future.get(120, TimeUnit.SECONDS);
            logger.info("PDF SIVCO-LOGGER procesado exitosamente en tiempo límite");
            return resultado;
        } catch (TimeoutException e) {
            logger.error("Timeout al procesar PDF SIVCO-LOGGER después de 120 segundos");
            future.cancel(true); // Cancelar la tarea si aún está ejecutándose
            throw new IOException("El procesamiento del PDF excedió el tiempo límite de 120 segundos. " +
                "El archivo es demasiado grande o tiene un formato muy complejo.", e);
        } catch (ExecutionException e) {
            logger.error("Error de ejecución al procesar PDF SIVCO-LOGGER", e);
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            } else {
                throw new IOException("Error interno al procesar el PDF: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
            }
        } catch (InterruptedException e) {
            logger.error("Procesamiento del PDF interrumpido", e);
            Thread.currentThread().interrupt();
            throw new IOException("El procesamiento del PDF fue interrumpido", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private List<DatoEnsayoTemporal> procesarPdfSivcoLogger(MultipartFile archivo, Long ensayoId, Maquina maquina) throws IOException {
        try {
            logger.info("Extrayendo texto del PDF...");
            // Obtener texto del PDF usando el servicio de parsing con fallback a OCR cuando sea posible
            String pdfText = pdfParsingService.extractText(archivo);
            if (pdfText == null || pdfText.trim().isEmpty()) {
                throw new IllegalArgumentException("No se pudo extraer texto del archivo PDF. El PDF podría estar vacío, no contener texto seleccionable o tener un formato inesperado.");
            }
            logger.info("Texto del PDF extraído, longitud: {} caracteres", pdfText.length());
            
            // Extraer datos del PDF
            SivcoLoggerPdfService.SivcoLoggerData loggerData = sivcoLoggerPdfService.parseSivcoLoggerText(pdfText);
            logger.info("Datos SIVCO-LOGGER extraídos: {}", loggerData);
            
            logger.info("Extrayendo datos de temperatura...");
            // Extraer datos de temperatura del texto ya cargado
            List<DatoEnsayoTemporal> datosTemperatura = sivcoLoggerPdfService.extractTemperatureData(pdfText, ensayoId);
            if (datosTemperatura.isEmpty()) {
                throw new IOException("No se encontraron datos de temperatura en el PDF. Verifique que el formato sea correcto.");
            }
            logger.info("Extraídos {} datos de temperatura del PDF", datosTemperatura.size());
            
            // Comparar sensores del PDF con sensores registrados usando el texto ya cargado
            Map<String, Object> comparacionSensores = compararSensoresPdfConRegistrados(pdfText);
            logger.info("Comparación sensores PDF/registrados: {}", comparacionSensores);
            
            @SuppressWarnings("unchecked")
            Set<String> sensoresNoCoincidentes = (Set<String>) comparacionSensores.get("sensoresNoCoincidentes");
            if (sensoresNoCoincidentes != null && !sensoresNoCoincidentes.isEmpty()) {
                throw new IllegalArgumentException("El PDF contiene sensores no registrados: " + sensoresNoCoincidentes + ". " +
                    "Cree los sensores en la sección Sensores con códigos T1, T2, ... antes de volver a cargar.");
            }
            
            logger.info("Procesando y validando {} datos...", datosTemperatura.size());
            // Procesar y validar datos
            List<DatoEnsayoTemporal> datosProcessados = new ArrayList<>();
            int numeroSecuencia = 0;
            
            for (DatoEnsayoTemporal dato : datosTemperatura) {
                String sensorOriginal = dato.getSensor();
                String sensorRegistrado = mapearSensorPdfARegistrado(sensorOriginal);
                if (sensorRegistrado == null) {
                    logger.warn("Sensor del PDF no coincide con ningún sensor registrado: {}", sensorOriginal);
                    continue; // saltar el dato porque no tiene sensor válido
                }
                dato.setSensor(sensorRegistrado);
                dato.setFuente("PDF_SIVCO_LOGGER");
                dato.setNumeroSecuencia(numeroSecuencia++);
                
                // Validar si el valor está fuera de los límites de la máquina
                if (maquina != null && maquina.getLimiteInferior() != null && maquina.getLimiteSuperior() != null) {
                    boolean esAnormal = dato.getValor() < maquina.getLimiteInferior() || 
                                       dato.getValor() > maquina.getLimiteSuperior();
                    dato.setAnormal(esAnormal);
                    if (esAnormal) {
                        logger.debug("Valor anormal detectado en {} con timestamp {}: {} fuera del rango [{}, {}]",
                            dato.getSensor(), dato.getTimestamp(), dato.getValor(),
                            maquina.getLimiteInferior(), maquina.getLimiteSuperior());
                    }
                } else {
                    dato.setAnormal(false);
                }
                
                datosProcessados.add(dato);
            }
            
            if (datosProcessados.isEmpty()) {
                throw new IllegalArgumentException("No se pudieron procesar datos válidos del PDF. Todos los sensores podrían no estar registrados o el formato de filas no coincide con el esperado.");
            }
            
            logger.info("PDF SIVCO-LOGGER procesado: {} registros de temperatura validados", datosProcessados.size());
            
            logger.info("Aplicando correcciones a {} datos...", datosProcessados.size());
            // Aplicar correcciones existentes en paralelo para mejor rendimiento
            List<DatoEnsayoTemporal> datosCorregidos = aplicarCorreccionesEnParalelo(datosProcessados, ensayoId);
            logger.info("Correcciones aplicadas: {} datos procesados, {} datos corregidos", 
                datosProcessados.size(), datosCorregidos.size());
            
            return datosCorregidos;
            
        } catch (Exception e) {
            logger.error("Error al procesar PDF SIVCO-LOGGER", e);
            throw new IOException("Error al procesar el archivo PDF SIVCO-LOGGER: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método auxiliar para obtener el texto completo del PDF
     */
    private String obtenerTextoPdf(MultipartFile archivo) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument document = 
             org.apache.pdfbox.pdmodel.PDDocument.load(archivo.getInputStream())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    public List<DatoEnsayoTemporal> cargarDatosCSV(MultipartFile archivo, Long ensayoId, Set<String> sensoresPermitidos) throws IOException {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        
        logger.info("Iniciando carga de CSV para ensayo: " + ensayoId + ", archivo: " + archivo.getOriginalFilename());
        
        // Obtener el ensayo para acceder a su máquina y rangos/límites
        Optional<Ensayo> ensayoOpt = ensayoServicio.obtenerEnsayo(ensayoId);
        Maquina maquina = null;
        if (ensayoOpt.isPresent()) {
            Ensayo ensayo = ensayoOpt.get();
            maquina = ensayo.getMaquina();
            if (maquina != null) {
                logger.info("Máquina del ensayo: " + maquina.getNombre());
                logger.info("Rangos de validación: [" + maquina.getLimiteInferior() + ", " + maquina.getLimiteSuperior() + "]");
            } else {
                logger.warn("El ensayo no tiene máquina asociada");
            }
        } else {
            logger.warn("Ensayo no encontrado con ID: " + ensayoId);
        }
        
        try (InputStreamReader streamReader = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8)) {
            // Detectar separador (coma o punto y coma) usando OpenCSV
            char separator = detectSeparator(archivo);
            logger.info("Separador detectado: '" + separator + "'");
            
            // Usar CSVReader con el separador detectado
            CSVReader csvReader = new CSVReaderBuilder(streamReader)
                    .withCSVParser(new CSVParserBuilder()
                       .withSeparator(separator)
                       .withIgnoreQuotations(false)
                       .build())
                   .build();
            
            List<String[]> allLines = csvReader.readAll();
            int numeroSecuencia = 0;

            logger.info("Total de líneas en archivo: " + allLines.size());

            // Intentar extraer nombres de sensor desde la línea de encabezado (4ª línea)
            String[] headerSensors = null;
            if (allLines.size() >= 4) {
                String[] possibleHeader = allLines.get(3); // índice 3 = línea 4
                if (possibleHeader != null && possibleHeader.length >= 3) {
                    boolean hasSensorNames = false;
                    for (int i = 2; i < possibleHeader.length; i++) {
                        if (possibleHeader[i] != null && !possibleHeader[i].trim().isEmpty()) {
                            hasSensorNames = true;
                            break;
                        }
                    }
                    if (hasSensorNames) {
                        headerSensors = new String[possibleHeader.length];
                        for (int i = 0; i < possibleHeader.length; i++) {
                            headerSensors[i] = possibleHeader[i] == null ? null : possibleHeader[i].trim();
                        }
                        logger.info("Header de sensores detectado en CSV: " + Arrays.toString(headerSensors));

                        // Si se proporcionó un conjunto de sensores permitidos (desde PDF), filtrar nombres
                        if (sensoresPermitidos != null && !sensoresPermitidos.isEmpty()) {
                            Set<String> permitidosNorm = new HashSet<>();
                            for (String s : sensoresPermitidos) {
                                if (s == null) continue;
                                permitidosNorm.add(s.toLowerCase().trim());
                                try {
                                    String norm = normalizarNombreSensor(s);
                                    if (norm != null) permitidosNorm.add(norm);
                                } catch (Exception ignore) {}
                                Matcher mt = Pattern.compile("[tT](\\d+)").matcher(s);
                                if (mt.find()) {
                                    permitidosNorm.add("sensor_" + mt.group(1));
                                }
                            }

                            // Filtrar headerSensors que no estén en permitidosNorm
                            for (int i = 0; i < headerSensors.length; i++) {
                                if (i < 2) continue; // columnas de fecha/hora
                                String h = headerSensors[i];
                                if (h == null || h.isEmpty()) {
                                    headerSensors[i] = null;
                                    continue;
                                }
                                String hNorm = normalizarNombreSensor(h);
                                if (!permitidosNorm.contains(hNorm) && !permitidosNorm.contains(h.toLowerCase().trim())) {
                                    logger.debug("Header sensor '{}' no coincide con PDF, será ignorado para mapeo", h);
                                    headerSensors[i] = null;
                                } else {
                                    headerSensors[i] = h;
                                }
                            }
                        }
                    }
                }
            }
            
            for (int lineIndex = 0; lineIndex < allLines.size(); lineIndex++) {
                String[] partes = allLines.get(lineIndex);
                int numeroLinea = lineIndex + 1;
                
                // Saltar las primeras 4 líneas (metadatos/encabezados)
                if (numeroLinea <= 4) {
                    logger.debug("Saltando línea de encabezado: " + numeroLinea);
                    continue;
                }
                
                // Saltar líneas vacías
                if (partes == null || partes.length == 0) {
                    continue;
                }
                
                // Verificar si toda la línea está vacía
                boolean isEmpty = true;
                for (String p : partes) {
                    if (p != null && !p.trim().isEmpty()) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    continue;
                }
                
                // Parsear línea: formato esperado es [DATE, TIME, val1, val2, val3, val4, ...]
                if (partes.length >= 3) {
                    try {
                        String fecha = partes[0].trim();
                        String hora = partes[1].trim();
                        String fechaHora = fecha + " " + hora;
                        
                        LocalDateTime timestamp = parseTimestamp(fechaHora);
                        
                        // Procesar todos los valores numéricos a partir de la posición 2
                        for (int i = 2; i < partes.length; i++) {
                            String valorStr = partes[i].trim();
                            if (!valorStr.isEmpty()) {
                                try {
                                    Double valor = Double.parseDouble(valorStr);
                                    
                                    DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
                                    dato.setEnsayoId(ensayoId);
                                    dato.setTimestamp(timestamp);
                                    dato.setValor(valor);
                                    String sensorName = "sensor_" + (i - 1);
                                    // Si se detectaron nombres de sensor en el encabezado, úsalos
                                    if (headerSensors != null && i < headerSensors.length && headerSensors[i] != null && !headerSensors[i].isEmpty()) {
                                        sensorName = normalizarNombreSensor(headerSensors[i]);
                                    }
                                    dato.setFuente("ARCHIVO_CICLO");
                                    dato.setSensor(sensorName);
                                    dato.setNumeroSecuencia(numeroSecuencia++);
                                    
                                    // Validar si el valor está fuera de los límites de la máquina
                                    if (maquina != null && maquina.getLimiteInferior() != null && maquina.getLimiteSuperior() != null) {
                                        boolean esAnormal = valor < maquina.getLimiteInferior() || valor > maquina.getLimiteSuperior();
                                        dato.setAnormal(esAnormal);
                                        if (esAnormal) {
                                            logger.debug("Valor anormal detectado en " + sensorName + " (línea " + numeroLinea + "): " + valor + 
                                                " fuera del rango [" + maquina.getLimiteInferior() + ", " + maquina.getLimiteSuperior() + "]");
                                        }
                                    } else {
                                        dato.setAnormal(false);
                                    }
                                    
                                    datos.add(dato);
                                } catch (NumberFormatException nfe) {
                                    logger.debug("Valor no numérico ignorado en línea " + numeroLinea + ", columna " + i + ": " + valorStr);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error procesando línea " + numeroLinea + ": " + String.join("|", partes), e);
                    }
                }
            }
            
            logger.info("CSV cargado exitosamente: " + datos.size() + " registros procesados de " + allLines.size() + " líneas");
            
            // Aplicar correcciones existentes antes de devolver los datos
            List<DatoEnsayoTemporal> datosCorregidos = aplicarCorrecciones(datos, ensayoId);
            logger.info("Correcciones aplicadas: " + (datos.size() - datosCorregidos.size()) + " datos modificados");
            
            return datosCorregidos;
        } catch (Exception e) {
            logger.error("Error al cargar CSV", e);
            throw new IOException("Error al cargar el archivo CSV: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detecta automáticamente si el separador del CSV es coma (,) o punto y coma (;)
     */
    private char detectSeparator(MultipartFile archivo) throws IOException {
        try (InputStreamReader streamReader = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8)) {
            CSVReader commaReader = new CSVReaderBuilder(streamReader)
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                    .build();
            String[] firstDataLine = null;
            int lineNum = 0;
            for (String[] line : commaReader.readAll()) {
                lineNum++;
                if (lineNum > 4) { // Skip header lines
                    firstDataLine = line;
                    break;
                }
            }
            
            if (firstDataLine != null && firstDataLine.length >= 3) {
                return ',';
            }
        } catch (Exception e) {
            logger.debug("Comma detection failed, trying semicolon");
        }
        
        // Si falla con coma, intentar con punto y coma
        try (InputStreamReader streamReader = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8)) {
            CSVReader semiReader = new CSVReaderBuilder(streamReader)
                    .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                    .build();
            String[] firstDataLine = null;
            int lineNum = 0;
            for (String[] line : semiReader.readAll()) {
                lineNum++;
                if (lineNum > 4) {
                    firstDataLine = line;
                    break;
                }
            }
            
            if (firstDataLine != null && firstDataLine.length >= 3) {
                return ';';
            }
        } catch (Exception e) {
            logger.debug("Semicolon detection failed, defaulting to comma");
        }
        
        return ','; // Default to comma
    }
    
    // Agregar dato temporal al ensayo (para que aparezca en análisis)
    public void agregarDatoTemporalAlEnsayo(Long ensayoId, DatoEnsayoTemporal dato) {
        ensayoServicio.agregarDatoTemporalCSV(ensayoId, dato);
    }
    
    private LocalDateTime parseTimestamp(String timestamp) {
        timestamp = timestamp.trim();
        
        // Intentar con ISO_LOCAL_DATE_TIME primero (más rápido)
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e1) {
            // Continuar con otros formatos
        }
        
        String[] formatos = {
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "M/d/yyyy HH:mm:ss",           // Formato: 1/10/2025 10:06:18
            "dd/MM/yyyy HH:mm:ss",         // Formato: 22/10/2025 07:43:02
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "M/d/yyyy",
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
        };
        
        for (String formato : formatos) {
            try {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern(formato));
            } catch (Exception e) {
                // Continuar con el siguiente formato
            }
        }
        
        // Si ningún formato funciona, usar la hora actual
        logger.warn("No se pudo parsear timestamp: " + timestamp + ", usando hora actual");
        return LocalDateTime.now();
    }
    
    public List<DatoEnsayoTemporal> cargarDatosJSON(MultipartFile archivo, Long ensayoId) throws IOException {
        // Implementación básica - se puede expandir con una librería JSON
        throw new UnsupportedOperationException("Carga JSON aún no implementada");
    }
    
    public List<DatoEnsayoTemporal> cargarDatosTxt(MultipartFile archivo, Long ensayoId) throws IOException {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        
        logger.info("Iniciando carga de TXT para ensayo: " + ensayoId + ", archivo: " + archivo.getOriginalFilename());
        
        // Obtener el ensayo para acceder a su máquina y rangos/límites
        Optional<Ensayo> ensayoOpt = ensayoServicio.obtenerEnsayo(ensayoId);
        Maquina maquina = null;
        if (ensayoOpt.isPresent()) {
            Ensayo ensayo = ensayoOpt.get();
            maquina = ensayo.getMaquina();
            if (maquina != null) {
                logger.info("Máquina del ensayo: " + maquina.getNombre());
                logger.info("Rangos de validación: [" + maquina.getLimiteInferior() + ", " + maquina.getLimiteSuperior() + "]");
            } else {
                logger.warn("El ensayo no tiene máquina asociada");
            }
        } else {
            logger.warn("Ensayo no encontrado con ID: " + ensayoId);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String linea;
            int numeroSecuencia = 0;
            
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                
                String[] partes = linea.split("\\s+");
                if (partes.length >= 2) {
                    try {
                        // Combinar fecha y hora si están separadas
                        String fechaHora;
                        Double valor;
                        String fuente;
                        
                        if (partes.length >= 3) {
                            fechaHora = partes[0] + " " + partes[1];
                            valor = Double.parseDouble(partes[2]);
                            fuente = partes.length > 3 ? partes[3] : "ARCHIVO";
                        } else {
                            fechaHora = partes[0];
                            valor = Double.parseDouble(partes[1]);
                            fuente = "ARCHIVO";
                        }
                        
                        LocalDateTime timestamp = parseTimestamp(fechaHora);
                        
                        DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
                        dato.setEnsayoId(ensayoId);
                        dato.setTimestamp(timestamp);
                        dato.setValor(valor);
                        dato.setFuente(fuente);
                        dato.setSensor("sensor_1"); // Por defecto para archivos TXT
                        dato.setNumeroSecuencia(numeroSecuencia++);
                        
                        // Validar si el valor está fuera de los límites de la máquina
                        if (maquina != null && maquina.getLimiteInferior() != null && maquina.getLimiteSuperior() != null) {
                            boolean esAnormal = valor < maquina.getLimiteInferior() || valor > maquina.getLimiteSuperior();
                            dato.setAnormal(esAnormal);
                            if (esAnormal) {
                                logger.debug("Valor anormal detectado en sensor_1 (línea " + (numeroSecuencia) + "): " + valor + 
                                    " fuera del rango [" + maquina.getLimiteInferior() + ", " + maquina.getLimiteSuperior() + "]");
                            }
                        } else {
                            dato.setAnormal(false);
                        }
                        
                        datos.add(dato);
                        
                        // Registrar el dato en el servicio - sin excepción si falla
                        try {
                            ensayoServicio.registrarDato(ensayoId, valor, fuente);
                        } catch (Exception e) {
                            System.err.println("Advertencia: No se pudo registrar dato en servicio: " + e.getMessage());
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error procesando línea: " + linea + " - " + e.getMessage());
                        // Ignorar líneas con formato inválido
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar TXT: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Error al cargar el archivo TXT: " + e.getMessage(), e);
        }
        
        // Aplicar correcciones existentes antes de devolver los datos
        List<DatoEnsayoTemporal> datosCorregidos = aplicarCorrecciones(datos, ensayoId);
        
        return datosCorregidos;
    }

    // Compatibilidad: llamada antigua sin conjunto de sensores (sin PDF)
    public List<DatoEnsayoTemporal> cargarDatosCSV(MultipartFile archivo, Long ensayoId) throws IOException {
        return cargarDatosCSV(archivo, ensayoId, Collections.emptySet());
    }

    /**
     * Detecta códigos de sensores dentro de un PDF (ej: t1, t12) y devuelve el conjunto encontrado.
     */
    public java.util.Set<String> detectarSensoresEnPDF(MultipartFile archivo, Long ensayoId) throws IOException {
        logger.info("Iniciando detección de sensores en PDF para ensayo: {} archivo: {}", ensayoId, archivo.getOriginalFilename());

        String filename = archivo.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            logger.warn("Archivo no es PDF: {}", filename);
            throw new IOException("El archivo debe ser PDF");
        }

        String texto = pdfParsingService.extractText(archivo);
        java.util.Set<String> sensores = pdfParsingService.extractSensorCodes(texto);

        logger.info("Sensores detectados en PDF {}: {}", filename, sensores);

        return sensores;
    }

    /**
     * Método de diagnóstico: extrae texto del PDF, devuelve vista previa y sensores detectados.
     * Útil para depuración cuando los PDFs no contienen texto seleccionable.
     */
    public java.util.Map<String, Object> procesarPDFParaDepuracion(MultipartFile archivo, Long ensayoId) throws IOException {
        java.util.Map<String, Object> resultado = new java.util.HashMap<>();

        logger.info("Procesando PDF para depuración: ensayo {}, archivo {}", ensayoId, archivo.getOriginalFilename());

        try {
            String texto = pdfParsingService.extractText(archivo);
            java.util.Set<String> sensores = pdfParsingService.extractSensorCodes(texto);

            String preview = "";
            if (texto != null && !texto.isEmpty()) {
                int max = Math.min(2048, texto.length());
                preview = texto.substring(0, max);
            }

            resultado.put("sensores", sensores);
            resultado.put("preview", preview);
            resultado.put("textLength", texto != null ? texto.length() : 0);
            // Guardar el texto completo en un archivo temporal para inspección si es necesario
            try {
                if (texto != null && !texto.isEmpty()) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("pdf_extracted_", ".txt");
                    java.nio.file.Files.writeString(tmp, texto, java.nio.charset.StandardCharsets.UTF_8);
                    resultado.put("textFile", tmp.toAbsolutePath().toString());
                }
            } catch (Exception ex) {
                logger.debug("No se pudo escribir textFile temporal: {}", ex.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error procesando PDF para depuración: {}", e.getMessage(), e);
            resultado.put("error", e.getMessage());
        }

        return resultado;
    }
    
    /**
     * Compara los sensores extraídos del PDF con los sensores registrados en el sistema.
     * Devuelve información de coincidencias y diferencias.
     */
    public Map<String, Object> compararSensoresPdfConRegistrados(MultipartFile archivo) throws IOException {
        String texto = pdfParsingService.extractText(archivo);
        return compararSensoresPdfConRegistrados(texto);
    }

    public Map<String, Object> compararSensoresPdfConRegistrados(String texto) {
        Set<String> sensoresPdf = pdfParsingService.extractSensorCodes(texto);
        Set<String> sensoresRegistrados = sensorServicio.listarActivos().stream()
            .map(sensor -> sensor.getCodigo() == null ? "" : sensor.getCodigo().trim().toLowerCase())
            .filter(codigo -> !codigo.isEmpty())
            .collect(Collectors.toSet());

        Map<String, String> mapaSensor = new HashMap<>();
        Set<String> sensoresCoincidentes = new HashSet<>();
        Set<String> sensoresNoCoincidentes = new HashSet<>();
        
        for (String sensorPdf : sensoresPdf) {
            String codigo = mapearSensorPdfARegistrado(sensorPdf);
            if (codigo != null) {
                String codigoLower = codigo.toLowerCase();
                if (sensoresRegistrados.contains(codigoLower)) {
                    sensoresCoincidentes.add(sensorPdf);
                    mapaSensor.put(sensorPdf, codigo);
                } else {
                    sensoresNoCoincidentes.add(sensorPdf);
                }
            } else {
                sensoresNoCoincidentes.add(sensorPdf);
            }
        }
        
        Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("sensoresPdf", sensoresPdf);
        resultado.put("sensoresRegistrados", sensoresRegistrados);
        resultado.put("mapaSensorPdfARegistrado", mapaSensor);
        resultado.put("sensoresCoincidentes", sensoresCoincidentes);
        resultado.put("sensoresNoCoincidentes", sensoresNoCoincidentes);
        resultado.put("todosCoinciden", sensoresNoCoincidentes.isEmpty());
        return resultado;
    }

    private String mapearSensorPdfARegistrado(String sensorPdf) {
        if (sensorPdf == null || sensorPdf.isEmpty()) {
            return null;
        }
        String codigoEsperado = convertirSensorPdfATipoCodigo(sensorPdf);
        if (codigoEsperado == null) {
            return null;
        }
        
        Set<String> sensoresRegistrados = sensorServicio.listarActivos().stream()
            .map(sensor -> sensor.getCodigo() == null ? "" : sensor.getCodigo().trim().toUpperCase())
            .collect(Collectors.toSet());
        
        if (sensoresRegistrados.contains(codigoEsperado.toUpperCase())) {
            return codigoEsperado.toUpperCase();
        }
        return null;
    }

    private String convertirSensorPdfATipoCodigo(String sensorPdf) {
        String original = sensorPdf.trim().toLowerCase();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("t\\s*(\\d+)").matcher(original);
        if (matcher.find()) {
            return "T" + Integer.parseInt(matcher.group(1));
        }
        matcher = java.util.regex.Pattern.compile("sensor[_\\s]?(\\d+)").matcher(original);
        if (matcher.find()) {
            return "T" + Integer.parseInt(matcher.group(1));
        }
        matcher = java.util.regex.Pattern.compile("^(\\d+)$").matcher(original);
        if (matcher.find()) {
            return "T" + Integer.parseInt(matcher.group(1));
        }
        return sensorPdf.trim().toUpperCase();
    }
    
    /**
     * REFACTORED: Applies calibration corrections to measurement data.
     * 
     * Now uses the regression-based calibration system (CalibrationSession + RegressionModels)
     * with fallback to legacy CoeficientesCorreccion for backward compatibility.
     * 
     * Workflow:
     * 1. For each sensor in the data, retrieves active calibration from the new system
     * 2. If new calibration exists, uses the active regression model
     * 3. If new system calibration not found, falls back to legacy system
     * 4. Applies correction: corrected_value = applyCorrection(instrument_reading)
     * 
     * @param datosOriginales the original uncorrected measurement data
     * @param ensayoId the test ID
     * @return the corrected measurement data
     */
    private List<DatoEnsayoTemporal> aplicarCorrecciones(List<DatoEnsayoTemporal> datosOriginales, Long ensayoId) {
        try {
            logger.info("Iniciando aplicación de correcciones para {} datos del ensayo {}", datosOriginales.size(), ensayoId);
            
            // Determine sensors in the data
            Set<String> sensoresEnDatos = datosOriginales.stream()
                .map(DatoEnsayoTemporal::getSensor)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());

            if (sensoresEnDatos.isEmpty()) {
                logger.info("No sensors detected in data for test " + ensayoId);
                return datosOriginales;
            }

            logger.info("Detected sensors: {}", sensoresEnDatos);
            
            logger.debug("Resolviendo device IDs para sensores...");
            // Map device IDs to sensors (from sensor name to device ID)
            Map<String, Long> sensorToDeviceId = resolveSensorDeviceIds(sensoresEnDatos);
            logger.info("Resueltos {} device IDs para sensores", sensorToDeviceId.size());

            // Read ensayo-level preferred model (if any) so we can honor it when applying new-system corrections
            RegressionModelType ensayoPreferredModel = null;
            try {
                Optional<Ensayo> optEns = ensayoServicio.obtenerEnsayo(ensayoId);
                if (optEns.isPresent()) {
                    String obs = optEns.get().getObservaciones();
                    if (obs != null) {
                        // look for pattern ModeloPreferido:VALUE (case-insensitive)
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("ModeloPreferido\s*:\s*([A-Za-z0-9_\-]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(obs);
                        if (m.find()) {
                            String val = m.group(1).trim();
                            ensayoPreferredModel = mapStringToModelType(val);
                            logger.info("Ensayo {} requests preferred model: {}", ensayoId, ensayoPreferredModel);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.debug("No se pudo leer preferencia de modelo del ensayo {}: {}", ensayoId, ex.getMessage());
            }
            
            // Create a map of sensor name to active calibration (prioritize new system)
            Map<String, com.sivco.gestion_archivos.modelos.calibration.CalibrationSession> newSystemCalibrations = new HashMap<>();
            Map<String, CoeficientesCorreccion> legacyCoefficients = new HashMap<>();
            Map<String, Integer> legacyCorrectionIndexes = new HashMap<>();
            
            logger.debug("Buscando calibraciones activas para {} sensores...", sensoresEnDatos.size());
            for (String sensorName : sensoresEnDatos) {
                String normalizedSensorName = normalizarNombreSensor(sensorName);
                Long deviceId = sensorToDeviceId.get(normalizedSensorName);
                
                if (deviceId != null) {
                    // Try new system first
                    try {
                        com.sivco.gestion_archivos.modelos.calibration.CalibrationSession newCalibration = 
                            calibrationManagementService.getActiveCalibration(deviceId);
                        
                        if (newCalibration != null) {
                            newSystemCalibrations.put(normalizedSensorName, newCalibration);
                            newSystemCalibrations.put(sensorName, newCalibration);
                            logger.info("Using new calibration system for sensor {}", sensorName);
                            continue;
                        }
                    } catch (Exception e) {
                        logger.debug("New calibration system not available for device {}: {}", deviceId, e.getMessage());
                    }
                }
                
                // Fallback to legacy system - load coefficients from CSV file
                try {
                    Map<String, CoeficientesCorreccion> loadedCoefs = cargarCorreccionCSVPorSensorNombre(normalizedSensorName);
                    if (!loadedCoefs.isEmpty()) {
                        legacyCoefficients.putAll(loadedCoefs);
                        legacyCoefficients.put(sensorName, loadedCoefs.values().iterator().next());
                        logger.info("Using legacy calibration system for sensor {}", sensorName);
                    }
                } catch (Exception e) {
                    logger.warn("Error loading legacy calibration for sensor {}: {}", sensorName, e.getMessage());
                }
            }

            if (newSystemCalibrations.isEmpty() && legacyCoefficients.isEmpty()) {
                logger.warn("No calibrations available for any sensor in test {}", ensayoId);
                // Don't throw - continue with uncorrected data
            }

            logger.info("Aplicando correcciones a {} data points (new system: {}, legacy: {})", 
                datosOriginales.size(), newSystemCalibrations.size(), legacyCoefficients.size());
            
            // Apply corrections to all data
            List<DatoEnsayoTemporal> datosCorregidos = new ArrayList<>();
            int correccionesAplicadas = 0;
            
            for (DatoEnsayoTemporal dato : datosOriginales) {
                try {
                    DatoEnsayoTemporal datoCorregido = dato;
                    
                    String sensorName = dato.getSensor();
                    String normalizedSensorName = normalizarNombreSensor(sensorName);
                    
                    // Try new system first
                    com.sivco.gestion_archivos.modelos.calibration.CalibrationSession newCalibration = 
                        newSystemCalibrations.get(normalizedSensorName);
                    
                    if (newCalibration != null && dato.getValor() != null) {
                        try {
                            Double correctedValue;
                            // If ensayo requests a specific model, try to find that model in the session and apply it directly
                            if (ensayoPreferredModel != null) {
                                RegressionModel rm = null;
                                if (newCalibration.getRegressionModels() != null) {
                                    for (RegressionModel m : newCalibration.getRegressionModels()) {
                                        if (m != null && m.getModelType() == ensayoPreferredModel) { rm = m; break; }
                                    }
                                }
                                if (rm != null) {
                                    correctedValue = dato.getValor() + rm.applyCorrectionModel(dato.getValor());
                                    // mark which model used via appliedCalibrationId (session id) and could store model type in metadata if needed
                                } else {
                                    // fallback to session default
                                    correctedValue = newCalibration.applyCorrection(dato.getValor());
                                }
                            } else {
                                correctedValue = newCalibration.applyCorrection(dato.getValor());
                            }
                            datoCorregido = copiarDato(dato);
                            datoCorregido.setValor(correctedValue);
                            datoCorregido.setAppliedCalibrationId(newCalibration.getId());
                            correccionesAplicadas++;
                            logger.debug("Applied new calibration to sensor {}: {} -> {}", 
                                sensorName, dato.getValor(), correctedValue);
                        } catch (IllegalStateException e) {
                            logger.debug("New calibration for sensor {} has no active model, trying legacy: {}", 
                                sensorName, e.getMessage());
                            // Fallthrough to legacy system
                            datoCorregido = aplicarCorreccionLegacy(dato, legacyCoefficients, legacyCorrectionIndexes);
                            if (datoCorregido != dato) {
                                correccionesAplicadas++;
                            }
                        }
                    } else {
                        // Use legacy system
                        datoCorregido = aplicarCorreccionLegacy(dato, legacyCoefficients, legacyCorrectionIndexes);
                        if (datoCorregido != dato) {
                            correccionesAplicadas++;
                        }
                    }
                    
                    datosCorregidos.add(datoCorregido);
                    
                } catch (Exception e) {
                    logger.warn("Error applying correction to individual data point: " + e.getMessage());
                    // On error, add original data
                    datosCorregidos.add(dato);
                }
            }
            
            logger.info("Successfully applied {} corrections out of {} data points", 
                correccionesAplicadas, datosOriginales.size());
            
            return datosCorregidos;
            
        } catch (Exception e) {
            logger.error("Error applying corrections: " + e.getMessage(), e);
            // Return original data on error
            return datosOriginales;
        }
    }

    /**
     * Aplica correcciones en paralelo para mejor rendimiento con PDFs grandes
     */
    private List<DatoEnsayoTemporal> aplicarCorreccionesEnParalelo(List<DatoEnsayoTemporal> datosOriginales, Long ensayoId) {
        try {
            int numChunks = Math.max(1, Math.min(4, datosOriginales.size() / 2000));
            if (numChunks == 1) {
                return aplicarCorrecciones(datosOriginales, ensayoId);
            }
            
            int chunkSize = (datosOriginales.size() + numChunks - 1) / numChunks;
            ExecutorService executor = Executors.newFixedThreadPool(numChunks);
            List<Future<List<DatoEnsayoTemporal>>> futures = new ArrayList<>();
            
            for (int i = 0; i < datosOriginales.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, datosOriginales.size());
                List<DatoEnsayoTemporal> chunk = new ArrayList<>(datosOriginales.subList(i, end));
                
                futures.add(executor.submit(() -> aplicarCorrecciones(chunk, ensayoId)));
            }
            
            List<DatoEnsayoTemporal> resultado = new ArrayList<>();
            for (Future<List<DatoEnsayoTemporal>> future : futures) {
                resultado.addAll(future.get(60, TimeUnit.SECONDS));
            }
            executor.shutdown();
            
            return resultado;
        } catch (Exception e) {
            logger.warn("Error en procesamiento paralelo, usando secuencial: {}", e.getMessage());
            return aplicarCorrecciones(datosOriginales, ensayoId);
        }
    }

    /**
     * Copies a DatoEnsayoTemporal object
     */
    private DatoEnsayoTemporal copiarDato(DatoEnsayoTemporal dato) {
        DatoEnsayoTemporal copia = new DatoEnsayoTemporal();
        copia.setId(dato.getId());
        copia.setEnsayoId(dato.getEnsayoId());
        copia.setTimestamp(dato.getTimestamp());
        copia.setValor(dato.getValor());
        copia.setAnormal(dato.getAnormal());
        copia.setFuente(dato.getFuente());
        copia.setNumeroSecuencia(dato.getNumeroSecuencia());
        copia.setSensor(dato.getSensor());
        copia.setAppliedCalibrationId(dato.getAppliedCalibrationId());
        return copia;
    }

    /**
     * Applies legacy coefficient-based correction to a single data point
     */
    private DatoEnsayoTemporal aplicarCorreccionLegacy(
            DatoEnsayoTemporal dato, 
            Map<String, CoeficientesCorreccion> coeficientesCorreccion,
            Map<String, Integer> legacyCorrectionIndexes) {
        
        String sensorName = dato.getSensor();
        String normalizedSensorName = normalizarNombreSensor(sensorName);
        
        CoeficientesCorreccion coef = coeficientesCorreccion.get(normalizedSensorName);
        if (coef == null) {
            coef = coeficientesCorreccion.get(sensorName);
        }
        
        if (coef == null || dato.getValor() == null) {
            logger.debug("No correction coefficients found for sensor: " + sensorName);
            return dato;
        }
        
        try {
            Double correctedValue;
            if (coef.getValores() != null && !coef.getValores().isEmpty()) {
                int index = legacyCorrectionIndexes.getOrDefault(normalizedSensorName, 0);
                if (index < coef.getValores().size()) {
                    double correctionOffset = coef.getValores().get(index);
                    correctedValue = dato.getValor() + correctionOffset;
                    legacyCorrectionIndexes.put(normalizedSensorName, index + 1);
                    logger.debug("Applied legacy correction offset to sensor {}: {} + {} = {}", 
                        sensorName, dato.getValor(), correctionOffset, correctedValue);
                } else {
                    // Fallback to polynomial correction if direct values are exhausted
                    double correctionOffset = coef.aplicarCorreccion(dato.getValor());
                    correctedValue = dato.getValor() + correctionOffset;
                    logger.debug("Applied legacy polynomial correction to sensor {}: {} + {} = {}", 
                        sensorName, dato.getValor(), correctionOffset, correctedValue);
                }
            } else {
                double correctionOffset = coef.aplicarCorreccion(dato.getValor());
                correctedValue = dato.getValor() + correctionOffset;
                logger.debug("Applied legacy polynomial correction to sensor {}: {} + {} = {}", 
                    sensorName, dato.getValor(), correctionOffset, correctedValue);
            }

            DatoEnsayoTemporal datoCorregido = copiarDato(dato);
            datoCorregido.setValor(correctedValue);
            return datoCorregido;
        } catch (Exception e) {
            logger.warn("Error applying legacy correction to sensor {}: {}", sensorName, e.getMessage());
            return dato;
        }
    }

    /**
     * Resolves sensor names to device IDs for new calibration system lookups
     * Device IDs correspond to sensor IDs in the database
     */
    private Map<String, Long> resolveSensorDeviceIds(Set<String> sensorNames) {
        Map<String, Long> result = new HashMap<>();
        
        try {
            logger.debug("Resolviendo device IDs para sensores: {}", sensorNames);
            
            // Get all active sensors at once to avoid multiple database queries
            List<com.sivco.gestion_archivos.modelos.Sensor> sensoresActivos = sensorServicio.listarActivos();
            logger.debug("Encontrados {} sensores activos en la base de datos", sensoresActivos.size());
            
            for (String sensorName : sensorNames) {
                String normalized = normalizarNombreSensor(sensorName);
                try {
                    // First try: if it's already in "sensor_N" format, extract N as deviceId
                    if (normalized.startsWith("sensor_")) {
                        try {
                            Long deviceId = Long.parseLong(normalized.substring("sensor_".length()));
                            result.put(normalized, deviceId);
                            continue;
                        } catch (NumberFormatException e) {
                            logger.debug("Could not extract device ID from sensor name: {}", normalized);
                        }
                    }
                    
                    // Second try: look up sensor by code (T1, T2, etc.) and get its ID
                    try {
                        List<com.sivco.gestion_archivos.modelos.Sensor> matchingSensors = sensoresActivos.stream()
                            .filter(s -> {
                                String sensorCode = s.getCodigo();
                                if (sensorCode == null) return false;
                                String normalizedCode = normalizarNombreSensor(sensorCode);
                                return normalizedCode.equals(normalized) || sensorCode.equalsIgnoreCase(sensorName);
                            })
                            .toList();
                        
                        if (!matchingSensors.isEmpty()) {
                            Long deviceId = matchingSensors.get(0).getId(); // Take first match
                            result.put(normalized, deviceId);
                            result.put(sensorName, deviceId); // Also map original name
                            logger.debug("Resolved sensor '{}' (normalized: '{}') to deviceId: {}", 
                                sensorName, normalized, deviceId);
                        } else {
                            logger.debug("No sensor found with code '{}' or normalized '{}'", sensorName, normalized);
                        }
                    } catch (Exception e) {
                        logger.debug("Error looking up sensor by code for '{}': {}", sensorName, e.getMessage());
                    }
                    
                } catch (Exception e) {
                    logger.debug("Error resolving device ID for sensor {}: {}", sensorName, e.getMessage());
                }
            }
            
            logger.info("Resolved {} out of {} sensor names to device IDs", result.size(), sensorNames.size());
            return result;
        } catch (Exception e) {
            logger.error("Error in resolveSensorDeviceIds: {}", e.getMessage(), e);
            return new HashMap<>(); // Return empty map on error
        }
    }

    /**
     * Carga todos los coeficientes de corrección de los archivos de corrección
     */
    // Carga coeficientes desde un archivo CSV ya ubicado en el sistema, opcionalmente filtrando por clave sensor esperada
    private Map<String, CoeficientesCorreccion> cargarCorreccionCSVDesdeRuta(com.sivco.gestion_archivos.modelos.CalibrationCorrection correccion, String expectedKey) throws IOException {
        return cargarCorreccionCSVDesdeRuta(correccion.getRutaArchivo(), expectedKey);
    }

    private Map<String, CoeficientesCorreccion> cargarCorreccionCSVDesdeRuta(String rutaArchivoStr, String expectedKey) throws IOException {
        Map<String, CoeficientesCorreccion> coeficientes = new HashMap<>();
        Path rutaArchivo = Paths.get(rutaArchivoStr);
        if (!Files.exists(rutaArchivo)) {
            logger.warn("Archivo de calibración no encontrado: " + rutaArchivo);
            return coeficientes;
        }

        try (BufferedReader reader = Files.newBufferedReader(rutaArchivo, StandardCharsets.UTF_8)) {
            String linea;
            boolean primeraLinea = true;
            int lineasProcesadas = 0;

            while ((linea = reader.readLine()) != null) {
                lineasProcesadas++;

                if (primeraLinea) {
                    primeraLinea = false; // Saltar encabezado
                    continue;
                }

                String[] partes = splitCsvPreservingQuotes(linea);

                if (partes.length >= 5) {
                    try {
                        String sensorOriginal = partes[0].trim().replace("\"", "");
                        String sensor = normalizarNombreSensor(sensorOriginal);

                        double coefA = parsearCoeficiente(partes[1]);
                        double coefB = parsearCoeficiente(partes[2]);
                        double coefC = parsearCoeficiente(partes[3]);
                        double coefD = parsearCoeficiente(partes[4]);

                        CoeficientesCorreccion coef = new CoeficientesCorreccion(sensor, coefA, coefB, coefC, coefD);
                        coeficientes.put(sensor, coef);
                        if (!sensor.equals(sensorOriginal)) coeficientes.put(sensorOriginal, coef);
                    } catch (Exception e) {
                        logger.warn("Error parseando línea de calibración: " + e.getMessage());
                    }
                }
            }
        }

        return coeficientes;
    }

    /**
     * Loads calibration coefficients by sensor name (legacy path)
     * Searches for the most recent calibration file for the given sensor
     */
    private Map<String, CoeficientesCorreccion> cargarCorreccionCSVPorSensorNombre(String sensorNombre) throws IOException {
        Map<String, CoeficientesCorreccion> coeficientes = new HashMap<>();
        
        try {
            // Try to find calibration correction record for this sensor
            java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones = 
                calibrationCorrectionRepositorio.findAll()
                    .stream()
                    .filter(c -> c.getSensor() != null && normalizarNombreSensor(c.getSensor().getCodigo()).equals(sensorNombre))
                    .sorted((a, b) -> b.getFechaSubida().compareTo(a.getFechaSubida()))
                    .limit(1)
                    .toList();
            
            if (correcciones.isEmpty()) {
                logger.debug("No calibration correction found for sensor: {}", sensorNombre);
                return coeficientes;
            }
            
            return cargarCorreccionCSV(correcciones.get(0));
        } catch (Exception e) {
            logger.debug("Error loading calibration CSV by sensor name {}: {}", sensorNombre, e.getMessage());
            return coeficientes;
        }
    }

    
    /**
     * Overload for CalibrationCorrection type
     * Converts CalibrationCorrection to Map of coefficients
     */
    private Map<String, CoeficientesCorreccion> cargarCorreccionCSV(com.sivco.gestion_archivos.modelos.CalibrationCorrection calibrationCorr) throws IOException {
        Map<String, CoeficientesCorreccion> coeficientes = new HashMap<>();
        
        if (calibrationCorr == null || calibrationCorr.getRutaArchivo() == null) {
            return coeficientes;
        }
        
        try {
            Path rutaArchivo = Paths.get(calibrationCorr.getRutaArchivo());
            if (!Files.exists(rutaArchivo)) {
                logger.warn("Archivo de corrección no encontrado: " + rutaArchivo);
                return coeficientes;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(rutaArchivo, StandardCharsets.UTF_8)) {
                String linea;
                boolean primeraLinea = true;
                int lineasProcesadas = 0;
                
                while ((linea = reader.readLine()) != null) {
                    lineasProcesadas++;
                    
                    if (primeraLinea) {
                        primeraLinea = false; // Saltar encabezado
                        logger.debug("Encabezado del archivo de corrección: " + linea);
                        continue;
                    }
                    
                    try {
                        // Support both legacy "timestamp,sensor,valor_corregido" and
                        // the coefficient CSV: "[sensor,]A,B,C,D,..." formats
                        String[] partes = splitCsvPreservingQuotes(linea);

                        if (partes.length >= 4) { // Necesitamos al menos A,B,C,D
                            String sensor;
                            int coefIndex = 0;
                            
                            // Verificar si la primera columna es un nombre de sensor
                            String primeraColumna = partes[0].trim().replace("\"", "");
                            boolean primeraColumnaEsSensor = false;
                            
                            // Intentar determinar si es un nombre de sensor
                            if (primeraColumna.matches("(?i)(sensor|T)\\s*\\d+") || 
                                primeraColumna.matches("(?i)sensor_\\d+") ||
                                (primeraColumna.length() > 0 && !primeraColumna.matches("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?"))) {
                                primeraColumnaEsSensor = true;
                                sensor = normalizarNombreSensor(primeraColumna);
                                coefIndex = 1;
                            } else {
                                // No hay columna de sensor, usar el sensor del CalibrationCorrection
                                sensor = normalizarNombreSensor(calibrationCorr.getSensor().getCodigo());
                                coefIndex = 0;
                            }
                            
                            // coefficient format: [sensor,]A,B,C,D,...
                            double coefA = parsearCoeficiente(partes[coefIndex]);
                            double coefB = parsearCoeficiente(partes[coefIndex + 1]);
                            double coefC = parsearCoeficiente(partes[coefIndex + 2]);
                            double coefD = parsearCoeficiente(partes[coefIndex + 3]);

                            CoeficientesCorreccion coef = new CoeficientesCorreccion(sensor, coefA, coefB, coefC, coefD);
                            try {
                                if (partes.length > coefIndex + 4) coef.setIndice(Integer.parseInt(partes[coefIndex + 4].trim().replace("\"", "")));
                                if (partes.length > coefIndex + 5) coef.setHumedad(parsearCoeficiente(partes[coefIndex + 5]));
                                if (partes.length > coefIndex + 6) coef.setLineal(parsearCoeficiente(partes[coefIndex + 6]));
                                if (partes.length > coefIndex + 7) coef.setCubica(parsearCoeficiente(partes[coefIndex + 7]));
                                if (partes.length > coefIndex + 8) coef.setCoLinHum(parsearCoeficiente(partes[coefIndex + 8]));
                            } catch (Exception ignore) {}

                            coeficientes.put(sensor, coef);
                            if (primeraColumnaEsSensor && !sensor.equals(primeraColumna)) coeficientes.put(primeraColumna, coef);
                        } else if (partes.length >= 3) {
                            // legacy format: timestamp,sensor,valor_corregido
                            String sensor = partes[1].trim().replace("\"", "");
                            String sensorNormalizad = normalizarNombreSensor(sensor);
                            double valorCorregido = Double.parseDouble(partes[2].trim().replace("\"", ""));

                            if (!coeficientes.containsKey(sensorNormalizad)) {
                                coeficientes.put(sensorNormalizad, new CoeficientesCorreccion());
                            }
                            coeficientes.get(sensorNormalizad).addValor(valorCorregido);
                        }
                    } catch (NumberFormatException e) {
                        logger.debug("Línea " + lineasProcesadas + " contiene formato inválido en corrección: " + e.getMessage());
                    }
                }
                
                logger.info("Cargadas correcciones desde CalibrationCorrection: {} sensores procesados", coeficientes.size());
            }
        } catch (Exception e) {
            logger.error("Error cargando correcciones desde CalibrationCorrection: " + e.getMessage(), e);
        }
        
        return coeficientes;
    }
    
    /**
     * Carga valores corregidos desde un archivo CSV de corrección
     * Formato esperado: [sensor,]A,B,C,D,...
     * Si no hay columna de sensor, usa el sensor del CalibrationCorrection
     */
    private Map<String, CoeficientesCorreccion> cargarCorreccionCSV(CorreccionEnsayo correccion) throws IOException {
        Map<String, CoeficientesCorreccion> coeficientes = new HashMap<>();
        
        try {
            Path rutaArchivo = Paths.get(correccion.getRutaArchivo());
            if (!Files.exists(rutaArchivo)) {
                logger.warn("Archivo de corrección no encontrado: " + rutaArchivo);
                return coeficientes;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(rutaArchivo, StandardCharsets.UTF_8)) {
                String linea;
                boolean primeraLinea = true;
                int lineasProcesadas = 0;
                
                while ((linea = reader.readLine()) != null) {
                    lineasProcesadas++;
                    
                    if (primeraLinea) {
                        primeraLinea = false; // Saltar encabezado
                        logger.debug("Encabezado del archivo de corrección: " + linea);
                        continue;
                    }
                    
                    // Parsear línea considerando que puede tener comillas
                        String[] partes = splitCsvPreservingQuotes(linea);
                    
                    if (partes.length >= 4) { // Necesitamos al menos A,B,C,D
                        try {
                            String sensor;
                            int coefIndex = 0;
                            
                            // Verificar si la primera columna es un nombre de sensor o directamente coeficientes
                            String primeraColumna = partes[0].trim().replace("\"", "");
                            boolean primeraColumnaEsSensor = false;
                            
                            // Intentar determinar si es un nombre de sensor
                            if (primeraColumna.matches("(?i)(sensor|T)\\s*\\d+") || 
                                primeraColumna.matches("(?i)sensor_\\d+") ||
                                primeraColumna.length() > 0 && !primeraColumna.matches("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?")) {
                                primeraColumnaEsSensor = true;
                                sensor = normalizarNombreSensor(primeraColumna);
                                coefIndex = 1;
                            } else {
                                // No hay columna de sensor, usar el sensor del ensayo (legacy format)
                                // Para CorreccionEnsayo, no tenemos sensor directo, así que usamos un valor genérico
                                sensor = "default";
                                coefIndex = 0;
                            }
                            
                            // Parsear coeficientes de manera segura
                            double coefA = parsearCoeficiente(partes[coefIndex]);
                            double coefB = parsearCoeficiente(partes[coefIndex + 1]);
                            double coefC = parsearCoeficiente(partes[coefIndex + 2]);
                            double coefD = parsearCoeficiente(partes[coefIndex + 3]);
                            
                            CoeficientesCorreccion coef = new CoeficientesCorreccion(sensor, coefA, coefB, coefC, coefD);
                            
                            // Agregar valores adicionales si están presentes (de forma segura)
                            try {
                                if (partes.length > coefIndex + 4) coef.setIndice(Integer.parseInt(partes[coefIndex + 4].trim().replace("\"", "")));
                                if (partes.length > coefIndex + 5) coef.setHumedad(parsearCoeficiente(partes[coefIndex + 5]));
                                if (partes.length > coefIndex + 6) coef.setLineal(parsearCoeficiente(partes[coefIndex + 6]));
                                if (partes.length > coefIndex + 7) coef.setCubica(parsearCoeficiente(partes[coefIndex + 7]));
                                if (partes.length > coefIndex + 8) coef.setCoLinHum(parsearCoeficiente(partes[coefIndex + 8]));
                            } catch (Exception e) {
                                // Ignorar errores en campos opcionales
                                logger.debug("Error parseando campos opcionales en línea " + lineasProcesadas + ": " + e.getMessage());
                            }
                            
                            // Agregar con clave original y normalizada
                            coeficientes.put(sensor, coef);
                            if (primeraColumnaEsSensor && !sensor.equals(primeraColumna)) {
                                coeficientes.put(primeraColumna, coef);
                            }
                            
                            logger.debug("Coeficientes cargados para " + sensor + ": A=" + coefA + ", B=" + coefB + ", C=" + coefC + ", D=" + coefD);
                            
                        } catch (NumberFormatException e) {
                            logger.warn("Error parseando coeficientes en línea " + lineasProcesadas + ": " + linea + " - " + e.getMessage());
                        } catch (Exception e) {
                            logger.warn("Error procesando línea " + lineasProcesadas + " de corrección: " + linea + " - " + e.getMessage());
                        }
                    } else {
                        logger.debug("Línea " + lineasProcesadas + " no tiene suficientes columnas (" + partes.length + "): " + linea);
                    }
                }
                
                logger.info("Procesadas " + lineasProcesadas + " líneas del archivo de corrección, cargados " + coeficientes.size() + " coeficientes");
            }
        } catch (Exception e) {
            logger.error("Error cargando archivo de corrección " + correccion.getNombreArchivo() + ": " + e.getMessage());
            // Retornar mapa vacío en caso de error
        }
        
        return coeficientes;
    }
    
    /**
     * Aplica corrección a un dato individual usando los coeficientes de corrección
     */
    private DatoEnsayoTemporal aplicarCorreccionIndividual(DatoEnsayoTemporal dato, Map<String, CoeficientesCorreccion> coeficientesCorreccion, Map<String, Long> calibrationIdPorSensor) {
        try {
            String sensor = dato.getSensor();
            if (sensor == null) {
                return dato;
            }
            
            // Buscar coeficientes para este sensor (intentar con sensor original y normalizado)
            CoeficientesCorreccion coeficientes = coeficientesCorreccion.get(sensor);
            
            // Si no se encuentra, intentar con versión normalizada
            if (coeficientes == null) {
                String sensorNormalizado = normalizarNombreSensor(sensor);
                coeficientes = coeficientesCorreccion.get(sensorNormalizado);
            }
            
            if (coeficientes != null) {
                double valorOriginal = dato.getValor();
                
                // Validar que el valor no sea NaN o Infinity
                if (Double.isNaN(valorOriginal) || Double.isInfinite(valorOriginal)) {
                    logger.warn("Valor inválido detectado en sensor " + sensor + ": " + valorOriginal);
                    return dato;
                }
                
                double correctionOffset = coeficientes.aplicarCorreccion(valorOriginal);
                double valorCorregido = valorOriginal + correctionOffset;
                
                // Validar resultado de la corrección
                if (Double.isNaN(valorCorregido) || Double.isInfinite(valorCorregido)) {
                    logger.warn("Corrección resultó en valor inválido para sensor " + sensor + 
                              ". Original: " + valorOriginal + ", Offset: " + correctionOffset + ", Final: " + valorCorregido);
                    return dato;
                }
                
                logger.debug("Aplicando corrección a " + sensor + ": " + valorOriginal + " + " + correctionOffset + " = " + valorCorregido + 
                           " (A=" + coeficientes.getCoeficienteA() + ", B=" + coeficientes.getCoeficienteB() + ")");
                
                // Crear una copia del dato con el valor corregido
                DatoEnsayoTemporal datoCorregido = new DatoEnsayoTemporal();
                datoCorregido.setId(dato.getId());
                datoCorregido.setEnsayoId(dato.getEnsayoId());
                datoCorregido.setTimestamp(dato.getTimestamp());
                datoCorregido.setValor(valorCorregido);
                datoCorregido.setSensor(dato.getSensor());
                datoCorregido.setFuente(dato.getFuente());
                datoCorregido.setNumeroSecuencia(dato.getNumeroSecuencia());
                datoCorregido.setAnormal(dato.getAnormal()); // Mantener el flag de anormal
                // Registrar la calibración aplicada (si existe)
                String keyNorm = normalizarNombreSensor(sensor);
                Long calId = calibrationIdPorSensor.getOrDefault(sensor, calibrationIdPorSensor.get(keyNorm));
                datoCorregido.setAppliedCalibrationId(calId);
                
                return datoCorregido;
            }
        } catch (Exception e) {
            logger.warn("Error aplicando corrección individual: " + e.getMessage());
            // En caso de error, devolver el dato original
        }
        
        // Si no hay coeficientes para este sensor, devolver el dato original
        return dato;
    }
    
    /**
     * Normaliza diferentes formatos de timestamp al formato estándar yyyy-MM-dd HH:mm:ss
     */
    private String normalizarTimestamp(String timestampRaw) {
        if (timestampRaw == null || timestampRaw.trim().isEmpty()) {
            return timestampRaw;
        }
        
        String timestamp = timestampRaw.trim();
        
        // Si ya está en el formato esperado, devolverlo
        if (timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return timestamp;
        }
        
        // Intentar diferentes formatos comunes
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                // Continuar con el siguiente formato
            }
        }
        
        // Si no se puede parsear, devolver el original
        return timestamp;
    }
    
    /**
     * Parsea un coeficiente de forma segura, manejando notación científica y diferentes formatos
     */
    private double parsearCoeficiente(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0.0;
        }
        String valorLimpio = valor.trim().replace("\"", "");
        if (valorLimpio.isEmpty()) {
            return 0.0;
        }
        // Aceptar notación con comas decimales en archivos locales
        valorLimpio = valorLimpio.replace(',', '.');
        return Double.parseDouble(valorLimpio);
    }
    
    /**
     * Normaliza el nombre del sensor para que coincida con el formato de los datos
     * Ejemplos:
     * "Sensor 2" -> "sensor_2"
     * "sensor_2" -> "sensor_2"
     * "SENSOR 3" -> "sensor_3"
     */
    private String normalizarNombreSensor(String sensorOriginal) {
        if (sensorOriginal == null || sensorOriginal.isEmpty()) {
            return sensorOriginal;
        }
        
        // Convertir a minúsculas y reemplazar espacios con guión bajo
        String normalizado = sensorOriginal.toLowerCase().trim();
        normalizado = normalizado.replace(" ", "_");
        
        // Si el sensor está en formato "sensor X" o "sensor_X", asegurarse de que sea "sensor_X"
        if (normalizado.matches("sensor_?\\d+")) {
            normalizado = normalizado.replace("sensor", "sensor_").replace("__", "_");
        }
        
        return normalizado;
    }

    private String[] splitCsvPreservingQuotes(String line) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        if (line == null) return new String[0];
        char delimiter = ',';
        int commaCount = 0;
        int semicolonCount = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ',') commaCount++;
            if (c == ';') semicolonCount++;
        }
        if (semicolonCount > commaCount) {
            delimiter = ';';
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
