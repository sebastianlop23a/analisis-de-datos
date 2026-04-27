package com.sivco.gestion_archivos.utilidades;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

public class GeneradorGraficos {

    private static final int CHART_WIDTH = 1000;
    private static final int CHART_HEIGHT = 500;
    
    // Paleta de colores profesional
    private static final Color COLOR_PRIMARY = new Color(41, 128, 185);      // Azul moderno
    private static final Color COLOR_SUCCESS = new Color(39, 174, 96);       // Verde éxito
    private static final Color COLOR_DANGER = new Color(231, 76, 60);        // Rojo alerta
    private static final Color COLOR_WARNING = new Color(243, 156, 18);      // Naranja advertencia
    private static final Color COLOR_BACKGROUND = new Color(250, 250, 250);  // Gris muy claro
    private static final Color COLOR_GRID = new Color(230, 230, 230);        // Gris claro
    private static final Color COLOR_TEXT = new Color(52, 73, 94);           // Gris oscuro

    /**
     * Genera gráfico de serie temporal con límites - Diseño profesional
     */
    public static BufferedImage generarSerieTemporal(List<DatoEnsayoTemporal> datos, 
                                                     double limiteInferior, 
                                                     double limiteSuperior) {
        return generarSerieTemporalConEventos(datos, limiteInferior, limiteSuperior, null, null);
    }
    
    /**
     * Genera gráfico de serie temporal con límites y marcadores de eventos
     */
    public static BufferedImage generarSerieTemporalConEventos(
            List<DatoEnsayoTemporal> datos, 
            double limiteInferior, 
            double limiteSuperior,
            java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> cortesEnergia,
            java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> aperturasPuerta) {
        
        XYSeries series = new XYSeries("Valores medidos");
        XYSeries limSup = new XYSeries("Límite superior");
        XYSeries limInf = new XYSeries("Límite inferior");

        int maxPuntos = Math.min(datos.size(), 1000);
        int step = datos.size() > 1000 ? datos.size() / 1000 : 1;

        for (int i = 0; i < datos.size(); i += step) {
            DatoEnsayoTemporal dato = datos.get(i);
            series.add(i, dato.getValor());
            limSup.add(i, limiteSuperior);
            limInf.add(i, limiteInferior);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(limSup);
        dataset.addSeries(limInf);

        JFreeChart chart = ChartFactory.createXYLineChart(
            null, // Sin título en el gráfico (más limpio)
            "Secuencia de medición",
            "Valor",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            false,
            false
        );

        aplicarEstiloModerno(chart);
        
        XYPlot plot = chart.getXYPlot();
        configurarPlotXY(plot);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Serie principal con gradiente
        renderer.setSeriesPaint(0, COLOR_PRIMARY);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesShapesVisible(0, false);
        
        // Límites con líneas punteadas elegantes
        float[] dashPattern = {10.0f, 5.0f};
        renderer.setSeriesPaint(1, COLOR_DANGER);
        renderer.setSeriesStroke(1, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashPattern, 0.0f));
        renderer.setSeriesShapesVisible(1, false);
        
        renderer.setSeriesPaint(2, COLOR_DANGER);
        renderer.setSeriesStroke(2, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashPattern, 0.0f));
        renderer.setSeriesShapesVisible(2, false);
        
        plot.setRenderer(renderer);
        
        // Agregar marcadores de cortes de energía (áreas rojas)
        if (cortesEnergia != null && !cortesEnergia.isEmpty()) {
            java.util.Map<java.time.LocalDateTime, Integer> timestampToIndex = new java.util.HashMap<>();
            for (int i = 0; i < datos.size(); i++) {
                timestampToIndex.put(datos.get(i).getTimestamp(), i);
            }
            
            for (com.sivco.gestion_archivos.modelos.EventoCorteEnergia corte : cortesEnergia) {
                Integer indiceInicio = timestampToIndex.get(corte.getInicio());
                Integer indiceFin = timestampToIndex.get(corte.getFin());
                
                if (indiceInicio != null && indiceFin != null) {
                    org.jfree.chart.plot.IntervalMarker marcador = new org.jfree.chart.plot.IntervalMarker(
                        indiceInicio, indiceFin,
                        new Color(231, 76, 60, 50), // Rojo transparente
                        new BasicStroke(1.0f),
                        new Color(231, 76, 60, 100),
                        new BasicStroke(1.0f),
                        0.5f
                    );
                    marcador.setLabel("Corte");
                    marcador.setLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
                    plot.addDomainMarker(marcador);
                }
            }
        }
        
        // Agregar marcadores de aperturas de puerta (áreas naranjas)
        if (aperturasPuerta != null && !aperturasPuerta.isEmpty()) {
            java.util.Map<java.time.LocalDateTime, Integer> timestampToIndex = new java.util.HashMap<>();
            for (int i = 0; i < datos.size(); i++) {
                timestampToIndex.put(datos.get(i).getTimestamp(), i);
            }
            
            for (com.sivco.gestion_archivos.modelos.EventoCorteEnergia apertura : aperturasPuerta) {
                Integer indiceInicio = timestampToIndex.get(apertura.getInicio());
                Integer indiceFin = timestampToIndex.get(apertura.getFin());
                
                if (indiceInicio != null && indiceFin != null) {
                    org.jfree.chart.plot.IntervalMarker marcador = new org.jfree.chart.plot.IntervalMarker(
                        indiceInicio, indiceFin,
                        new Color(243, 156, 18, 50), // Naranja transparente
                        new BasicStroke(1.0f),
                        new Color(243, 156, 18, 100),
                        new BasicStroke(1.0f),
                        0.5f
                    );
                    marcador.setLabel("Apertura");
                    marcador.setLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
                    plot.addDomainMarker(marcador);
                }
            }
        }

        return chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
    }

    /**
     * Genera gráfico de barras para cuartiles - Diseño profesional
     */
    public static BufferedImage generarGraficoCuartiles(double min, double q1, double q2, 
                                                        double q3, double max) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(min, "Distribución", "Q0\nMínimo");
        dataset.addValue(q1, "Distribución", "Q1\n(25%)");
        dataset.addValue(q2, "Distribución", "Q2\nMediana");
        dataset.addValue(q3, "Distribución", "Q3\n(75%)");
        dataset.addValue(max, "Distribución", "Q4\nMáximo");

        JFreeChart chart = ChartFactory.createBarChart(
            null,
            null,
            "Valor",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        );

        aplicarEstiloModerno(chart);
        
        CategoryPlot plot = chart.getCategoryPlot();
        configurarPlotCategory(plot);
        
        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter()); // Sin efectos 3D
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        
        // Gradiente de colores de azul a verde
        GradientPaint gp = new GradientPaint(0.0f, 0.0f, COLOR_PRIMARY, 
                                             0.0f, 400.0f, COLOR_SUCCESS);
        renderer.setSeriesPaint(0, gp);
        renderer.setMaximumBarWidth(0.15);
        
        plot.setRenderer(renderer);

        return chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
    }

    /**
     * Genera histograma de distribución - Diseño profesional
     */
    public static BufferedImage generarHistograma(List<DatoEnsayoTemporal> datos, 
                                                  double q1, double q2, double q3) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        long cnt1 = datos.stream().filter(d -> d.getValor() < q1).count();
        long cnt2 = datos.stream().filter(d -> d.getValor() >= q1 && d.getValor() < q2).count();
        long cnt3 = datos.stream().filter(d -> d.getValor() >= q2 && d.getValor() < q3).count();
        long cnt4 = datos.stream().filter(d -> d.getValor() >= q3).count();

        dataset.addValue(cnt1, "Frecuencia", "Rango bajo\n(< Q1)");
        dataset.addValue(cnt2, "Frecuencia", "Rango medio-bajo\n(Q1-Q2)");
        dataset.addValue(cnt3, "Frecuencia", "Rango medio-alto\n(Q2-Q3)");
        dataset.addValue(cnt4, "Frecuencia", "Rango alto\n(> Q3)");

        JFreeChart chart = ChartFactory.createBarChart(
            null,
            null,
            "Cantidad de mediciones",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        );

        aplicarEstiloModerno(chart);
        
        CategoryPlot plot = chart.getCategoryPlot();
        configurarPlotCategory(plot);
        
        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        
        // Colores degradados por barra
        renderer.setSeriesPaint(0, new Color(52, 152, 219, 200));
        renderer.setMaximumBarWidth(0.20);
        
        plot.setRenderer(renderer);

        return chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
    }

    /**
     * Genera gráfico de pastel para anomalías - Diseño profesional
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static BufferedImage generarGraficoAnomalias(int normales, int anormales) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Valores normales (" + normales + ")", normales);
        dataset.setValue("Valores anormales (" + anormales + ")", anormales);

        JFreeChart chart = ChartFactory.createPieChart(
            null,
            dataset,
            true,
            false,
            false
        );

        aplicarEstiloModerno(chart);
        
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(COLOR_BACKGROUND);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        plot.setSectionPaint("Valores normales (" + normales + ")", COLOR_SUCCESS);
        plot.setSectionPaint("Valores anormales (" + anormales + ")", COLOR_DANGER);
        
        plot.setSimpleLabels(true);
        plot.setCircular(true);

        return chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
    }

    /**
     * Genera gráfico comparativo de límites - Diseño profesional
     */
    public static BufferedImage generarGraficoLimites(double limInf, double min, 
                                                      double media, double max, 
                                                      double limSup) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(limInf, "Valores", "Límite\ninferior");
        dataset.addValue(min, "Valores", "Mínimo\nmedido");
        dataset.addValue(media, "Valores", "Media\ngeneral");
        dataset.addValue(max, "Valores", "Máximo\nmedido");
        dataset.addValue(limSup, "Valores", "Límite\nsuperior");

        JFreeChart chart = ChartFactory.createBarChart(
            null,
            null,
            "Valor",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        );

        aplicarEstiloModerno(chart);
        
        CategoryPlot plot = chart.getCategoryPlot();
        configurarPlotCategory(plot);
        
        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        
        GradientPaint gp = new GradientPaint(0.0f, 0.0f, COLOR_WARNING, 
                                             0.0f, 400.0f, COLOR_PRIMARY);
        renderer.setSeriesPaint(0, gp);
        renderer.setMaximumBarWidth(0.15);
        
        plot.setRenderer(renderer);

        return chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
    }

    /**
     * Aplica estilo moderno al gráfico
     */
    private static void aplicarEstiloModerno(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(Color.WHITE);
            chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
    }

    /**
     * Configura plot XY con estilo profesional
     */
    private static void configurarPlotXY(XYPlot plot) {
        plot.setBackgroundPaint(COLOR_BACKGROUND);
        plot.setDomainGridlinePaint(COLOR_GRID);
        plot.setRangeGridlinePaint(COLOR_GRID);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        
        plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelPaint(COLOR_TEXT);
        plot.getDomainAxis().setTickLabelPaint(COLOR_TEXT);
        
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelPaint(COLOR_TEXT);
        plot.getRangeAxis().setTickLabelPaint(COLOR_TEXT);
    }

    /**
     * Configura plot de categorías con estilo profesional
     */
    private static void configurarPlotCategory(CategoryPlot plot) {
        plot.setBackgroundPaint(COLOR_BACKGROUND);
        plot.setDomainGridlinePaint(COLOR_GRID);
        plot.setRangeGridlinePaint(COLOR_GRID);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);
        
        plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelPaint(COLOR_TEXT);
        plot.getDomainAxis().setTickLabelPaint(COLOR_TEXT);
        
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelPaint(COLOR_TEXT);
        plot.getRangeAxis().setTickLabelPaint(COLOR_TEXT);
    }

    /**
     * Calcula cuartil
     */
    public static double calcularCuartil(List<Double> valores, double percentil) {
        if (valores.isEmpty()) return 0;
        List<Double> sorted = valores.stream().sorted().collect(Collectors.toList());
        int indice = (int) Math.ceil(percentil * sorted.size()) - 1;
        return sorted.get(Math.max(0, indice));
    }
    
    /**
     * Genera gráfico comparativo entre sensores
     */
    public static BufferedImage generarComparacionSensores(java.util.Map<String, List<DatoEnsayoTemporal>> datosPorSensor) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (String sensor : datosPorSensor.keySet()) {
            List<DatoEnsayoTemporal> datos = datosPorSensor.get(sensor);
            double media = datos.stream().mapToDouble(DatoEnsayoTemporal::getValor).average().orElse(0);
            double min = datos.stream().mapToDouble(DatoEnsayoTemporal::getValor).min().orElse(0);
            double max = datos.stream().mapToDouble(DatoEnsayoTemporal::getValor).max().orElse(0);
            
            dataset.addValue(media, "Media", sensor);
            dataset.addValue(min, "Mínimo", sensor);
            dataset.addValue(max, "Máximo", sensor);
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Comparación de Sensores",
            "Sensor",
            "Valor",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        aplicarEstiloModerno(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        configurarPlotCategory(plot);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        
        // Colores para media, min, max
        renderer.setSeriesPaint(0, COLOR_PRIMARY);   // Media - azul
        renderer.setSeriesPaint(1, COLOR_SUCCESS);   // Mínimo - verde
        renderer.setSeriesPaint(2, COLOR_DANGER);    // Máximo - rojo

        return chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
    }
}
