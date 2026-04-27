package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SivcoLoggerPdfService {

    private static final Logger logger = LoggerFactory.getLogger(SivcoLoggerPdfService.class);

    // Patrones regex para extraer información del PDF
    private static final Pattern LOGGER_SN_PATTERN = Pattern.compile("LOGGER_SN[:\\s]+([\\w\\-]+)");
    private static final Pattern BEGIN_TIME_PATTERN = Pattern.compile("Begin Time[:\\s]+([\\d\\-]+\\s+[\\d:]+)");
    private static final Pattern END_TIME_PATTERN = Pattern.compile("End Time[:\\s]+([\\d\\-]+\\s+[\\d:]+)");
    private static final Pattern NUMBER_OF_POINTS_PATTERN = Pattern.compile("Number of Points[:\\s]+(\\d+)");
    private static final Pattern MIN_PATTERN = Pattern.compile("Min[:\\s]+([\\d\\./\\-]+)");
    private static final Pattern MAX_PATTERN = Pattern.compile("Max[:\\s]+([\\d\\./\\-]+)");
    private static final Pattern MEAN_PATTERN = Pattern.compile("Mean[:\\s]+([\\d\\./\\-]+)");
    
    // Patrón para parsear filas de datos
    private static final Pattern DATA_ROW_PATTERN = Pattern.compile(
        "^\\s*(\\d+)\\s+" +  // SN
        "([\\d\\-]+\\s+[\\d:]+)\\s+" + // Timestamp
        "(.+)$" // Resto de valores de sensores
    );

    /**
     * Extrae información y datos del PDF SIVCO-LOGGER
     */
    public SivcoLoggerData extractSivcoLoggerData(MultipartFile archivo) throws IOException {
        String pdfText = extractTextFromPdf(archivo);
        return parseSivcoLoggerData(pdfText);
    }
    
    /**
     * Parseador público del contenido del PDF SIVCO-LOGGER desde texto ya extraído
     */
    public SivcoLoggerData parseSivcoLoggerText(String pdfText) {
        return parseSivcoLoggerData(pdfText);
    }

    /**
     * Extrae las líneas de datos de temperatura del PDF
     */
    public List<DatoEnsayoTemporal> extractTemperatureData(String pdfText, Long ensayoId) {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        
        // Encontrar dónde comienzan los datos
        String[] lines = pdfText.split("\r?\n");
        boolean dataStarted = false;
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        };
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            String lower = line.toLowerCase();
            
            // Detectar el inicio de la sección de datos por distintos encabezados SIVCO
            if (!dataStarted && lower.contains("sn") && lower.contains("time") && (lower.contains("t1") || lower.contains("sensor_1") || lower.contains("t1-oc") || lower.contains("°c") || lower.contains("t1 oc"))) {
                dataStarted = true;
                continue;
            }
            
            String[] parts = line.split("\\s+");
            if (parts.length < 4) {
                continue;
            }
            
            boolean looksLikeDataRow = isPotentialDataRow(parts, formatters);
            if (!dataStarted && !looksLikeDataRow) {
                continue;
            }
            if (looksLikeDataRow) {
                dataStarted = true;
            }
            
            if (lower.startsWith("min") || lower.startsWith("max") || lower.startsWith("mean") || lower.startsWith("end") || lower.startsWith("begin") || lower.startsWith("number of points")) {
                continue;
            }
            
            try {
                Integer.parseInt(parts[0]);
                
                String timestampStr = parts[1] + " " + parts[2];
                LocalDateTime timestamp = parseTimestamp(timestampStr, formatters);
                if (timestamp == null) {
                    logger.debug("No se pudo parsear timestamp en línea de datos: {}", line);
                    continue;
                }
                
                int sensorIndex = 1;
                for (int i = 3; i < parts.length; i++) {
                    String valorStr = parts[i].trim();
                    if (valorStr.isEmpty()) {
                        continue;
                    }
                    try {
                        Double valor = Double.parseDouble(valorStr.replace(",", "."));
                        DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
                        dato.setEnsayoId(ensayoId);
                        dato.setTimestamp(timestamp);
                        dato.setValor(valor);
                        dato.setSensor("sensor_" + sensorIndex);
                        datos.add(dato);
                        sensorIndex++;
                    } catch (NumberFormatException e) {
                        logger.debug("No se pudo parsear valor de temperatura: {} en línea: {}", valorStr, line);
                        sensorIndex++;
                    }
                }
            } catch (NumberFormatException e) {
                logger.debug("No se pudo parsear SN en línea de datos: {}", line);
            }
        }
        
        logger.info("Se extrajeron {} datos de temperatura del PDF", datos.size());
        if (datos.isEmpty()) {
            String preview = pdfTextPreview(pdfText);
            logger.debug("No se extrajeron datos de temperatura. Texto extraído (preview):\n{}", preview);
        }
        return datos;
    }

    private String pdfTextPreview(String text) {
        if (text == null) {
            return "<null>";
        }
        return text.length() <= 1200 ? text : text.substring(0, 1200) + "...";
    }

    /**
     * Parseador principal del contenido del PDF SIVCO-LOGGER
     */
    private SivcoLoggerData parseSivcoLoggerData(String pdfText) {
        SivcoLoggerData data = new SivcoLoggerData();
        
        // Extraer Logger SN
        Matcher snMatcher = LOGGER_SN_PATTERN.matcher(pdfText);
        if (snMatcher.find()) {
            data.setLoggerSn(snMatcher.group(1).trim());
        }
        
        // Extraer Begin Time
        Matcher beginMatcher = BEGIN_TIME_PATTERN.matcher(pdfText);
        if (beginMatcher.find()) {
            data.setBeginTime(beginMatcher.group(1).trim());
        }
        
        // Extraer End Time
        Matcher endMatcher = END_TIME_PATTERN.matcher(pdfText);
        if (endMatcher.find()) {
            data.setEndTime(endMatcher.group(1).trim());
        }
        
        // Extraer Number of Points
        Matcher pointsMatcher = NUMBER_OF_POINTS_PATTERN.matcher(pdfText);
        if (pointsMatcher.find()) {
            try {
                data.setNumberOfPoints(Integer.parseInt(pointsMatcher.group(1).trim()));
            } catch (NumberFormatException e) {
                logger.warn("No se pudo parsear Number of Points");
            }
        }
        
        // Extraer estadísticas (Min, Max, Mean) por sensor
        extractStatistics(pdfText, data);
        
        logger.info("PDF parseado - Logger SN: {}, Puntos: {}, Desde: {} hasta: {}",
            data.getLoggerSn(), data.getNumberOfPoints(), data.getBeginTime(), data.getEndTime());
        
        return data;
    }

    /**
     * Extrae estadísticas de temperatura por sensor
     */
    private void extractStatistics(String pdfText, SivcoLoggerData data) {
        // Buscar secciones de Min, Max, Mean
        Pattern statsPattern = Pattern.compile(
            "(Min|Max|Mean)[:\\s]+([^\\n]+)"
        );
        
        Matcher matcher = statsPattern.matcher(pdfText);
        Map<String, List<Double>> stats = new HashMap<>();
        
        while (matcher.find()) {
            String statType = matcher.group(1).trim();
            String valuesStr = matcher.group(2).trim();
            
            // Parsear los valores numéricos de la línea
            List<Double> values = parseNumericValues(valuesStr);
            stats.put(statType, values);
        }
        
        if (!stats.isEmpty()) {
            data.setStatistics(stats);
            logger.info("Estadísticas extraídas. Min: {} valores, Max: {} valores, Mean: {} valores",
                stats.get("Min") != null ? stats.get("Min").size() : 0,
                stats.get("Max") != null ? stats.get("Max").size() : 0,
                stats.get("Mean") != null ? stats.get("Mean").size() : 0
            );
        }
    }

    /**
     * Parsea una cadena y extrae todos los valores numéricos (pueden ser decimales negativos)
     */
    private List<Double> parseNumericValues(String valuesStr) {
        List<Double> values = new ArrayList<>();
        
        // Patrón para números decimales, potencialmente negativos, en paréntesis o sin ellos
        Pattern numPattern = Pattern.compile("([\\-]?\\d+\\.?\\d*)");
        Matcher matcher = numPattern.matcher(valuesStr);
        
        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                values.add(value);
            } catch (NumberFormatException e) {
                // Ignorar si no se puede parsear
            }
        }
        
        return values;
    }

    private LocalDateTime parseTimestamp(String timestampStr, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(timestampStr, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean isPotentialDataRow(String[] parts, DateTimeFormatter[] formatters) {
        if (parts.length < 4) {
            return false;
        }
        try {
            Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        String timestampStr = parts[1] + " " + parts[2];
        return parseTimestamp(timestampStr, formatters) != null;
    }

    /**
     * Extrae texto del archivo PDF
     */
    private String extractTextFromPdf(MultipartFile archivo) throws IOException {
        try (InputStream in = archivo.getInputStream(); PDDocument document = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            logger.info("Texto extraído del PDF: {} caracteres", text.length());
            return text != null ? text : "";
            
        } catch (IOException e) {
            logger.error("Error extrayendo texto del PDF: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Clase para almacenar datos extraídos del PDF SIVCO-LOGGER
     */
    public static class SivcoLoggerData {
        private String loggerSn;
        private String beginTime;
        private String endTime;
        private Integer numberOfPoints;
        private Map<String, List<Double>> statistics; // Min, Max, Mean por sensor
        
        // Getters y Setters
        public String getLoggerSn() { return loggerSn; }
        public void setLoggerSn(String loggerSn) { this.loggerSn = loggerSn; }
        
        public String getBeginTime() { return beginTime; }
        public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
        
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        
        public Integer getNumberOfPoints() { return numberOfPoints; }
        public void setNumberOfPoints(Integer numberOfPoints) { this.numberOfPoints = numberOfPoints; }
        
        public Map<String, List<Double>> getStatistics() { return statistics; }
        public void setStatistics(Map<String, List<Double>> statistics) { this.statistics = statistics; }
        
        @Override
        public String toString() {
            return "SivcoLoggerData{" +
                "loggerSn='" + loggerSn + '\'' +
                ", beginTime='" + beginTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", numberOfPoints=" + numberOfPoints +
                ", statistics=" + (statistics != null ? statistics.size() : 0) + " items" +
                '}';
        }
    }
}
