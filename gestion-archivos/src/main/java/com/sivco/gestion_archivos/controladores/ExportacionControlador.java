package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.Reporte;
import com.sivco.gestion_archivos.servicios.ExcelServicio;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import com.sivco.gestion_archivos.servicios.ReporteServicio;
import com.sivco.gestion_archivos.servicios.AnalisisServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exportar")
@CrossOrigin(origins = "*")
public class ExportacionControlador {
    
    @Autowired
    private ExcelServicio excelServicio;
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @Autowired
    private ReporteServicio reporteServicio;
    
    @Autowired
    private AnalisisServicio analisisServicio;
    
    @GetMapping("/excel/datos/{ensayoId}")
    public ResponseEntity<?> exportarDatosExcel(@PathVariable Long ensayoId) {
        try {
            Ensayo ensayo = ensayoServicio.obtenerEnsayo(ensayoId)
                .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
            
            List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
            
            byte[] excelData = excelServicio.generarExcelDatos(datos, ensayo.getNombre());
            
            // Generar nombre de archivo usando el nombre del ensayo
            String nombreArchivo = ensayo.getNombre().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-_]", "")
                                                      .replaceAll("\\s+", "_")
                                                      .trim();
            if (nombreArchivo.isEmpty()) {
                nombreArchivo = "ensayo_" + ensayoId;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + nombreArchivo + "_datos_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx\"");
            headers.set(HttpHeaders.CONTENT_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
                
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al generar Excel: " + e.getMessage()));
        }
    }
    
    @GetMapping("/excel/reporte/{ensayoId}")
    public ResponseEntity<?> exportarReporteExcel(@PathVariable Long ensayoId) {
        try {
            Ensayo ensayo = ensayoServicio.obtenerEnsayo(ensayoId)
                .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
            
            List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
            
            double media = analisisServicio.calcularMedia(datos);
            double desviacion = analisisServicio.calcularDesviacionEstandar(datos);
            double maximo = analisisServicio.calcularMaximo(datos);
            double minimo = analisisServicio.calcularMinimo(datos);
            int anormales = analisisServicio.contarAnormales(datos);
            
            // Factor Histórico (si aplica)
            Double fh = null;
            Double z = null;
            if (ensayo.getMaquina().getCalcularFH() != null && ensayo.getMaquina().getCalcularFH()) {
                z = ensayo.getMaquina().getParametroZ() != null ? ensayo.getMaquina().getParametroZ() : 14.0;
                fh = analisisServicio.calcularFactorHistorico(datos, z);
            }
            
            byte[] excelData = excelServicio.generarExcelReporte(
                ensayo.getNombre(),
                ensayo.getMaquina().getNombre(),
                ensayo.getMaquina().getTipo(),
                maximo,
                minimo,
                media,
                desviacion,
                anormales,
                datos.size(),
                fh,
                z
            );
            
            // Generar nombre de archivo usando el nombre del ensayo
            String nombreArchivo = ensayo.getNombre().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-_]", "")
                                                      .replaceAll("\\s+", "_")
                                                      .trim();
            if (nombreArchivo.isEmpty()) {
                nombreArchivo = "ensayo_" + ensayoId;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + nombreArchivo + "_reporte_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx\"");
            headers.set(HttpHeaders.CONTENT_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
                
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al generar Excel: " + e.getMessage()));
        }
    }
    
    @GetMapping("/csv/{ensayoId}")
    public ResponseEntity<?> exportarCSV(@PathVariable Long ensayoId) {
        try {
            Ensayo ensayo = ensayoServicio.obtenerEnsayo(ensayoId)
                .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
            
            List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Secuencia,Timestamp,Valor,Anormal,Fuente\n");
            
            for (DatoEnsayoTemporal dato : datos) {
                csv.append(dato.getNumeroSecuencia()).append(",")
                    .append(dato.getTimestamp()).append(",")
                    .append(dato.getValor()).append(",")
                    .append(dato.getAnormal() ? "SÍ" : "NO").append(",")
                    .append(dato.getFuente()).append("\n");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"datos_ensayo_" + ensayoId + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv\"");
            headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al generar CSV: " + e.getMessage()));
        }
    }
}
