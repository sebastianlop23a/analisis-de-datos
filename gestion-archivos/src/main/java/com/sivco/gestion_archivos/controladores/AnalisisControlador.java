package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.modelos.EstadisticasEnsayo;
import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.servicios.AnalisisServicio;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analisis")
@CrossOrigin(origins = "*")
public class AnalisisControlador {
    
    @Autowired
    private AnalisisServicio analisisServicio;
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @GetMapping("/ensayo/{ensayoId}")
    public ResponseEntity<EstadisticasEnsayo> obtenerEstadisticas(@PathVariable Long ensayoId) {
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        Ensayo ensayo = ensayoServicio.obtenerEnsayo(ensayoId).orElse(null);
        
        EstadisticasEnsayo estadisticas = new EstadisticasEnsayo();
        estadisticas.setMedia(analisisServicio.calcularMedia(datos));
        estadisticas.setDesviacionEstandar(analisisServicio.calcularDesviacionEstandar(datos));
        estadisticas.setMaximo(analisisServicio.calcularMaximo(datos));
        estadisticas.setMinimo(analisisServicio.calcularMinimo(datos));
        estadisticas.setRango(analisisServicio.calcularRango(datos));
        estadisticas.setCoeficienteVariacion(analisisServicio.calcularCoeficienteVariacion(datos));
        estadisticas.setTotalDatos(datos.size());
        estadisticas.setDatosAnormales(analisisServicio.contarAnormales(datos));
        estadisticas.setPorcentajeAnormales(analisisServicio.calcularPorcentajeAnormales(datos));
        
        // Datos para Boxplot
        double q1 = analisisServicio.calcularQ1(datos);
        double q3 = analisisServicio.calcularQ3(datos);
        double mediana = analisisServicio.calcularMediana(datos);
        double media = estadisticas.getMedia();
        double maximo = estadisticas.getMaximo();
        double minimo = estadisticas.getMinimo();
        
        estadisticas.setQ1(q1);
        estadisticas.setQ3(q3);
        estadisticas.setMediana(mediana);
        
        // Distancias para análisis del boxplot
        estadisticas.setMediaMinusQ1(media - q1);      // (Media - Q1)
        estadisticas.setQ3MinusMedia(q3 - media);      // (Q3 - Media)
        estadisticas.setMaximoMinusQ3(maximo - q3);    // (Máximo - Q3)
        estadisticas.setQ1MinusMinimo(q1 - minimo);    // (Q1 - Mínimo)
        
        // Factor Histórico (si aplica)
        if (ensayo != null && ensayo.getMaquina() != null && 
            ensayo.getMaquina().getCalcularFH() != null && ensayo.getMaquina().getCalcularFH()) {
            double z = ensayo.getMaquina().getParametroZ() != null ? ensayo.getMaquina().getParametroZ() : 14.0;
            double fh = analisisServicio.calcularFactorHistorico(datos, z);
            estadisticas.setFactorHistorico(fh);
        }
        
        return ResponseEntity.ok(estadisticas);
    }
    
    @GetMapping("/ensayo/{ensayoId}/anormales")
    public ResponseEntity<List<DatoEnsayoTemporal>> obtenerDatosAnormales(@PathVariable Long ensayoId) {
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        List<DatoEnsayoTemporal> anormales = analisisServicio.obtenerAnormales(datos);
        return ResponseEntity.ok(anormales);
    }
    
    @GetMapping("/ensayo/{ensayoId}/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumen(@PathVariable Long ensayoId) {
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        
        return ResponseEntity.ok(Map.of(
            "totalDatos", datos.size(),
            "media", analisisServicio.calcularMedia(datos),
            "desviacionEstandar", analisisServicio.calcularDesviacionEstandar(datos),
            "maximo", analisisServicio.calcularMaximo(datos),
            "minimo", analisisServicio.calcularMinimo(datos),
            "datosAnormales", analisisServicio.contarAnormales(datos),
            "porcentajeAnormales", String.format("%.2f%%", analisisServicio.calcularPorcentajeAnormales(datos))
        ));
    }
}
