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
        // Obtener correcciones aplicadas históricamente a los datos del ensayo
        List<Long> calibrationIds = datos.stream()
            .map(DatoEnsayoTemporal::getAppliedCalibrationId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones = new java.util.ArrayList<>();
        if (!calibrationIds.isEmpty()) {
            // Try to obtain the legacy repository or service from the Spring context.
            com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio repo = null;
            com.sivco.gestion_archivos.servicios.CalibrationCorrectionServicio calServ = null;
            try {
                var ctx = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
                if (ctx != null) {
                    try { repo = ctx.getBean(com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio.class); } catch (Exception ignore) {}
                    try { calServ = ctx.getBean(com.sivco.gestion_archivos.servicios.CalibrationCorrectionServicio.class); } catch (Exception ignore) {}
                }
            } catch (Exception ignore) {
                // ignore - we'll attempt fallbacks below
            }

            for (Long cid : calibrationIds) {
                try {
                    // First try to treat cid as a legacy CalibrationCorrection id
                    if (repo != null) {
                        repo.findById(cid).ifPresent(correcciones::add);
                        continue;
                    }

                    // Fallback: try to interpret cid as a sensor id and fetch history from service
                    if (calServ != null) {
                        java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> hist = calServ.historyForSensor(cid);
                        if (hist != null && !hist.isEmpty()) {
                            correcciones.addAll(hist);
                        }
                    }
                } catch (Exception ex) {
                    // ignore individual fetch errors
                }
            }
        }

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
        html.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Reporte - ").append(base.getNombreEnsayo()).append("</title>\n");
        html.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js\"></script>\n");
        html.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-zoom/2.1.0/chartjs-plugin-zoom.min.js\"></script>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: Arial, sans-serif; color: #333; }\n");
        html.append("    .container { max-width: 1000px; margin: 0 auto; background: white; padding: 40px; }\n");
        html.append("    h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding: 10px 0; }\n");
        html.append("    h2 { color: #34495e; margin-top: 30px; border-left: 4px solid #3498db; padding-left: 10px; }\n");
        html.append("    h3 { color: #7f8c8d; }\n");
        html.append("    .info-section { background: #ecf0f1; padding: 15px; border-radius: 5px; margin: 15px 0; }\n");
        html.append("    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 20px 0; }\n");
        html.append("    .stat-box { background: linear-gradient(135deg, #3498db, #2980b9); color: white; padding: 15px; border-radius: 5px; text-align: center; }\n");
        html.append("    .stat-value { font-size: 20px; font-weight: bold; }\n");
        html.append("    .stat-label { font-size: 11px; margin-top: 5px; }\n");
        html.append("    .chart-container { position: relative; width: 100%; height: 350px; margin: 30px 0; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }\n");
        html.append("    .chart-zoom-info { background: #d1ecf1; border: 1px solid #0c5460; color: #0c5460; padding: 10px; border-radius: 3px; margin-bottom: 10px; font-size: 12px; }\n");
        html.append("    .chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }\n");
        html.append("    .chart-half { position: relative; width: 100%; height: 300px; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }\n");
        html.append("    .sensor-section { background: #f8f9fa; padding: 20px; margin: 20px 0; border-left: 4px solid #17a2b8; border-radius: 5px; }\n");
        html.append("    .sensor-title { color: #17a2b8; font-weight: bold; margin-bottom: 15px; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; margin: 15px 0; }\n");
        html.append("    th { background: #34495e; color: white; padding: 12px; text-align: left; }\n");
        html.append("    td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
        html.append("    tr:nth-child(even) { background: #f9f9f9; }\n");
        html.append("    .page-break { page-break-after: always; margin: 40px 0; }\n");
        html.append("  </style>\n");
        html.append("</head>\n<body>\n<div class=\"container\">\n");
        
        // Título
        html.append("  <h1>REPORTE COMPLETO DE ENSAYO</h1>\n");
        
        // Información
        html.append("  <h2>Información del Ensayo</h2>\n");
        html.append("  <div class=\"info-section\">\n");
        html.append("    <p><strong>Ensayo:</strong> ").append(base.getNombreEnsayo()).append("</p>\n");
        html.append("    <p><strong>Máquina:</strong> ").append(base.getNombreMaquina()).append(" (").append(base.getTipoMaquina()).append(")</p>\n");
        html.append("    <p><strong>Período:</strong> ").append(base.getFechaInicio()).append(" a ").append(base.getFechaFin()).append("</p>\n");
        html.append("    <p><strong>Responsable:</strong> ").append(base.getResponsable()).append("</p>\n");
        html.append("  </div>\n");
        
        // Estadísticas con Cuartiles
        html.append("  <h2>Estadísticas y Cuartiles</h2>\n");
        html.append("  <div class=\"stats-grid\">\n");
        html.append("    <div class=\"stat-box\"><div class=\"stat-value\">").append(String.format("%.2f", base.getMinimo())).append("</div><div class=\"stat-label\">Q0 Mín</div></div>\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #f39c12, #d68910);\"><div class=\"stat-value\">").append(String.format("%.2f", q1)).append("</div><div class=\"stat-label\">Q1 (25%)</div></div>\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #2ecc71, #27ae60);\"><div class=\"stat-value\">").append(String.format("%.2f", q2)).append("</div><div class=\"stat-label\">Q2 Mediana</div></div>\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #e74c3c, #c0392b);\"><div class=\"stat-value\">").append(String.format("%.2f", q3)).append("</div><div class=\"stat-label\">Q3 (75%)</div></div>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"stats-grid\">\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #9b59b6, #8e44ad);\"><div class=\"stat-value\">").append(String.format("%.2f", base.getMaximo())).append("</div><div class=\"stat-label\">Q4 Máx</div></div>\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #3498db, #2980b9);\"><div class=\"stat-value\">").append(String.format("%.2f", base.getMedia())).append("</div><div class=\"stat-label\">Media</div></div>\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #1abc9c, #16a085);\"><div class=\"stat-value\">").append(String.format("%.2f", base.getDesviacionEstandar())).append("</div><div class=\"stat-label\">Desv. Est.</div></div>\n");
        html.append("    <div class=\"stat-box\" style=\"background: linear-gradient(135deg, #34495e, #2c3e50);\"><div class=\"stat-value\">").append(base.getTotalDatos()).append("</div><div class=\"stat-label\">Registros</div></div>\n");
        html.append("  </div>\n");
        
        // Tabla de Estadísticas Detallada
        html.append("  <h2>Tabla de Estadísticas Detallada</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr><th>Métrica</th><th>Valor</th></tr>\n");
        html.append("    <tr><td>Total de Registros</td><td>").append(base.getTotalDatos()).append("</td></tr>\n");
        html.append("    <tr><td>Registros Anormales</td><td>").append(base.getDatosAnormales()).append(" (").append(String.format("%.2f%%", base.getPorcentajeAnormales())).append(")</td></tr>\n");
        html.append("    <tr><td>Media</td><td>").append(String.format("%.4f", base.getMedia())).append("</td></tr>\n");
        html.append("    <tr><td>Desviación Estándar</td><td>").append(String.format("%.4f", base.getDesviacionEstandar())).append("</td></tr>\n");
        html.append("    <tr><td>Q1 (25%)</td><td>").append(String.format("%.4f", q1)).append("</td></tr>\n");
        html.append("    <tr><td>Mediana (Q2)</td><td>").append(String.format("%.4f", q2)).append("</td></tr>\n");
        html.append("    <tr><td>Q3 (75%)</td><td>").append(String.format("%.4f", q3)).append("</td></tr>\n");
        html.append("    <tr><td>Valor Mínimo</td><td>").append(String.format("%.4f", base.getMinimo())).append("</td></tr>\n");
        html.append("    <tr><td>Valor Máximo</td><td>").append(String.format("%.4f", base.getMaximo())).append("</td></tr>\n");
        html.append("    <tr><td>Rango</td><td>").append(String.format("%.4f", base.getRango())).append("</td></tr>\n");
        html.append("    <tr><td>IQR (Q3-Q1)</td><td>").append(String.format("%.4f", q3 - q1)).append("</td></tr>\n");
        html.append("    <tr><td>Límite Inferior</td><td>").append(String.format("%.4f", base.getLimiteInferior())).append("</td></tr>\n");
        html.append("    <tr><td>Límite Superior</td><td>").append(String.format("%.4f", base.getLimiteSuperior())).append("</td></tr>\n");
        
        // Factor Histórico (si aplica)
        if (base.getCalculaFH() != null && base.getCalculaFH() && base.getFactorHistorico() != null) {
            html.append("    <tr style=\"background: #fff3cd; font-weight: bold;\"><td>Factor Histórico (FH)</td><td>").append(String.format("%.6f", base.getFactorHistorico())).append("</td></tr>\n");
            html.append("    <tr style=\"background: #fff3cd;\"><td>Parámetro Z</td><td>").append(base.getParametroZ()).append("</td></tr>\n");
            html.append("    <tr style=\"background: #fff3cd; font-size: 11px;\"><td colspan=\"2\"><em>Fórmula: FH = Σ(10<sup>((Ti - 250)/z)</sup> · Δt)</em></td></tr>\n");
        }
        
        html.append("  </table>\n");
        
        // Página de Gráficas 1
        html.append("  <div class=\"page-break\"></div>\n");
        html.append("  <h2>Gráficas de Análisis - Parte 1</h2>\n");
        html.append("  <div class=\"chart-container\"><canvas id=\"boxPlot\"></canvas></div>\n");
        html.append("  <div class=\"chart-zoom-info\"><strong>Serie Temporal Interactiva:</strong> Usa la rueda del ratón para hacer ZOOM. Mantén click izquierdo para desplazarte (PAN). Click derecho para resetear.</div>\n");
        html.append("  <div class=\"chart-container\"><canvas id=\"timeSeries\"></canvas></div>\n");
        html.append("  <div style=\"margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 5px;\">\n");
        html.append("    <label for=\"timeSeriesSlider\" style=\"display: block; margin-bottom: 5px; font-weight: bold; font-size: 12px;\">Desplazamiento: <span id=\"sliderValue\">0</span> / <span id=\"sliderMax\">0</span></label>\n");
        html.append("    <input type=\"range\" id=\"timeSeriesSlider\" min=\"0\" max=\"100\" value=\"0\" style=\"width: 100%; cursor: pointer;\">\n");
        html.append("  </div>\n");
        
        // Página de Gráficas 2
        html.append("  <div class=\"page-break\"></div>\n");
        html.append("  <h2>Gráficas de Análisis - Parte 2</h2>\n");
        html.append("  <div class=\"chart-row\">\n");
        html.append("    <div class=\"chart-half\"><canvas id=\"histogram\"></canvas></div>\n");
        html.append("    <div class=\"chart-half\"><canvas id=\"anomaly\"></canvas></div>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"chart-row\">\n");
        html.append("    <div class=\"chart-half\"><canvas id=\"quartiles\"></canvas></div>\n");
        html.append("    <div class=\"chart-half\"><canvas id=\"limits\"></canvas></div>\n");
        html.append("  </div>\n");
        
        // Análisis por Sensor
        html.append("  <div class=\"page-break\"></div>\n");
        html.append("  <h2>Análisis Detallado por Sensor</h2>\n");
        int sensorIdx = 0;
        for (String sensor : datosPorSensor.keySet()) {
            List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
            double mediaSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).average().orElse(0);
            double minSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).min().orElse(0);
            double maxSensor = datosSensor.stream().mapToDouble(DatoEnsayoTemporal::getValor).max().orElse(0);
            long anormalesSensor = datosSensor.stream().filter(d -> d.getAnormal() != null && d.getAnormal()).count();
            
            html.append("  <div class=\"sensor-section\">\n");
            html.append("    <div class=\"sensor-title\">Sensor: ").append(sensor).append("</div>\n");
            html.append("    <table style=\"font-size: 12px;\">\n");
            html.append("      <tr><td><strong>Registros:</strong></td><td>").append(datosSensor.size()).append("</td><td><strong>Media:</strong></td><td>").append(String.format("%.2f", mediaSensor)).append("</td></tr>\n");
            html.append("      <tr><td><strong>Mínimo:</strong></td><td>").append(String.format("%.2f", minSensor)).append("</td><td><strong>Máximo:</strong></td><td>").append(String.format("%.2f", maxSensor)).append("</td></tr>\n");
            html.append("      <tr><td><strong>Rango:</strong></td><td>").append(String.format("%.2f", maxSensor - minSensor)).append("</td><td><strong>Anormales:</strong></td><td>").append(anormalesSensor).append("</td></tr>\n");
            html.append("    </table>\n");
            html.append("    <div class=\"chart-container\" style=\"height: 250px; margin: 10px 0;\">\n");
            html.append("      <canvas id=\"sensorChart").append(sensorIdx).append("\"></canvas>\n");
            html.append("    </div>\n");
            html.append("  </div>\n");
            sensorIdx++;
        }
        
        // Correcciones
        if (correcciones != null && !correcciones.isEmpty()) {
            html.append("  <div class=\"page-break\"></div>\n");
            html.append("  <h2>Correcciones Aplicadas</h2>\n");
            for (com.sivco.gestion_archivos.modelos.CalibrationCorrection correccion : correcciones) {
                html.append("  <div class=\"sensor-section\">\n");
                html.append("    <p><strong>Archivo:</strong> ").append(correccion.getNombreArchivo()).append("</p>\n");
                html.append("    <p><strong>Fecha:</strong> ").append(correccion.getFechaSubida()).append("</p>\n");
                html.append("    <p><strong>Subido por:</strong> ").append(correccion.getSubidoPor()).append("</p>\n");
                if (correccion.getDescripcion() != null && !correccion.getDescripcion().isEmpty()) {
                    html.append("    <p><strong>Descripción:</strong> ").append(correccion.getDescripcion()).append("</p>\n");
                }
                html.append("  </div>\n");
            }
        }
        
        // Footer
        html.append("  <div style=\"margin-top: 40px; padding-top: 20px; border-top: 2px solid #ecf0f1; text-align: center; color: #7f8c8d; font-size: 12px;\">\n");
        html.append("    <p>Reporte generado automáticamente - Sistema de Gestión de Archivos y Ensayos</p>\n");
        html.append("  </div>\n");
        html.append("</div>\n");
        
        // Scripts Chart.js
        html.append("<script>\n");
        html.append("  const media = ").append(base.getMedia()).append(";\n");
        html.append("  const desv = ").append(base.getDesviacionEstandar()).append(";\n");
        html.append("  const minVal = ").append(base.getMinimo()).append(";\n");
        html.append("  const maxVal = ").append(base.getMaximo()).append(";\n");
        html.append("  const q1 = ").append(q1).append(";\n");
        html.append("  const q2 = ").append(q2).append(";\n");
        html.append("  const q3 = ").append(q3).append(";\n");
        html.append("  const limInf = ").append(base.getLimiteInferior()).append(";\n");
        html.append("  const limSup = ").append(base.getLimiteSuperior()).append(";\n");
        html.append("  const normales = ").append(base.getTotalDatos() - base.getDatosAnormales()).append(";\n");
        html.append("  const anormales = ").append(base.getDatosAnormales()).append(";\n");
        
        // Gráfica 1: Box Plot
        html.append("  new Chart(document.getElementById('boxPlot'), {\n");
        html.append("    type: 'bar',\n");
        html.append("    data: {\n");
        html.append("      labels: ['Q0', 'Q1', 'Q2', 'Q3', 'Q4'],\n");
        html.append("      datasets: [{\n");
        html.append("        label: 'Cuartiles',\n");
        html.append("        data: [minVal, q1, q2, q3, maxVal],\n");
        html.append("        backgroundColor: ['#3498db', '#2ecc71', '#f39c12', '#e74c3c', '#9b59b6']\n");
        html.append("      }]\n");
        html.append("    },\n");
        html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Box Plot - Análisis de Cuartiles' } } }\n");
        html.append("  });\n");
        
        // Gráfica 2: Serie de Tiempo
        String valoresArray = generarArrayValores(datos);
        html.append("  const valores = ").append(valoresArray).append(";\n");
        html.append("  const etiquetas = Array.from({length: valores.length}, (_, i) => i+1);\n");
        html.append("  const timeSeriesChart = new Chart(document.getElementById('timeSeries'), {\n");
        html.append("    type: 'line',\n");
        html.append("    data: {\n");
        html.append("      labels: etiquetas,\n");
        html.append("      datasets: [\n");
        html.append("        {label: 'Valores', data: valores, borderColor: '#3498db', tension: 0.2, fill: false},\n");
        html.append("        {label: 'Límite Sup', data: Array(valores.length).fill(limSup), borderColor: '#e74c3c', borderDash: [5,5], fill: false},\n");
        html.append("        {label: 'Límite Inf', data: Array(valores.length).fill(limInf), borderColor: '#e74c3c', borderDash: [5,5], fill: false}\n");
        html.append("      ]\n");
        html.append("    },\n");
        html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Serie Temporal - Valores vs Límites (Scroll para hacer zoom)' }, zoom: { zoom: { wheel: { enabled: true, speed: 0.1 }, pinch: { enabled: true }, mode: 'x' }, pan: { enabled: true, mode: 'x' } } } }\n");
        html.append("  });\n");
        html.append("  \n");
        html.append("  // Configurar slider para serie temporal (actualiza options.scales.x)\n");
        html.append("  const slider = document.getElementById('timeSeriesSlider');\n");
        html.append("  const sliderValue = document.getElementById('sliderValue');\n");
        html.append("  const sliderMax = document.getElementById('sliderMax');\n");
        html.append("  const dataLength = valores.length;\n");
        html.append("  const windowSize = Math.min(50, dataLength);\n");
        html.append("  const maxPos = Math.max(0, dataLength - windowSize);\n");
        html.append("  slider.max = maxPos;\n");
        html.append("  slider.value = 0;\n");
        html.append("  sliderMax.textContent = slider.max;\n");
        html.append("  sliderValue.textContent = 0;\n");
        html.append("\n");
        html.append("  function updateChartWindow(pos) {\n");
        html.append("    const min = pos;\n");
        html.append("    const max = pos + windowSize;\n");
        html.append("    if (typeof timeSeriesChart !== 'undefined') {\n");
        html.append("      if (timeSeriesChart.options && timeSeriesChart.options.scales && timeSeriesChart.options.scales.x) {\n");
        html.append("        timeSeriesChart.options.scales.x.min = min;\n");
        html.append("        timeSeriesChart.options.scales.x.max = max;\n");
        html.append("      }\n");
        html.append("      if (timeSeriesChart.scales && timeSeriesChart.scales.x) {\n");
        html.append("        timeSeriesChart.scales.x.min = min;\n");
        html.append("        timeSeriesChart.scales.x.max = max;\n");
        html.append("      }\n");
        html.append("      try { timeSeriesChart.update('none'); } catch (e) { }\n");
        html.append("    }\n");
        html.append("  }\n");
        html.append("\n");
        html.append("  // Inicializa ventana visible\n");
        html.append("  setTimeout(() => { updateChartWindow(0); }, 50);\n");
        html.append("\n");
        html.append("  slider.addEventListener('input', function() {\n");
        html.append("    const pos = Math.max(0, Math.min(parseInt(this.value || 0), maxPos));\n");
        html.append("    sliderValue.textContent = pos;\n");
        html.append("    updateChartWindow(pos);\n");
        html.append("  });\n");
        
        // Gráfica 3: Histograma
        long cnt1 = contarEnRango(datos, Double.NEGATIVE_INFINITY, q1);
        long cnt2 = contarEnRango(datos, q1, q2);
        long cnt3 = contarEnRango(datos, q2, q3);
        long cnt4 = contarEnRango(datos, q3, Double.POSITIVE_INFINITY);
        html.append("  new Chart(document.getElementById('histogram'), {\n");
        html.append("    type: 'bar',\n");
        html.append("    data: {\n");
        html.append("      labels: ['<Q1', 'Q1-Q2', 'Q2-Q3', '>Q3'],\n");
        html.append("      datasets: [{\n");
        html.append("        label: 'Histograma',\n");
        html.append("        data: [").append(cnt1).append(", ").append(cnt2).append(", ").append(cnt3).append(", ").append(cnt4).append("],\n");
        html.append("        backgroundColor: ['#3498db', '#2ecc71', '#f39c12', '#e74c3c']\n");
        html.append("      }]\n");
        html.append("    },\n");
        html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Histograma - Distribución por Cuartiles' } } }\n");
        html.append("  });\n");
        
        // Gráfica 4: Anomalías
        html.append("  new Chart(document.getElementById('anomaly'), {\n");
        html.append("    type: 'doughnut',\n");
        html.append("    data: {\n");
        html.append("      labels: ['Normales', 'Anormales'],\n");
        html.append("      datasets: [{\n");
        html.append("        data: [normales, anormales],\n");
        html.append("        backgroundColor: ['#2ecc71', '#e74c3c']\n");
        html.append("      }]\n");
        html.append("    },\n");
        html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Distribución de Anomalías' } } }\n");
        html.append("  });\n");
        
        // Gráfica 5: Cuartiles
        html.append("  new Chart(document.getElementById('quartiles'), {\n");
        html.append("    type: 'bar',\n");
        html.append("    data: {\n");
        html.append("      labels: ['IQR', 'Min-Q1', 'Q3-Max'],\n");
        html.append("      datasets: [{\n");
        html.append("        label: 'Rangos',\n");
        html.append("        data: [").append(q3 - q1).append(", ").append(q1 - base.getMinimo()).append(", ").append(base.getMaximo() - q3).append("],\n");
        html.append("        backgroundColor: ['#3498db', '#f39c12', '#e74c3c']\n");
        html.append("      }]\n");
        html.append("    },\n");
        html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Análisis de Cuartiles - IQR' } } }\n");
        html.append("  });\n");
        
        // Gráfica 6: Límites
        html.append("  new Chart(document.getElementById('limits'), {\n");
        html.append("    type: 'bar',\n");
        html.append("    data: {\n");
        html.append("      labels: ['Lim Inf', 'Mín', 'Media', 'Máx', 'Lim Sup'],\n");
        html.append("      datasets: [{\n");
        html.append("        label: 'Valores',\n");
        html.append("        data: [limInf, minVal, media, maxVal, limSup],\n");
        html.append("        backgroundColor: ['#e74c3c', '#3498db', '#f39c12', '#3498db', '#e74c3c']\n");
        html.append("      }]\n");
        html.append("    },\n");
        html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Comparación de Límites' } } }\n");
        html.append("  });\n");
        
        // Gráficas por Sensor
        sensorIdx = 0;
        for (String sensor : datosPorSensor.keySet()) {
            List<DatoEnsayoTemporal> datosSensor = datosPorSensor.get(sensor);
            String valoresS = generarArrayValores(datosSensor);
            html.append("  new Chart(document.getElementById('sensorChart").append(sensorIdx).append("'), {\n");
            html.append("    type: 'line',\n");
            html.append("    data: {\n");
            html.append("      labels: Array.from({length: ").append(datosSensor.size()).append("}, (_, i) => i+1),\n");
            html.append("      datasets: [{label: '").append(sensor).append("', data: ").append(valoresS).append(", borderColor: '#3498db', tension: 0.2, fill: false}]\n");
            html.append("    },\n");
            html.append("    options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Sensor: ").append(sensor).append("' } } }\n");
            html.append("  });\n");
            sensorIdx++;
        }
        
        html.append("</script>\n</body>\n</html>\n");
        return html.toString();
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
