package com.sivco.gestion_archivos.utilidades;

import com.sivco.gestion_archivos.modelos.*;
import com.sivco.gestion_archivos.servicios.AnalisisServicio;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import com.sivco.gestion_archivos.servicios.CorreccionEnsayoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GeneradorReporteFinal {
    
    @Autowired
    private AnalisisServicio analisisServicio;
    
    @Autowired
    private EnsayoServicio ensayoServicio;

    @Autowired
    private CorreccionEnsayoServicio correccionEnsayoServicio;
    
    public ReporteFinal construirReporte(Long ensayoId, Ensayo ensayo) {
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        
        ReporteFinal reporte = new ReporteFinal();
        reporte.setEnsayoId(ensayoId);
        reporte.setNombreEnsayo(ensayo.getNombre());
        reporte.setNombreMaquina(ensayo.getMaquina().getNombre());
        reporte.setTipoMaquina(ensayo.getMaquina().getTipo());
        reporte.setFechaInicio(ensayo.getFechaInicio());
        reporte.setFechaFin(ensayo.getFechaFin());
        reporte.setResponsable(ensayo.getResponsable());
        reporte.setEstado(ensayo.getEstado().getDescripcion());
        
        // Estadísticas
        reporte.setTotalDatos(datos.size());
        reporte.setDatosAnormales(analisisServicio.contarAnormales(datos));
        reporte.setMedia(analisisServicio.calcularMedia(datos));
        reporte.setDesviacionEstandar(analisisServicio.calcularDesviacionEstandar(datos));
        reporte.setMaximo(analisisServicio.calcularMaximo(datos));
        reporte.setMinimo(analisisServicio.calcularMinimo(datos));
        reporte.setRango(analisisServicio.calcularRango(datos));
        reporte.setCoeficienteVariacion(analisisServicio.calcularCoeficienteVariacion(datos));
        reporte.setPorcentajeAnormales(analisisServicio.calcularPorcentajeAnormales(datos));
        
        // Estadísticos avanzados
        reporte.setErrorEstandar(analisisServicio.calcularErrorEstandar(datos));
        double valorT = 2.0; // Valor por defecto, puede ser configurable
        reporte.setValorT(valorT);
        reporte.setLimiteConfianzaInferior(analisisServicio.calcularLimiteConfianzaInferior(datos, valorT));
        reporte.setLimiteConfianzaSuperior(analisisServicio.calcularLimiteConfianzaSuperior(datos, valorT));
        
        // Detectar eventos: cortes de energía y aperturas de puerta
        // Parámetros: umbralCaida=5°C, duracionMinima=5min para cortes
        reporte.setCortesEnergia(analisisServicio.detectarCortesEnergia(datos, 5.0, 5));
        // Parámetros: umbralSubida=3°C, duracionMaxima=15min para aperturas
        reporte.setAperturasPuerta(analisisServicio.detectarAperturasPuerta(datos, 3.0, 15));
        
        // Límites de la máquina
        reporte.setLimiteInferior(ensayo.getMaquina().getLimiteInferior());
        reporte.setLimiteSuperior(ensayo.getMaquina().getLimiteSuperior());
        
        // Factor Histórico
        reporte.setCalculaFH(ensayo.getMaquina().getCalcularFH());
        reporte.setParametroZ(ensayo.getMaquina().getParametroZ());
        if (ensayo.getMaquina().getCalcularFH() != null && ensayo.getMaquina().getCalcularFH()) {
            double z = ensayo.getMaquina().getParametroZ() != null ? ensayo.getMaquina().getParametroZ() : 14.0;
            double fh = analisisServicio.calcularFactorHistorico(datos, z);
            reporte.setFactorHistorico(fh);
        }
        
        reporte.setObservaciones(ensayo.getObservaciones());
        
        return reporte;
    }

    /**
     * Genera HTML optimizado para conversión a PDF
     * - Sin JavaScript ni Canvas
     * - CSS simplificado compatible con OpenHTMLtoPDF
     * - Sin elementos interactivos
     */
    public String construirHtmlReporteParaPdf(Long ensayoId, Ensayo ensayo) {
        ReporteFinal base = construirReporte(ensayoId, ensayo);
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        
        // Calcular cuartiles
        List<Double> valoresOrdenados = datos.stream()
            .map(DatoEnsayoTemporal::getValor)
            .sorted()
            .collect(Collectors.toList());
        
        double q1 = calcularCuartil(valoresOrdenados, 0.25);
        double q2 = calcularCuartil(valoresOrdenados, 0.50);
        double q3 = calcularCuartil(valoresOrdenados, 0.75);
        
        // Agrupar por sensor
        Map<String, List<DatoEnsayoTemporal>> datosPorSensor = datos.stream()
            .collect(Collectors.groupingBy(d -> d.getSensor() != null ? d.getSensor() : "Sin Sensor"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>").append(escaparHtml(base.getNombreEnsayo())).append("</title>\n");
        html.append("<style>\n");
        html.append("@page { size: A4; margin: 20mm; }\n");
        html.append("* { margin: 0; padding: 0; }\n");
        html.append("body { font-family: Arial, sans-serif; color: #333; font-size: 10pt; line-height: 1.4; }\n");
        html.append(".container { width: 100%; }\n");
        html.append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding: 8px 0; font-size: 18pt; margin-bottom: 15px; }\n");
        html.append("h2 { color: #34495e; margin-top: 20px; border-left: 3px solid #3498db; padding-left: 8px; font-size: 14pt; margin-bottom: 10px; }\n");
        html.append("h3 { color: #555; font-size: 12pt; margin: 10px 0; }\n");
        html.append(".info-section { background: #f5f5f5; padding: 10px; margin: 10px 0; border: 1px solid #ddd; }\n");
        html.append(".info-section p { margin: 3px 0; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 9pt; }\n");
        html.append("th { background: #34495e; color: white; padding: 8px; text-align: left; font-weight: bold; }\n");
        html.append("td { padding: 6px 8px; border-bottom: 1px solid #ddd; }\n");
        html.append("tr:nth-child(even) { background: #f9f9f9; }\n");
        html.append(".stat-box { background: #e3f2fd; padding: 8px; margin: 5px 0; border-left: 3px solid #2196f3; }\n");
        html.append(".stat-box strong { color: #1976d2; }\n");
        html.append(".page-break { page-break-after: always; }\n");
        html.append(".highlight { background: #fff3cd; font-weight: bold; }\n");
        html.append(".sensor-section { background: #f8f9fa; padding: 10px; margin: 10px 0; border-left: 3px solid #17a2b8; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"container\">\n");
        
        // Título
        html.append("<h1>REPORTE COMPLETO DE ENSAYO</h1>\n");
        
        // Información
        html.append("<h2>Informacion del Ensayo</h2>\n");
        html.append("<div class=\"info-section\">\n");
        html.append("<p><strong>Ensayo:</strong> ").append(escaparHtml(base.getNombreEnsayo())).append("</p>\n");
        html.append("<p><strong>Maquina:</strong> ").append(escaparHtml(base.getNombreMaquina())).append(" (").append(escaparHtml(base.getTipoMaquina())).append(")</p>\n");
        html.append("<p><strong>Periodo:</strong> ").append(base.getFechaInicio()).append(" a ").append(base.getFechaFin()).append("</p>\n");
        html.append("<p><strong>Responsable:</strong> ").append(escaparHtml(base.getResponsable())).append("</p>\n");
        html.append("</div>\n");
        
        // Estadísticas - Usando divs en lugar de grid
        html.append("<h2>Estadisticas Principales</h2>\n");
        html.append("<div class=\"stat-box\"><strong>Total de Registros:</strong> ").append(base.getTotalDatos()).append("</div>\n");
        html.append("<div class=\"stat-box\"><strong>Media:</strong> ").append(String.format("%.2f", base.getMedia())).append("</div>\n");
        html.append("<div class=\"stat-box\"><strong>Desviacion Estandar:</strong> ").append(String.format("%.2f", base.getDesviacionEstandar())).append("</div>\n");
        html.append("<div class=\"stat-box\"><strong>Minimo:</strong> ").append(String.format("%.2f", base.getMinimo())).append("</div>\n");
        html.append("<div class=\"stat-box\"><strong>Maximo:</strong> ").append(String.format("%.2f", base.getMaximo())).append("</div>\n");
        html.append("<div class=\"stat-box\"><strong>Rango:</strong> ").append(String.format("%.2f", base.getRango())).append("</div>\n");
        html.append("<div class=\"stat-box\"><strong>Registros Anormales:</strong> ").append(base.getDatosAnormales()).append(" (").append(String.format("%.2f", base.getPorcentajeAnormales())).append("%)</div>\n");
        
        // Tabla de Estadísticas Detallada
        html.append("<h2>Tabla de Estadisticas Detallada</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Metrica</th><th>Valor</th></tr>\n");
        html.append("<tr><td>Total de Registros</td><td>").append(base.getTotalDatos()).append("</td></tr>\n");
        html.append("<tr><td>Registros Anormales</td><td>").append(base.getDatosAnormales()).append(" (").append(String.format("%.2f", base.getPorcentajeAnormales())).append("%)</td></tr>\n");
        html.append("<tr><td>Media</td><td>").append(String.format("%.4f", base.getMedia())).append("</td></tr>\n");
        html.append("<tr><td>Desviacion Estandar</td><td>").append(String.format("%.4f", base.getDesviacionEstandar())).append("</td></tr>\n");
        html.append("<tr><td>Q1 (25%)</td><td>").append(String.format("%.4f", q1)).append("</td></tr>\n");
        html.append("<tr><td>Mediana (Q2)</td><td>").append(String.format("%.4f", q2)).append("</td></tr>\n");
        html.append("<tr><td>Q3 (75%)</td><td>").append(String.format("%.4f", q3)).append("</td></tr>\n");
        html.append("<tr><td>Valor Minimo</td><td>").append(String.format("%.4f", base.getMinimo())).append("</td></tr>\n");
        html.append("<tr><td>Valor Maximo</td><td>").append(String.format("%.4f", base.getMaximo())).append("</td></tr>\n");
        html.append("<tr><td>Rango</td><td>").append(String.format("%.4f", base.getRango())).append("</td></tr>\n");
        html.append("<tr><td>IQR (Q3-Q1)</td><td>").append(String.format("%.4f", q3 - q1)).append("</td></tr>\n");
        html.append("<tr><td>Limite Inferior</td><td>").append(String.format("%.4f", base.getLimiteInferior())).append("</td></tr>\n");
        html.append("<tr><td>Limite Superior</td><td>").append(String.format("%.4f", base.getLimiteSuperior())).append("</td></tr>\n");
        
        // Error estándar y límites de confianza
        if (base.getErrorEstandar() != null) {
            html.append("<tr class=\"highlight\"><td>Error Estandar (SE)</td><td>").append(String.format("%.6f", base.getErrorEstandar())).append("</td></tr>\n");
            if (base.getValorT() != null) {
                html.append("<tr class=\"highlight\"><td>Valor t</td><td>").append(String.format("%.2f", base.getValorT())).append("</td></tr>\n");
            }
            if (base.getLimiteConfianzaInferior() != null && base.getLimiteConfianzaSuperior() != null) {
                html.append("<tr class=\"highlight\"><td>Limite Confianza Inferior</td><td>").append(String.format("%.4f", base.getLimiteConfianzaInferior())).append("</td></tr>\n");
                html.append("<tr class=\"highlight\"><td>Limite Confianza Superior</td><td>").append(String.format("%.4f", base.getLimiteConfianzaSuperior())).append("</td></tr>\n");
                html.append("<tr class=\"highlight\"><td colspan=\"2\"><em>Formula: Media ± (t × Error Estandar), donde SE = σ/√n</em></td></tr>\n");
            }
        }
        
        // Factor Histórico (si aplica)
        if (base.getCalculaFH() != null && base.getCalculaFH() && base.getFactorHistorico() != null) {
            html.append("<tr class=\"highlight\"><td>Factor Historico (FH)</td><td>").append(String.format("%.6f", base.getFactorHistorico())).append("</td></tr>\n");
            html.append("<tr class=\"highlight\"><td>Parametro Z</td><td>").append(base.getParametroZ()).append("</td></tr>\n");
            html.append("<tr class=\"highlight\"><td colspan=\"2\"><em>Formula: FH = Suma(10^((Ti - 250)/z) * Delta-t)</em></td></tr>\n");
        }
        
        html.append("</table>\n");
        
        // === COMPARACIÓN ENTRE SENSORES ===
        if (datosPorSensor.size() > 1) {
            html.append("<div class=\"page-break\"></div>\n");
            html.append("<h2>Comparacion Entre Sensores</h2>\n");
            
            html.append("<table>\n");
            html.append("<tr><th>Sensor</th><th>Registros</th><th>Media</th><th>Min</th><th>Max</th><th>Diferencia (Max-Min)</th><th>Desv. Est.</th><th>Anormales</th></tr>\n");
            
            for (String sensor : datosPorSensor.keySet()) {
                List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
                double mediaSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).average().orElse(0);
                double minSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).min().orElse(0);
                double maxSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).max().orElse(0);
                double diferencia = maxSensor - minSensor;
                
                // Calcular desviación estándar del sensor
                double mediaSensorFinal = mediaSensor;
                double varianza = datosSensor.stream()
                    .mapToDouble(d -> Math.pow(d.getValor() - mediaSensorFinal, 2))
                    .average().orElse(0);
                double desvSensor = Math.sqrt(varianza);
                
                long anormalesSensor = datosSensor.stream().filter(d -> d.getAnormal() != null && d.getAnormal()).count();
                
                html.append("<tr>");
                html.append("<td><strong>").append(escaparHtml(sensor)).append("</strong></td>");
                html.append("<td>").append(datosSensor.size()).append("</td>");
                html.append("<td>").append(String.format("%.4f", mediaSensor)).append("</td>");
                html.append("<td>").append(String.format("%.4f", minSensor)).append("</td>");
                html.append("<td>").append(String.format("%.4f", maxSensor)).append("</td>");
                html.append("<td>").append(String.format("%.4f", diferencia)).append("</td>");
                html.append("<td>").append(String.format("%.4f", desvSensor)).append("</td>");
                html.append("<td>").append(anormalesSensor).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
            
            // Análisis de diferencias entre sensores
            double mediaGeneral = base.getMedia();
            html.append("<h3>Analisis de Desviaciones Respecto a la Media General</h3>\n");
            html.append("<table>\n");
            html.append("<tr><th>Sensor</th><th>Media del Sensor</th><th>Media General</th><th>Desviacion Absoluta</th><th>Desviacion Relativa (%)</th></tr>\n");
            
            for (String sensor : datosPorSensor.keySet()) {
                List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
                double mediaSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).average().orElse(0);
                double desviacionAbs = mediaSensor - mediaGeneral;
                double desviacionRel = (mediaGeneral != 0) ? (desviacionAbs / mediaGeneral) * 100 : 0;
                
                html.append("<tr>");
                html.append("<td><strong>").append(escaparHtml(sensor)).append("</strong></td>");
                html.append("<td>").append(String.format("%.4f", mediaSensor)).append("</td>");
                html.append("<td>").append(String.format("%.4f", mediaGeneral)).append("</td>");
                html.append("<td style=\"color: ").append(desviacionAbs > 0 ? "red" : "blue").append(";\">")
                    .append(String.format("%+.4f", desviacionAbs)).append("</td>");
                html.append("<td style=\"color: ").append(Math.abs(desviacionRel) > 5 ? "red" : "green").append(";\">")
                    .append(String.format("%+.2f%%", desviacionRel)).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        // === EVENTOS DETECTADOS: CORTES DE ENERGÍA ===
        if (base.getCortesEnergia() != null && !base.getCortesEnergia().isEmpty()) {
            html.append("<div class=\"page-break\"></div>\n");
            html.append("<h2>Eventos de Corte de Energia Detectados</h2>\n");
            html.append("<p><strong>Total de cortes detectados:</strong> ").append(base.getCortesEnergia().size()).append("</p>\n");
            html.append("<p><em>Criterio: Caidas >= 5°C con duracion >= 5 minutos</em></p>\n");
            
            html.append("<table>\n");
            html.append("<tr><th>#</th><th>Inicio</th><th>Fin</th><th>Duracion (min)</th><th>Temp. Antes</th><th>Temp. Minima</th><th>Temp. Despues</th><th>Caida (°C)</th></tr>\n");
            
            int numCorte = 1;
            for (com.sivco.gestion_archivos.modelos.EventoCorteEnergia evento : base.getCortesEnergia()) {
                double caida = evento.getTemperaturaAntes() - evento.getTemperaturaMinima();
                html.append("<tr>");
                html.append("<td>").append(numCorte++).append("</td>");
                html.append("<td>").append(evento.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))).append("</td>");
                html.append("<td>").append(evento.getFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))).append("</td>");
                html.append("<td>").append(evento.getDuracionMinutos()).append("</td>");
                html.append("<td>").append(String.format("%.2f", evento.getTemperaturaAntes())).append("</td>");
                html.append("<td style=\"color: red;\"><strong>").append(String.format("%.2f", evento.getTemperaturaMinima())).append("</strong></td>");
                html.append("<td>").append(String.format("%.2f", evento.getTemperaturaDespues())).append("</td>");
                html.append("<td style=\"color: red;\"><strong>").append(String.format("%.2f", caida)).append("</strong></td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        // === EVENTOS DETECTADOS: APERTURAS DE PUERTA ===
        if (base.getAperturasPuerta() != null && !base.getAperturasPuerta().isEmpty()) {
            html.append("<h2>Eventos de Apertura de Puerta Detectados</h2>\n");
            html.append("<p><strong>Total de aperturas detectadas:</strong> ").append(base.getAperturasPuerta().size()).append("</p>\n");
            html.append("<p><em>Criterio: Subidas >= 3°C con duracion <= 15 minutos</em></p>\n");
            
            html.append("<table>\n");
            html.append("<tr><th>#</th><th>Inicio</th><th>Fin</th><th>Duracion (min)</th><th>Temp. Antes</th><th>Temp. Maxima</th><th>Temp. Despues</th><th>Subida (°C)</th></tr>\n");
            
            int numApertura = 1;
            for (com.sivco.gestion_archivos.modelos.EventoCorteEnergia evento : base.getAperturasPuerta()) {
                double subida = evento.getTemperaturaMinima() - evento.getTemperaturaAntes(); // En aperturas, "minima" guarda el máximo
                html.append("<tr>");
                html.append("<td>").append(numApertura++).append("</td>");
                html.append("<td>").append(evento.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))).append("</td>");
                html.append("<td>").append(evento.getFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))).append("</td>");
                html.append("<td>").append(evento.getDuracionMinutos()).append("</td>");
                html.append("<td>").append(String.format("%.2f", evento.getTemperaturaAntes())).append("</td>");
                html.append("<td style=\"color: orange;\"><strong>").append(String.format("%.2f", evento.getTemperaturaMinima())).append("</strong></td>");
                html.append("<td>").append(String.format("%.2f", evento.getTemperaturaDespues())).append("</td>");
                html.append("<td style=\"color: orange;\"><strong>").append(String.format("%.2f", subida)).append("</strong></td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        // Análisis por Sensor (detalle individual)
        html.append("<div class=\"page-break\"></div>\n");
        html.append("<h2>Analisis Detallado por Sensor</h2>\n");
        for (String sensor : datosPorSensor.keySet()) {
            List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
            double mediaSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).average().orElse(0);
            double minSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).min().orElse(0);
            double maxSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).max().orElse(0);
            long anormalesSensor = datosSensor.stream().filter(d -> d.getAnormal() != null && d.getAnormal()).count();
            
            html.append("<div class=\"sensor-section\">\n");
            html.append("<h3>Sensor: ").append(escaparHtml(sensor)).append("</h3>\n");
            html.append("<p><strong>Registros:</strong> ").append(datosSensor.size()).append(" | ");
            html.append("<strong>Media:</strong> ").append(String.format("%.2f", mediaSensor)).append(" | ");
            html.append("<strong>Minimo:</strong> ").append(String.format("%.2f", minSensor)).append(" | ");
            html.append("<strong>Maximo:</strong> ").append(String.format("%.2f", maxSensor)).append(" | ");
            html.append("<strong>Anormales:</strong> ").append(anormalesSensor).append("</p>\n");
            html.append("</div>\n");
        }
        
        // Datos Anormales (si existen)
        List<DatoEnsayoTemporal> anormales = datos.stream()
            .filter(d -> d.getAnormal() != null && d.getAnormal())
            .collect(Collectors.toList());
        
        if (!anormales.isEmpty()) {
            html.append("<div class=\"page-break\"></div>\n");
            html.append("<h2>Registros Anormales Detectados</h2>\n");
            html.append("<table>\n");
            html.append("<tr><th>Secuencia</th><th>Timestamp</th><th>Valor</th><th>Fuente</th></tr>\n");
            
            int count = 0;
            for (DatoEnsayoTemporal dato : anormales) {
                if (count >= 100) {
                    html.append("<tr><td colspan=\"4\"><em>... y ").append(anormales.size() - 100).append(" mas</em></td></tr>\n");
                    break;
                }
                html.append("<tr><td>").append(dato.getNumeroSecuencia()).append("</td>");
                html.append("<td>").append(dato.getTimestamp()).append("</td>");
                html.append("<td>").append(String.format("%.2f", dato.getValor())).append("</td>");
                html.append("<td>").append(escaparHtml(dato.getFuente() != null ? dato.getFuente() : "-")).append("</td></tr>\n");
                count++;
            }
            html.append("</table>\n");
        }
        
        // Observaciones
        if (base.getObservaciones() != null && !base.getObservaciones().isEmpty()) {
            html.append("<h2>Observaciones</h2>\n");
            html.append("<div class=\"info-section\">\n");
            html.append("<p>").append(escaparHtml(base.getObservaciones())).append("</p>\n");
            html.append("</div>\n");
        }
        
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    // Método auxiliar para escapar caracteres HTML
    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("á", "&aacute;")
            .replace("é", "&eacute;")
            .replace("í", "&iacute;")
            .replace("ó", "&oacute;")
            .replace("ú", "&uacute;")
            .replace("ñ", "&ntilde;")
            .replace("Á", "&Aacute;")
            .replace("É", "&Eacute;")
            .replace("Í", "&Iacute;")
            .replace("Ó", "&Oacute;")
            .replace("Ú", "&Uacute;")
            .replace("Ñ", "&Ntilde;");
    }

    public String construirHtmlReporte(Long ensayoId, Ensayo ensayo) {
        ReporteFinal base = construirReporte(ensayoId, ensayo);
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        
        // Obtener correcciones aplicadas
        java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones = obtenerCorrecciones(datos);
        
        // Calcular cuartiles
        List<Double> valoresOrdenados = datos.stream()
            .map(DatoEnsayoTemporal::getValor)
            .sorted()
            .collect(Collectors.toList());
        double q1 = calcularCuartil(valoresOrdenados, 0.25);
        double q2 = calcularCuartil(valoresOrdenados, 0.50);
        double q3 = calcularCuartil(valoresOrdenados, 0.75);
        
        // Agrupar por sensor
        Map<String, List<DatoEnsayoTemporal>> datosPorSensor = datos.stream()
            .collect(Collectors.groupingBy(d -> d.getSensor() != null ? d.getSensor() : "Sin Sensor"));

        StringBuilder html = new StringBuilder();
        
        // Construir estructura HTML
        html.append(generarDocumentoHTML(base, datos, q1, q2, q3, datosPorSensor, correcciones));
        
        return html.toString();
    }

    /**
     * Obtiene las correcciones aplicadas a los datos del ensayo
     */
    private java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> obtenerCorrecciones(List<DatoEnsayoTemporal> datos) {
        List<Long> calibrationIds = datos.stream()
            .map(DatoEnsayoTemporal::getAppliedCalibrationId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones = new java.util.ArrayList<>();
        if (calibrationIds.isEmpty()) return correcciones;

        try {
            var ctx = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
            if (ctx == null) return correcciones;
            
            com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio repo = null;
            com.sivco.gestion_archivos.servicios.CalibrationCorrectionServicio calServ = null;
            
            try { repo = ctx.getBean(com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio.class); } catch (Exception ignore) {}
            try { calServ = ctx.getBean(com.sivco.gestion_archivos.servicios.CalibrationCorrectionServicio.class); } catch (Exception ignore) {}

            for (Long cid : calibrationIds) {
                try {
                    if (repo != null) {
                        repo.findById(cid).ifPresent(correcciones::add);
                    } else if (calServ != null) {
                        java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> hist = calServ.historyForSensor(cid);
                        if (hist != null && !hist.isEmpty()) correcciones.addAll(hist);
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
        
        return correcciones;
    }

    /**
     * Genera el documento HTML completo
     */
    private String generarDocumentoHTML(ReporteFinal base, List<DatoEnsayoTemporal> datos, 
            double q1, double q2, double q3, Map<String, List<DatoEnsayoTemporal>> datosPorSensor,
            java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones) {
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"es\">\n");
        html.append(generarHead(base));
        html.append("<body>\n<div class=\"container\">\n");
        
        // Contenido principal
        html.append(generarTituloEncabezado());
        html.append(generarResumenEjecutivo(base));
        html.append(generarSeccionResumenMetricas(base, q1, q2, q3));
        html.append(generarSeccionInformacion(base));
        html.append(generarSeccionEstadisticas(base, q1, q2, q3));
        html.append(generarTablaEstadisticas(base, q1, q2, q3));
        html.append(generarSeccionGraficas(base, datos, q1, q2, q3, datosPorSensor));
        html.append(generarSeccionSensores(datosPorSensor));
        html.append(generarSeccionCorrecciones(correcciones));
        html.append(generarFooter());
        
        html.append("</div>\n</body>\n</html>\n");
        return html.toString();
    }

    private String generarHead(ReporteFinal base) {
        StringBuilder head = new StringBuilder();
        head.append("<head>\n");
        head.append("  <meta charset=\"UTF-8\">\n");
        head.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        head.append("  <title>Reporte - ").append(base.getNombreEnsayo()).append("</title>\n");
        head.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js\"></script>\n");
        head.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-zoom/2.1.0/chartjs-plugin-zoom.min.js\"></script>\n");
        head.append(generarCSS());
        head.append("</head>\n");
        return head.toString();
    }

    private String generarCSS() {
        return "  <style>\n" +
            "    * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #e2e8f0; background: #0b1220; }\n" +
            "    .container { max-width: 1160px; margin: 24px auto 48px; background: #111827; padding: 30px 34px 40px; border-radius: 20px; border: 1px solid #1f2937; box-shadow: 0 20px 60px rgba(15, 23, 42, 0.45); }\n" +
            "    h1 { color: #f8fafc; border-bottom: 4px solid #2563eb; padding-bottom: 14px; margin-bottom: 20px; font-size: 34px; letter-spacing: 0.8px; }\n" +
            "    h2 { color: #f8fafc; margin-top: 34px; border-left: 5px solid #2563eb; padding-left: 14px; font-size: 20px; margin-bottom: 16px; }\n" +
            "    h3 { color: #cbd5e1; margin-bottom: 12px; }\n" +
            "    .info-section { background: #111827; padding: 20px; border-radius: 18px; margin: 18px 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; border: 1px solid #1f2937; }\n" +
            "    .info-section p { line-height: 1.75; color: #cbd5e1; font-size: 14px; }\n" +
            "    .badge { display: inline-block; padding: 6px 14px; border-radius: 999px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; }\n" +
            "    .badge-success { background: #22c55e; color: #0f172a; }\n" +
            "    .badge-warning { background: #facc15; color: #0f172a; }\n" +
            "    .summary-cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin: 20px 0 26px; }\n" +
            "    .summary-card { background: #0f172a; border: 1px solid #1f2937; padding: 18px 20px; border-radius: 18px; box-shadow: inset 0 1px 0 rgba(255,255,255,0.04); }\n" +
            "    .summary-card h4 { font-size: 13px; color: #94a3b8; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.04em; }\n" +
            "    .summary-card p { font-size: 22px; color: #f8fafc; font-weight: 700; margin: 0; }\n" +
            "    .metric-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin: 18px 0 26px; }\n" +
            "    .metric-card { background: #0f172a; border: 1px solid #1f2937; border-radius: 18px; padding: 18px 16px; color: #e2e8f0; box-shadow: inset 0 1px 0 rgba(255,255,255,0.04); }\n" +
            "    .metric-card strong { display: block; font-size: 12px; color: #94a3b8; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.06em; }\n" +
            "    .metric-card span { display: block; font-size: 28px; font-weight: 700; margin-top: 6px; color: #f8fafc; }\n" +
            "    .metric-card.emphasis { background: linear-gradient(135deg, #1e3a8a, #0f172a); border-color: #2563eb; }\n" +
            "    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 18px 0; }\n" +
            "    .stat-box { background: linear-gradient(135deg, #3b82f6, #2563eb); color: white; padding: 16px; border-radius: 14px; text-align: center; min-height: 100px; display: flex; flex-direction: column; justify-content: center; }\n" +
            "    .stat-value { font-size: 20px; font-weight: bold; }\n" +
            "    .stat-label { font-size: 11px; margin-top: 6px; opacity: 0.95; }\n" +
            "    .chart-container { position: relative; width: 100%; height: 350px; margin: 30px 0; padding: 20px; border: 1px solid #1f2937; border-radius: 18px; background: #0f172a; }\n" +
            "    .chart-zoom-info { background: #111827; border: 1px solid #1f2937; color: #cbd5e1; padding: 12px; border-radius: 12px; margin-bottom: 12px; font-size: 13px; }\n" +
            "    .chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }\n" +
            "    .chart-half { position: relative; width: 100%; height: 300px; padding: 18px; border: 1px solid #1f2937; border-radius: 18px; background: #111827; }\n" +
            "    .sensor-section { background: #111827; padding: 20px; margin: 20px 0; border-left: 4px solid #2563eb; border-radius: 18px; }\n" +
            "    .sensor-title { color: #e2e8f0; font-weight: bold; margin-bottom: 15px; }\n" +
            "    table { width: 100%; border-collapse: collapse; margin: 18px 0; background: #0f172a; }\n" +
            "    th { background: #1e293b; color: #e2e8f0; padding: 13px; text-align: left; }\n" +
            "    td { padding: 11px; border-bottom: 1px solid #1f2937; color: #cbd5e1; }\n" +
            "    tr:nth-child(even) { background: #111827; }\n" +
            "    .page-break { page-break-after: always; margin: 40px 0; }\n" +
            "  </style>\n";
    }

    private String generarResumenEjecutivo(ReporteFinal base) {
        String estado = base.getEstado() != null ? base.getEstado() : "N/A";
        String estiloEstado = estado.toLowerCase().contains("act") ? "success" : "warning";
        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"summary-cards\">\n");
        sb.append("    <div class=\"summary-card\"><h4>Ensayo</h4><p>").append(base.getNombreEnsayo()).append("</p></div>\n");
        sb.append("    <div class=\"summary-card\"><h4>Estado</h4><p><span class=\"badge badge-").append(estiloEstado).append("\">").append(estado).append("</span></p></div>\n");
        sb.append("    <div class=\"summary-card\"><h4>Duración</h4><p>").append(base.getFechaInicio()).append(" → ").append(base.getFechaFin()).append("</p></div>\n");
        sb.append("    <div class=\"summary-card\"><h4>Responsable</h4><p>").append(base.getResponsable()).append("</p></div>\n");
        sb.append("  </div>\n");
        return sb.toString();
    }

    private String generarTituloEncabezado() {
        return "  <h1>REPORTE COMPLETO DE ENSAYO</h1>\n";
    }

    private String generarSeccionResumenMetricas(ReporteFinal base, double q1, double q2, double q3) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <h2>Indicadores Clave</h2>\n");
        sb.append("  <div class=\"metric-row\">\n");
        sb.append("    <div class=\"metric-card emphasis\"><strong>Registros</strong><span>").append(base.getTotalDatos()).append("</span></div>\n");
        sb.append("    <div class=\"metric-card\"><strong>Anormales</strong><span>").append(base.getDatosAnormales()).append(" (" ).append(String.format("%.2f%%", base.getPorcentajeAnormales())).append(")</span></div>\n");
        sb.append("    <div class=\"metric-card\"><strong>Media</strong><span>").append(String.format("%.2f", base.getMedia())).append("</span></div>\n");
        sb.append("    <div class=\"metric-card\"><strong>Rango</strong><span>").append(String.format("%.2f", base.getRango())).append("</span></div>\n");
        sb.append("  </div>\n");
        return sb.toString();
    }

    private String generarSeccionInformacion(ReporteFinal base) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <h2>Información del Ensayo</h2>\n");
        sb.append("  <div class=\"info-section\">\n");
        sb.append("    <p><strong>Ensayo:</strong><br>").append(base.getNombreEnsayo()).append("</p>\n");
        sb.append("    <p><strong>Máquina:</strong><br>").append(base.getNombreMaquina()).append("<br><em>").append(base.getTipoMaquina()).append("</em></p>\n");
        sb.append("    <p><strong>Período:</strong><br>").append(base.getFechaInicio()).append(" a ").append(base.getFechaFin()).append("</p>\n");
        sb.append("    <p><strong>Responsable:</strong><br>").append(base.getResponsable()).append("</p>\n");
        sb.append("    <p><strong>Límite Inferior:</strong><br>").append(String.format("%.2f", base.getLimiteInferior())).append("</p>\n");
        sb.append("    <p><strong>Límite Superior:</strong><br>").append(String.format("%.2f", base.getLimiteSuperior())).append("</p>\n");
        sb.append("    <p><strong>Registros:</strong><br>").append(base.getTotalDatos()).append("</p>\n");
        sb.append("    <p><strong>Anormales:</strong><br>").append(base.getDatosAnormales()).append(" (" ).append(String.format("%.2f%%", base.getPorcentajeAnormales())).append(")</p>\n");
        sb.append("  </div>\n");
        return sb.toString();
    }

    private String generarSeccionEstadisticas(ReporteFinal base, double q1, double q2, double q3) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <h2>Estadísticas y Cuartiles</h2>\n");
        sb.append("  <div class=\"stats-grid\">\n");
        sb.append("    <div class=\"stat-box\"><div class=\"stat-value\">").append(String.format("%.2f", base.getMinimo())).append("</div><div class=\"stat-label\">Q0 Mín</div></div>\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #f39c12, #d68910);\"><div class=\"stat-value\">").append(String.format("%.2f", q1)).append("</div><div class=\"stat-label\">Q1 (25%)</div></div>\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #2ecc71, #27ae60);\"><div class=\"stat-value\">").append(String.format("%.2f", q2)).append("</div><div class=\"stat-label\">Q2 Mediana</div></div>\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #e74c3c, #c0392b);\"><div class=\"stat-value\">").append(String.format("%.2f", q3)).append("</div><div class=\"stat-label\">Q3 (75%)</div></div>\n");
        sb.append("  </div>\n");
        sb.append("  <div class=\"stats-grid\">\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #9b59b6, #8e44ad);\"><div class=\"stat-value\">").append(String.format("%.2f", base.getMaximo())).append("</div><div class=\"stat-label\">Q4 Máx</div></div>\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #3498db, #2980b9);\"><div class=\"stat-value\">").append(String.format("%.2f", base.getMedia())).append("</div><div class=\"stat-label\">Media</div></div>\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #1abc9c, #16a085);\"><div class=\"stat-value\">").append(String.format("%.2f", base.getDesviacionEstandar())).append("</div><div class=\"stat-label\">Desv. Est.</div></div>\n");
        sb.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #34495e, #2c3e50);\"><div class=\"stat-value\">").append(base.getTotalDatos()).append("</div><div class=\"stat-label\">Registros</div></div>\n");
        sb.append("  </div>\n");
        return sb.toString();
    }

    private String generarTablaEstadisticas(ReporteFinal base, double q1, double q2, double q3) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <h2>Tabla de Estadísticas Detallada</h2>\n");
        sb.append("  <table>\n");
        sb.append("    <tr><th>Métrica</th><th>Valor</th></tr>\n");
        sb.append("    <tr><td>Total de Registros</td><td>").append(base.getTotalDatos()).append("</td></tr>\n");
        sb.append("    <tr><td>Registros Anormales</td><td>").append(base.getDatosAnormales()).append(" (").append(String.format("%.2f%%", base.getPorcentajeAnormales())).append(")</td></tr>\n");
        sb.append("    <tr><td>Media</td><td>").append(String.format("%.4f", base.getMedia())).append("</td></tr>\n");
        sb.append("    <tr><td>Desviación Estándar</td><td>").append(String.format("%.4f", base.getDesviacionEstandar())).append("</td></tr>\n");
        sb.append("    <tr><td>Q1 (25%)</td><td>").append(String.format("%.4f", q1)).append("</td></tr>\n");
        sb.append("    <tr><td>Mediana (Q2)</td><td>").append(String.format("%.4f", q2)).append("</td></tr>\n");
        sb.append("    <tr><td>Q3 (75%)</td><td>").append(String.format("%.4f", q3)).append("</td></tr>\n");
        sb.append("    <tr><td>Valor Mínimo</td><td>").append(String.format("%.4f", base.getMinimo())).append("</td></tr>\n");
        sb.append("    <tr><td>Valor Máximo</td><td>").append(String.format("%.4f", base.getMaximo())).append("</td></tr>\n");
        sb.append("    <tr><td>Rango</td><td>").append(String.format("%.4f", base.getRango())).append("</td></tr>\n");
        sb.append("    <tr><td>IQR (Q3-Q1)</td><td>").append(String.format("%.4f", q3 - q1)).append("</td></tr>\n");
        sb.append("    <tr><td>Límite Inferior</td><td>").append(String.format("%.4f", base.getLimiteInferior())).append("</td></tr>\n");
        sb.append("    <tr><td>Límite Superior</td><td>").append(String.format("%.4f", base.getLimiteSuperior())).append("</td></tr>\n");
        
        if (base.getCalculaFH() != null && base.getCalculaFH() && base.getFactorHistorico() != null) {
            sb.append("    <tr style=\"background: #fff3cd; font-weight: bold;\"><td>Factor Histórico (FH)</td><td>").append(String.format("%.6f", base.getFactorHistorico())).append("</td></tr>\n");
            sb.append("    <tr style=\"background: #fff3cd;\"><td>Parámetro Z</td><td>").append(base.getParametroZ()).append("</td></tr>\n");
            sb.append("    <tr style=\"background: #fff3cd; font-size: 11px;\"><td colspan=\"2\"><em>Fórmula: FH = Σ(10<sup>((Ti - 250)/z)</sup> · Δt)</em></td></tr>\n");
        }
        sb.append("  </table>\n");
        return sb.toString();
    }

    private String generarSeccionGraficas(ReporteFinal base, List<DatoEnsayoTemporal> datos, 
            double q1, double q2, double q3, Map<String, List<DatoEnsayoTemporal>> datosPorSensor) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"page-break\"></div>\n");
        sb.append("  <h2>Gráficas de Análisis - Parte 1</h2>\n");
        sb.append("  <div class=\"chart-container\"><canvas id=\"boxPlot\"></canvas></div>\n");
        sb.append("  <div class=\"chart-zoom-info\"><strong>Serie Temporal Interactiva:</strong> Usa la rueda del ratón para hacer ZOOM. Mantén click izquierdo para desplazarte (PAN). Click derecho para resetear.</div>\n");
        sb.append("  <div class=\"chart-container\"><canvas id=\"timeSeries\"></canvas></div>\n");
        sb.append("  <div style=\"margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 5px;\">\n");
        sb.append("    <label for=\"timeSeriesSlider\" style=\"display: block; margin-bottom: 5px; font-weight: bold; font-size: 12px;\">Desplazamiento: <span id=\"sliderValue\">0</span> / <span id=\"sliderMax\">0</span></label>\n");
        sb.append("    <input type=\"range\" id=\"timeSeriesSlider\" min=\"0\" max=\"100\" value=\"0\" style=\"width: 100%; cursor: pointer;\">\n");
        sb.append("  </div>\n");
        
        sb.append("  <div class=\"page-break\"></div>\n");
        sb.append("  <h2>Gráficas de Análisis - Parte 2</h2>\n");
        sb.append("  <div class=\"chart-row\">\n");
        sb.append("    <div class=\"chart-half\"><canvas id=\"histogram\"></canvas></div>\n");
        sb.append("    <div class=\"chart-half\"><canvas id=\"anomaly\"></canvas></div>\n");
        sb.append("  </div>\n");
        sb.append("  <div class=\"chart-row\">\n");
        sb.append("    <div class=\"chart-half\"><canvas id=\"quartiles\"></canvas></div>\n");
        sb.append("    <div class=\"chart-half\"><canvas id=\"limits\"></canvas></div>\n");
        sb.append("  </div>\n");
        
        sb.append("<script>\n");
        sb.append(generarScriptGraficas(base, datos, q1, q2, q3, datosPorSensor));
        sb.append("</script>\n");
        
        return sb.toString();
    }

    private String generarSeccionSensores(Map<String, List<DatoEnsayoTemporal>> datosPorSensor) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"page-break\"></div>\n");
        sb.append("  <h2>Análisis Detallado por Sensor</h2>\n");
        
        int sensorIdx = 0;
        for (String sensor : datosPorSensor.keySet()) {
            List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
            double mediaSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).average().orElse(0);
            double minSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).min().orElse(0);
            double maxSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).max().orElse(0);
            long anormalesSensor = datosSensor.stream().filter(d -> d.getAnormal() != null && d.getAnormal()).count();
            
            sb.append("  <div class=\"sensor-section\">\n");
            sb.append("    <div class=\"sensor-title\">Sensor: ").append(sensor).append("</div>\n");
            sb.append("    <table style=\"font-size: 12px;\">\n");
            sb.append("      <tr><td><strong>Registros:</strong></td><td>").append(datosSensor.size()).append("</td><td><strong>Media:</strong></td><td>").append(String.format("%.2f", mediaSensor)).append("</td></tr>\n");
            sb.append("      <tr><td><strong>Mínimo:</strong></td><td>").append(String.format("%.2f", minSensor)).append("</td><td><strong>Máximo:</strong></td><td>").append(String.format("%.2f", maxSensor)).append("</td></tr>\n");
            sb.append("      <tr><td><strong>Rango:</strong></td><td>").append(String.format("%.2f", maxSensor - minSensor)).append("</td><td><strong>Anormales:</strong></td><td>").append(anormalesSensor).append("</td></tr>\n");
            sb.append("    </table>\n");
            sb.append("    <div class=\"chart-container\" style=\"height: 250px; margin: 10px 0;\">\n");
            sb.append("      <canvas id=\"sensorChart").append(sensorIdx).append("\"></canvas>\n");
            sb.append("    </div>\n");
            sb.append("  </div>\n");
            sensorIdx++;
        }
        
        return sb.toString();
    }

    private String generarSeccionCorrecciones(java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones) {
        if (correcciones == null || correcciones.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"page-break\"></div>\n");
        sb.append("  <h2>Correcciones Aplicadas</h2>\n");
        for (com.sivco.gestion_archivos.modelos.CalibrationCorrection correccion : correcciones) {
            sb.append("  <div class=\"sensor-section\">\n");
            sb.append("    <p><strong>Archivo:</strong> ").append(correccion.getNombreArchivo()).append("</p>\n");
            sb.append("    <p><strong>Fecha:</strong> ").append(correccion.getFechaSubida()).append("</p>\n");
            sb.append("    <p><strong>Subido por:</strong> ").append(correccion.getSubidoPor()).append("</p>\n");
            if (correccion.getDescripcion() != null && !correccion.getDescripcion().isEmpty()) {
                sb.append("    <p><strong>Descripción:</strong> ").append(correccion.getDescripcion()).append("</p>\n");
            }
            sb.append("  </div>\n");
        }
        return sb.toString();
    }

    private String generarFooter() {
        return "  <div style=\"margin-top: 40px; padding-top: 20px; border-top: 2px solid #ecf0f1; text-align: center; color: #7f8c8d; font-size: 12px;\">\n" +
               "    <p>Reporte generado automáticamente - Sistema de Gestión de Archivos y Ensayos</p>\n" +
               "  </div>\n";
    }

    private String generarScriptGraficas(ReporteFinal base, List<DatoEnsayoTemporal> datos, 
            double q1, double q2, double q3, Map<String, List<DatoEnsayoTemporal>> datosPorSensor) {
        StringBuilder script = new StringBuilder();
        
        // Variables globales
        script.append("  const media = ").append(base.getMedia()).append(";\n");
        script.append("  const minVal = ").append(base.getMinimo()).append(";\n");
        script.append("  const maxVal = ").append(base.getMaximo()).append(";\n");
        script.append("  const q1 = ").append(q1).append(";\n");
        script.append("  const q2 = ").append(q2).append(";\n");
        script.append("  const q3 = ").append(q3).append(";\n");
        script.append("  const limInf = ").append(base.getLimiteInferior()).append(";\n");
        script.append("  const limSup = ").append(base.getLimiteSuperior()).append(";\n");
        script.append("  const normales = ").append(base.getTotalDatos() - base.getDatosAnormales()).append(";\n");
        script.append("  const anormales = ").append(base.getDatosAnormales()).append(";\n");
        
        // Gráficas principales
        script.append(generarGraficaBoxPlot(q1, q2, q3, base));
        script.append(generarGraficaSeriesTiempo(datos, base));
        script.append(generarGraficasAnalisis(base, datos, q1, q2, q3));
        script.append(generarGraficasSensores(datosPorSensor));
        
        return script.toString();
    }

    private String generarGraficaBoxPlot(double q1, double q2, double q3, ReporteFinal base) {
        return "  new Chart(document.getElementById('boxPlot'), {\n" +
               "    type: 'bar',\n" +
               "    data: {\n" +
               "      labels: ['Q0', 'Q1', 'Q2', 'Q3', 'Q4'],\n" +
               "      datasets: [{\n" +
               "        label: 'Cuartiles',\n" +
               "        data: [minVal, q1, q2, q3, maxVal],\n" +
               "        backgroundColor: ['#3498db', '#2ecc71', '#f39c12', '#e74c3c', '#9b59b6']\n" +
               "      }]\n" +
               "    },\n" +
               "    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Box Plot - Análisis de Cuartiles' } } }\n" +
               "  });\n";
    }

    private String generarGraficaSeriesTiempo(List<DatoEnsayoTemporal> datos, ReporteFinal base) {
        String valoresArray = generarArrayValores(datos);
        return "  const valores = " + valoresArray + ";\n" +
               "  const etiquetas = Array.from({length: valores.length}, (_, i) => i+1);\n" +
               "  const timeSeriesChart = new Chart(document.getElementById('timeSeries'), {\n" +
               "    type: 'line',\n" +
               "    data: {\n" +
               "      labels: etiquetas,\n" +
               "      datasets: [\n" +
               "        {label: 'Valores', data: valores, borderColor: '#3498db', tension: 0.2, fill: false},\n" +
               "        {label: 'Límite Sup', data: Array(valores.length).fill(limSup), borderColor: '#e74c3c', borderDash: [5,5], fill: false},\n" +
               "        {label: 'Límite Inf', data: Array(valores.length).fill(limInf), borderColor: '#e74c3c', borderDash: [5,5], fill: false}\n" +
               "      ]\n" +
               "    },\n" +
               "    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Serie Temporal - Valores vs Límites' }, zoom: { zoom: { wheel: { enabled: true, speed: 0.1 }, pinch: { enabled: true }, mode: 'x' }, pan: { enabled: true, mode: 'x' } } } }\n" +
               "  });\n" +
               "  const slider = document.getElementById('timeSeriesSlider');\n" +
               "  const sliderValue = document.getElementById('sliderValue');\n" +
               "  const sliderMax = document.getElementById('sliderMax');\n" +
               "  const dataLength = valores.length;\n" +
               "  const windowSize = Math.min(50, dataLength);\n" +
               "  const maxPos = Math.max(0, dataLength - windowSize);\n" +
               "  slider.max = maxPos;\n" +
               "  slider.value = 0;\n" +
               "  sliderMax.textContent = slider.max;\n" +
               "  function updateChartWindow(pos) {\n" +
               "    if (timeSeriesChart.options?.scales?.x) {\n" +
               "      timeSeriesChart.options.scales.x.min = pos;\n" +
               "      timeSeriesChart.options.scales.x.max = pos + windowSize;\n" +
               "      timeSeriesChart.update('none');\n" +
               "    }\n" +
               "  }\n" +
               "  slider.addEventListener('input', function() {\n" +
               "    const pos = Math.max(0, Math.min(parseInt(this.value || 0), maxPos));\n" +
               "    sliderValue.textContent = pos;\n" +
               "    updateChartWindow(pos);\n" +
               "  });\n";
    }

    private String generarGraficasAnalisis(ReporteFinal base, List<DatoEnsayoTemporal> datos, double q1, double q2, double q3) {
        StringBuilder sb = new StringBuilder();
        
        long cnt1 = contarEnRango(datos, Double.NEGATIVE_INFINITY, q1);
        long cnt2 = contarEnRango(datos, q1, q2);
        long cnt3 = contarEnRango(datos, q2, q3);
        long cnt4 = contarEnRango(datos, q3, Double.POSITIVE_INFINITY);
        
        // Histograma
        sb.append("  new Chart(document.getElementById('histogram'), {\n");
        sb.append("    type: 'bar',\n");
        sb.append("    data: { labels: ['<Q1', 'Q1-Q2', 'Q2-Q3', '>Q3'],\n");
        sb.append("      datasets: [{ label: 'Histograma', data: [").append(cnt1).append(", ").append(cnt2).append(", ").append(cnt3).append(", ").append(cnt4).append("],\n");
        sb.append("        backgroundColor: ['#3498db', '#2ecc71', '#f39c12', '#e74c3c'] }] },\n");
        sb.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Histograma - Distribución' } } }\n");
        sb.append("  });\n");
        
        // Anomalías
        sb.append("  new Chart(document.getElementById('anomaly'), {\n");
        sb.append("    type: 'doughnut',\n");
        sb.append("    data: { labels: ['Normales', 'Anormales'],\n");
        sb.append("      datasets: [{ data: [normales, anormales], backgroundColor: ['#2ecc71', '#e74c3c'] }] },\n");
        sb.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Distribución de Anomalías' } } }\n");
        sb.append("  });\n");
        
        // Cuartiles
        sb.append("  new Chart(document.getElementById('quartiles'), {\n");
        sb.append("    type: 'bar',\n");
        sb.append("    data: { labels: ['IQR', 'Min-Q1', 'Q3-Max'],\n");
        sb.append("      datasets: [{ label: 'Rangos', data: [").append(q3 - q1).append(", ").append(q1 - base.getMinimo()).append(", ").append(base.getMaximo() - q3).append("],\n");
        sb.append("        backgroundColor: ['#3498db', '#f39c12', '#e74c3c'] }] },\n");
        sb.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Análisis de Cuartiles' } } }\n");
        sb.append("  });\n");
        
        // Límites
        sb.append("  new Chart(document.getElementById('limits'), {\n");
        sb.append("    type: 'bar',\n");
        sb.append("    data: { labels: ['Lim Inf', 'Mín', 'Media', 'Máx', 'Lim Sup'],\n");
        sb.append("      datasets: [{ label: 'Valores', data: [limInf, minVal, media, maxVal, limSup],\n");
        sb.append("        backgroundColor: ['#e74c3c', '#3498db', '#f39c12', '#3498db', '#e74c3c'] }] },\n");
        sb.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Comparación de Límites' } } }\n");
        sb.append("  });\n");
        
        return sb.toString();
    }

    private String generarGraficasSensores(Map<String, List<DatoEnsayoTemporal>> datosPorSensor) {
        StringBuilder sb = new StringBuilder();
        int sensorIdx = 0;
        for (String sensor : datosPorSensor.keySet()) {
            List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
            String valoresS = generarArrayValores(datosSensor);
            sb.append("  new Chart(document.getElementById('sensorChart").append(sensorIdx).append("'), {\n");
            sb.append("    type: 'line',\n");
            sb.append("    data: {\n");
            sb.append("      labels: Array.from({length: ").append(datosSensor.size()).append("}, (_, i) => i+1),\n");
            sb.append("      datasets: [{label: '").append(sensor).append("', data: ").append(valoresS).append(", borderColor: '#3498db', tension: 0.2, fill: false}]\n");
            sb.append("    },\n");
            sb.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Sensor: ").append(sensor).append("' } } }\n");
            sb.append("  });\n");
            sensorIdx++;
        }
        return sb.toString();
    }
    
    private double calcularCuartil(List<Double> valores, double percentil) {
        if (valores.isEmpty()) return 0;
        int indice = (int) Math.ceil(percentil * valores.size()) - 1;
        return valores.get(Math.max(0, indice));
    }
    
    private String generarArrayValores(List<DatoEnsayoTemporal> datos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < datos.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(datos.get(i).getValor());
        }
        sb.append("]");
        return sb.toString();
    }
    
    private long contarEnRango(List<DatoEnsayoTemporal> datos, double min, double max) {
        return datos.stream()
            .filter(d -> d.getValor() >= min && d.getValor() <= max)
            .count();
    }
}
