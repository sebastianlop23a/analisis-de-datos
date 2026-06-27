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
        
        // Filtrar sensores con carga 0.0 (sensores donde todas las lecturas son 0.0)
        Map<String, List<DatoEnsayoTemporal>> datosPorSensorTemp = datos.stream()
            .collect(Collectors.groupingBy(d -> d.getSensor() != null ? d.getSensor() : "Sin Sensor"));
        
        Set<String> sensoresValidos = datosPorSensorTemp.entrySet().stream()
            .filter(entry -> !entry.getValue().stream().allMatch(d -> d.getValor() == null || d.getValor() == 0.0))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        datos = datos.stream()
            .filter(d -> {
                String sensor = d.getSensor() != null ? d.getSensor() : "Sin Sensor";
                return sensoresValidos.contains(sensor);
            })
            .collect(Collectors.toList());
        
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
        html.append("<body class=\"reporte-html\">\n");
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
        
        // Sección "Análisis Detallado por Sensor" eliminada para esta versión del reporte.
        
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

    public String construirHtmlReporte(Long ensayoId, Ensayo ensayo) {
        ReporteFinal base = construirReporte(ensayoId, ensayo);
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);

        List<Double> valoresOrdenados = datos.stream()
            .map(DatoEnsayoTemporal::getValor)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());

        double q1 = calcularCuartil(valoresOrdenados, 0.25);
        double q2 = calcularCuartil(valoresOrdenados, 0.50);
        double q3 = calcularCuartil(valoresOrdenados, 0.75);

        Map<String, List<DatoEnsayoTemporal>> datosPorSensor = datos.stream()
            .collect(Collectors.groupingBy(d -> d.getSensor() != null ? d.getSensor() : "Sin Sensor"));

        java.util.List<com.sivco.gestion_archivos.modelos.CalibrationCorrection> correcciones = obtenerCorrecciones(datos);

        return generarDocumentoHTML(base, datos, q1, q2, q3, datosPorSensor, correcciones);
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
        // Sección "Análisis Detallado por Sensor" eliminada según solicitud.
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
        // Match the frontend: Chart.js + Hammer.js + zoom plugin v1.2.1 + boxplot + annotation
        head.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js\"></script>\n");
        head.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/hammer.js/2.0.8/hammer.min.js\"></script>\n");
        head.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-zoom/1.2.1/chartjs-plugin-zoom.min.js\"></script>\n");
        head.append("  <script src=\"https://cdn.jsdelivr.net/npm/chartjs-chart-box-and-violin-plot@3.1.0/dist/chartjs-chart-box-and-violin-plot.min.js\"></script>\n");
        head.append("  <script src=\"https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@1.1.1/dist/chartjs-plugin-annotation.min.js\"></script>\n");
        // Register annotation plugin if available to ensure horizontal lines render
        head.append("  <script>\n");
        head.append("    (function(){\n");
        head.append("      try {\n");
        head.append("        const plugin = window.chartjsPluginAnnotation || window.annotationPlugin || window.ChartAnnotation || window['chartjs-plugin-annotation'];\n");
        head.append("        if (plugin && window.Chart && typeof window.Chart.register === 'function') {\n");
        head.append("          try { window.Chart.register(plugin); } catch(e) { console.warn('No se pudo registrar annotation plugin:', e); }\n");
        head.append("        }\n");
        head.append("      } catch(e) { console.warn('Error registrando plugins en reporte:', e); }\n");
        head.append("    })();\n");
        head.append("  </script>\n");
        head.append(generarCSS());
        head.append("  <script>window.__SKIP_MAIN_APP_INITIALIZATION = true;</script>\n");
        head.append(generarScriptIncrustado("static/js/config.js"));
        head.append(generarScriptIncrustado("static/js/app.js"));
        head.append(generarScriptIncrustado("static/js/comparacion.js"));
        head.append("</head>\n");
        return head.toString();
    }

    private String generarScriptIncrustado(String recurso) {
        String contenido = cargarRecursoTexto(recurso);
        if (contenido == null || contenido.isEmpty()) {
            return "  <!-- No se pudo cargar " + recurso + " -->\n";
        }
        contenido = contenido.replace("</script>", "<\\/script>");
        return "  <script>\n" + contenido + "\n  </script>\n";
    }

    private String cargarRecursoTexto(String recurso) {
        try (var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(recurso)) {
            if (inputStream == null) {
                return null;
            }
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) {
                    sb.append(linea).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String generarCSS() {
        return "  <style>\n" +
            "    @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700;800&display=swap');\n" +
            "    :root {\n" +
            "      --color-primary: #3498db;\n" +
            "      --color-success: #2ecc71;\n" +
            "      --color-danger: #e74c3c;\n" +
            "      --color-warning: #f39c12;\n" +
            "      --color-dark: #2c3e50;\n" +
            "      --color-light: #f5f7fb;\n" +
            "      --color-border: #d1d5db;\n" +
            "      --shadow: 0 8px 30px rgba(0, 0, 0, 0.08);\n" +
            "      --transition: all 0.3s ease;\n" +
            "    }\n" +
            "    * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "    html { scroll-behavior: smooth; }\n" +
            "    body { font-family: 'Poppins', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f7fb; color: #2c3e50; line-height: 1.6; }\n" +
            "    .container { max-width: 1180px; margin: 24px auto 48px; padding: 30px 32px 40px; background: #ffffff; border-radius: 24px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08); }\n" +
            "    h1 { color: #2c3e50; margin-bottom: 24px; font-size: 2.6rem; letter-spacing: 0.6px; }\n" +
            "    h2 { color: #1f2937; margin-top: 40px; margin-bottom: 18px; font-size: 1.65rem; border-left: 6px solid var(--color-primary); padding-left: 14px; }\n" +
            "    h3, h4, h5 { color: #2c3e50; margin-bottom: 16px; }\n" +
            "    .section-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 24px; margin-bottom: 30px; }\n" +
            "    .chart-container { background: #ffffff; padding: 24px; border-radius: 18px; box-shadow: var(--shadow); border: 1px solid #e5e7eb; }\n" +
            "    .chart-container h3 { margin-bottom: 18px; font-size: 1.1rem; }\n" +
            "    .chart-container canvas { width: 100% !important; max-height: 520px; min-height: 380px; }\n" +
            "    .chart-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 18px; }\n" +
            "    .chart-filter { display: flex; align-items: center; gap: 8px; font-size: 0.88rem; flex-wrap: wrap; }\n" +
            "    .chart-filter label { font-weight: 600; color: #2c3e50; }\n" +
            "    .filter-time { padding: 7px 10px; border: 1px solid var(--color-border); border-radius: 6px; background: #fafbfc; font-size: 0.9rem; }\n" +
            "    .btn, .btn-filter, .btn-clear, .btn-sm { display: inline-flex; align-items: center; justify-content: center; cursor: pointer; border-radius: 8px; border: none; transition: var(--transition); font-weight: 600; }\n" +
            "    .btn { padding: 10px 16px; background: var(--color-primary); color: #ffffff; }\n" +
            "    .btn-sm { padding: 8px 12px; font-size: 0.88rem; }\n" +
            "    .btn-filter { padding: 8px 12px; background: var(--color-primary); color: #ffffff; }\n" +
            "    .btn-clear { padding: 8px 12px; background: #6b7280; color: #ffffff; }\n" +
            "    .btn-success { background: var(--color-success); color: #ffffff; }\n" +
            "    .btn-warning { background: var(--color-warning); color: #ffffff; }\n" +
            "    .btn-danger { background: var(--color-danger); color: #ffffff; }\n" +
            "    .table-section { background: #ffffff; padding: 24px; border-radius: 18px; box-shadow: var(--shadow); margin-top: 30px; overflow-x: auto; }\n" +
            "    .table { width: 100%; border-collapse: collapse; }\n" +
            "    .table thead { background-color: #1f2937; color: #ffffff; }\n" +
            "    .table th { padding: 16px; text-align: left; font-weight: 700; border-bottom: 3px solid var(--color-primary); }\n" +
            "    .table td { padding: 14px 16px; border-bottom: 1px solid #e5e7eb; color: #334155; }\n" +
            "    .table tbody tr:hover { background-color: #f8fafc; }\n" +
            "    .table tbody tr:nth-child(even) { background-color: #f3f4f6; }\n" +
            "    .stats-card { background: #ffffff; padding: 22px; border-radius: 18px; box-shadow: var(--shadow); border-top: 4px solid var(--color-primary); text-align: center; }\n" +
            "    .stats-card h4 { margin-bottom: 10px; font-size: 0.95rem; color: #475569; text-transform: uppercase; letter-spacing: 0.04em; }\n" +
            "    .stats-card p { margin: 0; font-size: 2rem; font-weight: 700; color: #111827; }\n" +
            "    .data-table { width: 100%; border-collapse: collapse; }\n" +
            "    .data-table th { padding: 14px 12px; background: #161e2b; color: #f8fafc; text-align: left; }\n" +
            "    .data-table td { padding: 12px; border-bottom: 1px solid #e5e7eb; color: #334155; }\n" +
            "    .data-table tbody tr:hover { background: #f8fafc; }\n" +
            "    .table-container { margin-top: 20px; }\n" +
            "    .loading { color: #64748b; font-style: italic; }\n" +
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
        sb.append("  <h2>Análisis Completo</h2>\n");
        sb.append("  <div class=\"chart-container\" style=\"margin-bottom: 24px;\">\n");
        sb.append("    <h3>Filtros de análisis</h3>\n");
        sb.append("    <div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; align-items: end;\">\n");
        sb.append("      <div style=\"display: flex; flex-direction: column; gap: 8px;\">\n");
        sb.append("        <label for=\"filtroHoraInicio\" style=\"font-weight: 700;\">Inicio</label>\n");
        sb.append("        <input type=\"datetime-local\" id=\"filtroHoraInicio\" class=\"filter-time\" style=\"width: 100%;\">\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; flex-direction: column; gap: 8px;\">\n");
        sb.append("        <label for=\"filtroHoraFin\" style=\"font-weight: 700;\">Fin</label>\n");
        sb.append("        <input type=\"datetime-local\" id=\"filtroHoraFin\" class=\"filter-time\" style=\"width: 100%;\">\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; flex-direction: column; gap: 8px;\">\n");
        sb.append("        <label for=\"filtroSensor\" style=\"font-weight: 700;\">Sensor</label>\n");
        sb.append("        <select id=\"filtroSensor\" class=\"filter-time\" style=\"width: 100%; min-height: 40px;\">\n");
        sb.append("          <option value=\"\">Todos los sensores</option>\n");
        sb.append("        </select>\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; gap: 10px; flex-wrap: wrap;\">\n");
        sb.append("        <button type=\"button\" onclick=\"aplicarFiltroHora()\" class=\"btn\">Aplicar filtro</button>\n");
        sb.append("        <button type=\"button\" onclick=\"limpiarFiltroHora()\" class=\"btn-clear\">Limpiar</button>\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; flex-direction: column; gap: 8px;\">\n");
        sb.append("        <label for=\"filtroAnalisisInicio\" style=\"font-weight: 700;\">Análisis parcial: inicio</label>\n");
        sb.append("        <input type=\"datetime-local\" id=\"filtroAnalisisInicio\" class=\"filter-time\" style=\"width: 100%;\">\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; flex-direction: column; gap: 8px;\">\n");
        sb.append("        <label for=\"filtroAnalisisFin\" style=\"font-weight: 700;\">Análisis parcial: fin</label>\n");
        sb.append("        <input type=\"datetime-local\" id=\"filtroAnalisisFin\" class=\"filter-time\" style=\"width: 100%;\">\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; gap: 10px; flex-wrap: wrap;\">\n");
        sb.append("        <button type=\"button\" onclick=\"aplicarFiltroAnalisisParcial()\" class=\"btn\">Aplicar análisis parcial</button>\n");
        sb.append("        <button type=\"button\" onclick=\"limpiarFiltroAnalisisParcial()\" class=\"btn-clear\">Restaurar</button>\n");
        sb.append("      </div>\n");
        sb.append("      <div id=\"filtroActivoTexto\" style=\"grid-column: 1 / -1; font-size: 0.95rem; color: #34495e; font-weight: 700;\">Sin filtros activos</div>\n");
        sb.append("      <div id=\"analisisParteTexto\" style=\"grid-column: 1 / -1; font-size: 0.95rem; color: #5f6b7a;\"></div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"section-row\">\n");
        sb.append("    <div class=\"stats-card emphasis\"><h4>Registros</h4><p id=\"statTotal\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Media</h4><p id=\"statMedia\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Desv. Est.</h4><p id=\"statDesv\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Máximo</h4><p id=\"statMax\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Mínimo</h4><p id=\"statMin\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Anormales</h4><p id=\"statAnormales\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Rango</h4><p id=\"statRango\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>Coef. Variación</h4><p id=\"statCoefVar\">0</p></div>\n");
        sb.append("    <div class=\"stats-card\"><h4>% Anormales</h4><p id=\"statPorcentajeAnormales\">0</p></div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"section-row\">\n");
        sb.append("    <div class=\"chart-container\">\n");
        sb.append("      <div class=\"chart-header\">\n");
        sb.append("        <h3>Distribución de Datos</h3>\n");
        sb.append("      </div>\n");
        sb.append("      <canvas id=\"chartDatos\"></canvas>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"chart-container\">\n");
        sb.append("      <div class=\"chart-header\">\n");
        sb.append("        <h3>Datos Normales vs Anormales</h3>\n");
        sb.append("      </div>\n");
        sb.append("      <canvas id=\"chartAnormales\"></canvas>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"section-row\">\n");
        sb.append("    <div class=\"chart-container\">\n");
        sb.append("      <div class=\"chart-header\">\n");
        sb.append("        <h3>Boxplot (Caja y Bigotes)</h3>\n");
        sb.append("      </div>\n");
        sb.append("      <canvas id=\"chartBoxplot\"></canvas>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"chart-container\">\n");
        sb.append("      <div class=\"chart-header\">\n");
        sb.append("        <h3>Análisis de Cuartiles</h3>\n");
        sb.append("      </div>\n");
        sb.append("      <canvas id=\"chartCuartiles\"></canvas>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"chart-container\">\n");
        sb.append("    <div class=\"chart-header\">\n");
        sb.append("      <h3>Serie Temporal</h3>\n");
        sb.append("    </div>\n");
        sb.append("    <canvas id=\"chartTemporal\"></canvas>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"table-section\">\n");
        sb.append("    <h3>Análisis por Sensor</h3>\n");
        sb.append("    <div id=\"analisisSensores\" class=\"table-container\"><p class=\"loading\">Cargando análisis por sensor...</p></div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"table-section\">\n");
        sb.append("    <h3>Gráficas por Sensor</h3>\n");
        sb.append("    <div style=\"margin-bottom: 15px; padding: 20px; background: rgba(52, 152, 219, 0.08); border-radius: 12px; border: 2px solid rgba(52, 152, 219, 0.2);\">\n");
        sb.append("      <div style=\"display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;\">\n");
        sb.append("        <label style=\"display: flex; align-items: center; gap: 10px; cursor: pointer;\">\n");
        sb.append("          <input type=\"checkbox\" id=\"modoComparacion\" onchange=\"toggleModoComparacion()\" style=\"width: 18px; height: 18px; cursor: pointer;\">\n");
        sb.append("          <span style=\"font-size: 16px;\"><strong>📏 Modo Comparación:</strong> Superponer todos los sensores</span>\n");
        sb.append("        </label>\n");
        sb.append("      </div>\n");
        sb.append("      <div style=\"display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;\">\n");
        sb.append("        <strong style=\"font-size: 15px;\">🎯 Sensores:</strong>\n");
        sb.append("        <div style=\"display: flex; gap: 8px;\">\n");
        sb.append("          <button type=\"button\" onclick=\"seleccionarTodosSensores()\" class=\"btn btn-sm\" style=\"padding: 4px 12px; font-size: 12px; background: #27ae60; border: none;\">✓ Todos</button>\n");
        sb.append("          <button type=\"button\" onclick=\"deseleccionarTodosSensores()\" class=\"btn btn-sm\" style=\"padding: 4px 12px; font-size: 12px; background: #e74c3c; border: none;\">✕ Ninguno</button>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("      <div id=\"selectoresSensores\" style=\"display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px;\">\n");
        sb.append("        <p style=\"color: #888; font-style: italic;\">🔄 Cargando sensores...</p>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("    <div id=\"graficasSensores\">\n");
        sb.append("      <p class=\"loading\">📈 Cargando gráficas por sensor...</p>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"table-section\">\n");
        sb.append("    <h3>Datos Registrados</h3>\n");
        sb.append("    <div style=\"margin-bottom: 20px; padding: 15px; background: rgba(52, 152, 219, 0.08); border-radius: 8px; border: 1px solid rgba(52, 152, 219, 0.2);\">\n");
        sb.append("      <table style=\"width: 100%; border-collapse: collapse;\">\n");
        sb.append("        <thead>\n");
        sb.append("          <tr>\n");
        sb.append("            <th style=\"width: 40px; text-align: center;\"><input type=\"checkbox\" id=\"checkAllDatos\" onchange=\"seleccionarTodosDatosTabla()\" style=\"width: 16px; height: 16px; cursor: pointer;\"></th>\n");
        sb.append("            <th>#</th>\n");
        sb.append("            <th>Timestamp</th>\n");
        sb.append("            <th>Sensor</th>\n");
        sb.append("            <th>Valor</th>\n");
        sb.append("            <th>Anormal</th>\n");
        sb.append("            <th>Fuente</th>\n");
        sb.append("          </tr>\n");
        sb.append("        </thead>\n");
        sb.append("        <tbody id=\"tablaDatosBody\">\n");
        sb.append("          <tr><td colspan=\"7\" class=\"loading\">Cargando datos...</td></tr>\n");
        sb.append("        </tbody>\n");
        sb.append("      </table>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        sb.append("<script>\n");
        sb.append("  (function(){\n");
        sb.append("    function iniciarReporte() {\n");
        sb.append("      try {\n");
        sb.append("        const datos = ").append(generarArrayObjetos(datos)).append(";\n");
        sb.append("        window.datosAnalisisOriginales = datos;\n");
        sb.append("        window.datosFiltradosActuales = datos;\n");
        sb.append("        window.datosTablaActual = datos;\n");
        sb.append("        window.maquinaLimites = { limiteInferior: ").append(base.getLimiteInferior()).append(", limiteSuperior: ").append(base.getLimiteSuperior()).append(" };\n");
        sb.append("        window.filtrosGraficas = window.filtrosGraficas || { distribucion: null, anormales: null, boxplot: null, cuartiles: null, temporal: null, sensores: {} };\n");
        sb.append("        window.indicesSeleccionadosTabla = window.indicesSeleccionadosTabla || new Set();\n");
        sb.append("        if (typeof poblarSelectorSensoresFiltro === 'function') { poblarSelectorSensoresFiltro(datos); }\n");
        sb.append("        if (typeof actualizarFiltroActivoTexto === 'function') { actualizarFiltroActivoTexto(null, null, ''); }\n");
        sb.append("        const valores = datos.map(d => d.valor);\n");
        sb.append("        const media = datos.length ? valores.reduce((s, v) => s + v, 0) / datos.length : 0;\n");
        sb.append("        const valoresOrdenados = datos.map(d => d.valor).sort((a, b) => a - b);\n");
        sb.append("        const q1 = datos.length ? calcularCuartil(valoresOrdenados, 0.25) : 0;\n");
        sb.append("        const q2 = datos.length ? calcularCuartil(valoresOrdenados, 0.50) : 0;\n");
        sb.append("        const q3 = datos.length ? calcularCuartil(valoresOrdenados, 0.75) : 0;\n");
        sb.append("        try {\n");
        sb.append("          actualizarEstadisticasAnalisis(datos, false);\n");
        sb.append("          crearGraficoDistribucion(datos, media);\n");
        sb.append("          crearGraficoAnormales(datos);\n");
        sb.append("          crearGraficoBoxplot(datos, { q1, mediana: q2, q3, minimo: Math.min(...valoresOrdenados), maximo: Math.max(...valoresOrdenados), media });\n");
        sb.append("          crearGraficoCuartiles({ q1, q3, media, maximo: Math.max(...valoresOrdenados), minimo: Math.min(...valoresOrdenados) });\n");
        sb.append("          crearGraficoTemporal(datos);\n");
        sb.append("          crearAnalisisPorSensor(datos, media);\n");
        sb.append("          crearGraficasPorSensor(datos);\n");
        sb.append("          llenarTablaDatos(datos);\n");
        sb.append("        } catch(e) {\n");
        sb.append("          console.error('Error creando gráficas frontend:', e);\n");
        sb.append("          const errorMsg = document.createElement('div');\n");
        sb.append("          errorMsg.style.color = '#c0392b';\n");
        sb.append("          errorMsg.style.marginTop = '16px';\n");
        sb.append("          errorMsg.textContent = 'Error creando las gráficas. Revisa la consola del navegador.';\n");
        sb.append("          document.querySelector('.container').prepend(errorMsg);\n");
        sb.append("        }\n");
        sb.append("      } catch(e) {\n");
        sb.append("        console.error('Error inyectando datos en reporte:', e);\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("    function ensureLibrariesThenStart() {\n");
        sb.append("      if (typeof Chart !== 'undefined') { iniciarReporte(); return; }\n");
        sb.append("      const libs = [\n");
        sb.append("        'https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js',\n");
        sb.append("        'https://cdnjs.cloudflare.com/ajax/libs/hammer.js/2.0.8/hammer.min.js',\n");
        sb.append("        'https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-zoom/1.2.1/chartjs-plugin-zoom.min.js',\n");
        sb.append("        'https://cdn.jsdelivr.net/npm/chartjs-chart-box-and-violin-plot@3.1.0/dist/chartjs-chart-box-and-violin-plot.min.js',\n");
        sb.append("        'https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@1.1.1/dist/chartjs-plugin-annotation.min.js'\n");
        sb.append("      ];\n");
        sb.append("      function loadScript(src) {\n");
        sb.append("        return new Promise((resolve, reject) => {\n");
        sb.append("          const s = document.createElement('script'); s.src = src; s.async = false; s.onload = () => resolve(src); s.onerror = () => reject(src); document.head.appendChild(s);\n");
        sb.append("        });\n");
        sb.append("      }\n");
        sb.append("      (async function(){\n");
        sb.append("        try {\n");
        sb.append("          for (const l of libs) { await loadScript(l); }\n");
        sb.append("          // Try registering annotation plugin if present\n");
        sb.append("          try { const plugin = window.chartjsPluginAnnotation || window.annotationPlugin || window.ChartAnnotation || window['chartjs-plugin-annotation']; if (plugin && window.Chart && typeof window.Chart.register === 'function') { window.Chart.register(plugin); } } catch(e){}\n");
        sb.append("          iniciarReporte();\n");
        sb.append("        } catch(e) { console.warn('No se pudieron cargar librerías externas:', e); iniciarReporte(); }\n");
        sb.append("      })();\n");
        sb.append("    }\n");
        sb.append("    if (document.readyState === 'loading') {\n");
        sb.append("      document.addEventListener('DOMContentLoaded', ensureLibrariesThenStart);\n");
        sb.append("    } else {\n");
        sb.append("      ensureLibrariesThenStart();\n");
        sb.append("    }\n");
        sb.append("  })();\n");
        sb.append("</script>\n");

        return sb.toString();
    }

    private String generarSeccionSensores(Map<String, List<DatoEnsayoTemporal>> datosPorSensor) {
        // Esta función ha sido desactivada: la sección detallada por sensor fue removida.
        return "";
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
        // Serializar datos como objetos JS (timestamp, sensor, valor, anormal)
        String datosObj = generarArrayObjetos(datos);
        StringBuilder sb = new StringBuilder();
        sb.append("  const palette = ['rgb(52,152,219)','rgb(46,204,113)','rgb(231,76,60)','rgb(243,156,18)','rgb(155,89,182)','rgb(26,188,156)','rgb(41,128,185)','rgb(39,174,96)'];\n");
        sb.append("  const datosRaw = ").append(datosObj).append(";\n");
        sb.append("  // Agrupar por sensor y obtener timestamps únicos ordenados\n");
        sb.append("  const sensoresMap = new Map();\n");
        sb.append("  const timestamps = new Set();\n");
        sb.append("  datosRaw.forEach(d => { timestamps.add(d.timestamp); if (!sensoresMap.has(d.sensor)) sensoresMap.set(d.sensor, []); sensoresMap.get(d.sensor).push(d); });\n");
        sb.append("  const labels = Array.from(timestamps).sort((a,b)=>new Date(a)-new Date(b)).map(ts=>{ const dt=new Date(ts); return dt.toLocaleString(); });\n");
        sb.append("  const datasets = [];\n");
        sb.append("  let idx=0; sensoresMap.forEach((arr, sensor) => {\n");
        sb.append("    const color = palette[idx % palette.length];\n");
        sb.append("    const map = new Map(arr.map(d=>[d.timestamp,d.valor]));\n");
        sb.append("    datasets.push({ label: sensor, data: labels.map(l=>{ const original=arr.find(x=>new Date(x.timestamp).toLocaleString()===l); return original ? original.valor : null; }), borderColor: color, backgroundColor: color.replace('rgb','rgba').replace(')',',0.1)'), pointRadius:3, tension:0.3, fill:false });\n");
        sb.append("    idx++;\n");
        sb.append("  });\n");
        sb.append("  // Añadir líneas de límite como datasets discontinuos\n");
        sb.append("  if (!isNaN(limSup)) datasets.push({ label: 'Límite Sup', data: labels.map(()=>limSup), borderColor: 'rgba(231,76,60,1)', borderDash:[8,4], pointRadius:0, fill:false });\n");
        sb.append("  if (!isNaN(limInf)) datasets.push({ label: 'Límite Inf', data: labels.map(()=>limInf), borderColor: 'rgba(46,204,113,1)', borderDash:[8,4], pointRadius:0, fill:false });\n");
        sb.append("  const timeSeriesChart = new Chart(document.getElementById('timeSeries'), { type: 'line', data: { labels: labels, datasets: datasets }, options: { responsive:true, maintainAspectRatio:false, plugins:{ title:{ display:true, text:'Serie Temporal - Valores vs Límites' }, zoom:{ zoom:{ wheel:{ enabled:true, speed:0.1 }, pinch:{ enabled:true }, mode:'x' }, pan:{ enabled:true, mode:'x' } } }, interaction:{ intersect:false, mode:'index' }, scales:{ x:{ display:true, title:{ display:true, text:'Tiempo' } } } } });\n");
        sb.append("  // Slider window similar al frontend\n");
        sb.append("  const slider = document.getElementById('timeSeriesSlider'); if (slider) { const sliderValue=document.getElementById('sliderValue'); const sliderMax=document.getElementById('sliderMax'); const dataLength = labels.length; const windowSize = Math.min(50, dataLength); const maxPos = Math.max(0, dataLength - windowSize); slider.max = maxPos; slider.value = 0; if(sliderMax) sliderMax.textContent = maxPos; if(sliderValue) sliderValue.textContent = 0; function updateChartWindow(pos) { if (timeSeriesChart.options?.scales?.x) { timeSeriesChart.options.scales.x.min = pos; timeSeriesChart.options.scales.x.max = pos + windowSize; timeSeriesChart.update('none'); } } slider.addEventListener('input', function(){ const pos = Math.max(0, Math.min(parseInt(this.value||0), maxPos)); if(sliderValue) sliderValue.textContent = pos; updateChartWindow(pos); }); }\n");
        return sb.toString();
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
            sb.append("  // Gráfica individual para sensor: ").append(sensor).append("\n");
            sb.append("  (function(){\n");
            sb.append("    const palette = ['rgb(52,152,219)','rgb(46,204,113)','rgb(231,76,60)','rgb(243,156,18)','rgb(155,89,182)','rgb(26,188,156)'];\n");
            sb.append("    const datos = ").append(generarArrayObjetos(datosSensor)).append(";\n");
            sb.append("    const labels = datos.map(d=>new Date(d.timestamp).toLocaleString());\n");
            sb.append("    const color = palette[").append(sensorIdx).append(" % palette.length];\n");
            sb.append("    const valores = datos.map(d=>d.valor);\n");
            sb.append("    new Chart(document.getElementById('sensorChart").append(sensorIdx).append("'), {\n");
            sb.append("      type: 'line',\n");
            sb.append("      data: { labels: labels, datasets: [{ label: '").append(sensor).append("', data: valores, borderColor: color, backgroundColor: color.replace('rgb','rgba').replace(')',',0.08)'), pointRadius:3, tension:0.3, fill:false }] },\n");
            sb.append("      options: { responsive: true, maintainAspectRatio: false, plugins: { title: { display: true, text: 'Sensor: ").append(sensor).append("' }, zoom: { zoom: { wheel: { enabled: true, speed: 0.1 }, pinch: { enabled: true }, mode: 'x' }, pan: { enabled: true, mode: 'x' } } }, interaction: { intersect:false, mode:'index' }, scales: { x: { display:true, title:{ display:true, text:'Tiempo' } } } }\n");
            sb.append("    });\n");
            sb.append("  })();\n");
            sensorIdx++;
        }
        return sb.toString();
    }

    private String generarArrayObjetos(List<DatoEnsayoTemporal> datos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < datos.size(); i++) {
            if (i > 0) sb.append(",");
            DatoEnsayoTemporal d = datos.get(i);
            String sensor = d.getSensor() != null ? d.getSensor().replace("\"", "\\\"") : "Sin Sensor";
            String ts = d.getTimestamp() != null ? d.getTimestamp().toString() : "";
            Double val = d.getValor() != null ? d.getValor() : 0.0;
            Boolean anormal = d.getAnormal() != null ? d.getAnormal() : false;
            sb.append("{timestamp:\"").append(escaparHtml(ts)).append("\", sensor:\"").append(escaparHtml(sensor)).append("\", valor:").append(val).append(", anormal:").append(anormal).append("}");
        }
        sb.append("]");
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
