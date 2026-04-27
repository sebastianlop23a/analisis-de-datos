package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.*;
import com.sivco.gestion_archivos.repositorios.ReporteRepositorio;
import com.sivco.gestion_archivos.repositorios.EnsayoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteServicio {
    
    @Autowired
    private ReporteRepositorio reporteRepositorio;
    
    @Autowired
    private EnsayoRepositorio ensayoRepositorio;
    
    @Autowired
    private EnsayoServicio ensayoServicio;

    @Autowired
    private com.sivco.gestion_archivos.utilidades.GeneradorReporteFinal generadorReporteFinal;
    
    public Reporte generarReporte(Long ensayoId, TipoReporte tipo, String generadoPor) {
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        
        // Obtener datos temporales antes de finalizarlos
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        
        // Usar versión optimizada para PDF si el tipo es PDF
        String contenido;
        if (tipo == TipoReporte.PDF) {
            contenido = generadorReporteFinal.construirHtmlReporteParaPdf(ensayoId, ensayo);
        } else {
            contenido = generadorReporteFinal.construirHtmlReporte(ensayoId, ensayo);
        }
        
        // Crear objeto reporte
        Reporte reporte = new Reporte();
        reporte.setEnsayo(ensayo);
        reporte.setFechaGeneracion(LocalDateTime.now());
        reporte.setTipo(tipo);
        reporte.setContenido(contenido);
        reporte.setGeneradoPor(generadoPor);
        
        // Guardar reporte en BD
        return reporteRepositorio.save(reporte);
    }
    
    private String generarContenidoReporte(Ensayo ensayo, List<DatoEnsayoTemporal> datos, TipoReporte tipo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("========== REPORTE DE ENSAYO ==========\n");
        sb.append("Nombre del Ensayo: ").append(ensayo.getNombre()).append("\n");
        sb.append("Máquina: ").append(ensayo.getMaquina().getNombre()).append("\n");
        sb.append("Tipo de Máquina: ").append(ensayo.getMaquina().getTipo()).append("\n");
        sb.append("Fecha Inicio: ").append(ensayo.getFechaInicio()).append("\n");
        sb.append("Fecha Fin: ").append(ensayo.getFechaFin()).append("\n");
        sb.append("Estado: ").append(ensayo.getEstado().getDescripcion()).append("\n");
        sb.append("Responsable: ").append(ensayo.getResponsable()).append("\n");
        sb.append("\n");
        
        sb.append("========== ESTADÍSTICAS ==========\n");
        sb.append("Total de Registros: ").append(ensayo.getTotalRegistros()).append("\n");
        sb.append("Registros Anormales: ").append(ensayo.getRegistrosAnormales()).append("\n");
        sb.append("Temperatura Promedio: ").append(String.format("%.2f", ensayo.getTemperaturaPromedio())).append("\n");
        sb.append("Temperatura Máxima: ").append(String.format("%.2f", ensayo.getTemperaturaMaxima())).append("\n");
        sb.append("Temperatura Mínima: ").append(String.format("%.2f", ensayo.getTemperaturaMinima())).append("\n");
        sb.append("Límites de Operación: ").append(ensayo.getMaquina().getLimiteInferior())
            .append(" - ").append(ensayo.getMaquina().getLimiteSuperior())
            .append(" ").append(ensayo.getMaquina().getUnidadMedida()).append("\n");
        sb.append("\n");
        
        if (ensayo.getObservaciones() != null && !ensayo.getObservaciones().isEmpty()) {
            sb.append("========== OBSERVACIONES ==========\n");
            sb.append(ensayo.getObservaciones()).append("\n");
        }
        
        return sb.toString();
    }
    
    public Reporte obtenerReporte(Long ensayoId) {
        return reporteRepositorio.findByEnsayoId(ensayoId)
            .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
    }

    public java.util.List<java.util.Map<String, Object>> obtenerTodosLosReportes() {
        java.util.List<Reporte> todos = reporteRepositorio.findAll();
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (Reporte r : todos) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", r.getId());
            m.put("ensayoId", r.getEnsayo() != null ? r.getEnsayo().getId() : null);
            m.put("tipo", r.getTipo() != null ? r.getTipo().name() : null);
            m.put("generadoPor", r.getGeneradoPor());
            m.put("fechaGeneracion", r.getFechaGeneracion() != null ? r.getFechaGeneracion().format(fmt) : null);
            resultado.add(m);
        }
        return resultado;
    }
}
