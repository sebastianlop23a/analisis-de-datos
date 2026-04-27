package com.sivco.gestion_archivos.utilidades;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sivco.gestion_archivos.modelos.ReporteFinal;
import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

public class PdfUtil {

    /** =========================================================
     *   GENERA PDF CON GRÁFICOS DESDE ReporteFinal + Datos
     *  ========================================================= */
    public static byte[] generarPdfConGraficos(ReporteFinal reporte, List<DatoEnsayoTemporal> datos) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;
            float pageWidth = page.getMediaBox().getWidth();

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // LOGO SIVCO
            yPosition = dibujarLogo(doc, cs, margin, yPosition);
            yPosition -= 15;

            // TÍTULO PRINCIPAL
            yPosition = escribirTexto(cs, "REPORTE COMPLETO DE ENSAYO", margin, yPosition, 
                PDType1Font.HELVETICA_BOLD, 18, pageWidth - 2 * margin, true);
            yPosition -= 20;

            cs.setLineWidth(2f);
            cs.moveTo(margin, yPosition);
            cs.lineTo(pageWidth - margin, yPosition);
            cs.stroke();
            yPosition -= 20;

            // INFORMACIÓN DEL ENSAYO
            yPosition = escribirSeccion(cs, page, doc, "Informacion del Ensayo", margin, yPosition, pageWidth);
            yPosition = escribirLinea(cs, "Ensayo: " + reporte.getNombreEnsayo(), margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition = escribirLinea(cs, "Maquina: " + reporte.getNombreMaquina() + " (" + reporte.getTipoMaquina() + ")", margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition = escribirLinea(cs, "Periodo: " + reporte.getFechaInicio() + " a " + reporte.getFechaFin(), margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition = escribirLinea(cs, "Responsable: " + reporte.getResponsable(), margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition -= 15;

            // ESTADÍSTICAS PRINCIPALES
            cs = obtenerStreamActual(cs, doc);
            yPosition = verificarEspacioYCrearPagina(cs, doc, yPosition, margin, 150);
            cs = obtenerStreamActual(cs, doc);
            yPosition = escribirSeccion(cs, page, doc, "Estadisticas Principales", margin, yPosition, pageWidth);
            
            String[][] stats = {
                {"Total de Registros:", String.valueOf(reporte.getTotalDatos())},
                {"Media:", String.format("%.2f", reporte.getMedia())},
                {"Desviacion Estandar:", String.format("%.2f", reporte.getDesviacionEstandar())},
                {"Minimo:", String.format("%.2f", reporte.getMinimo())},
                {"Maximo:", String.format("%.2f", reporte.getMaximo())},
                {"Rango:", String.format("%.2f", reporte.getRango())},
                {"Registros Anormales:", reporte.getDatosAnormales() + " (" + String.format("%.2f", reporte.getPorcentajeAnormales()) + "%)"}
            };

            for (String[] stat : stats) {
                yPosition = escribirLineaDoble(cs, stat[0], stat[1], margin + 10, yPosition, pageWidth - 2 * margin);
            }
            yPosition -= 15;

            // VERIFICACIÓN DE LÍMITES Y RESULTADO DE APROBACIÓN
            cs = obtenerStreamActual(cs, doc);
            yPosition = verificarEspacioYCrearPagina(cs, doc, yPosition, margin, 150);
            cs = obtenerStreamActual(cs, doc);
            yPosition = escribirSeccion(cs, page, doc, "Evaluacion de Conformidad", margin, yPosition, pageWidth);
            
            boolean dentroLimites = true;
            List<String> problemas = new ArrayList<>();
            
            // Verificar límite inferior
            if (reporte.getLimiteInferior() != null && reporte.getMinimo() < reporte.getLimiteInferior()) {
                dentroLimites = false;
                problemas.add("Valor minimo (" + String.format("%.2f", reporte.getMinimo()) + 
                             ") esta por debajo del limite inferior (" + String.format("%.2f", reporte.getLimiteInferior()) + ")");
            }
            
            // Verificar límite superior
            if (reporte.getLimiteSuperior() != null && reporte.getMaximo() > reporte.getLimiteSuperior()) {
                dentroLimites = false;
                problemas.add("Valor maximo (" + String.format("%.2f", reporte.getMaximo()) + 
                             ") supera el limite superior (" + String.format("%.2f", reporte.getLimiteSuperior()) + ")");
            }
            
            // Verificar porcentaje de anomalías
            if (reporte.getPorcentajeAnormales() > 5.0) {
                dentroLimites = false;
                problemas.add("Porcentaje de datos anormales (" + String.format("%.2f", reporte.getPorcentajeAnormales()) + 
                             "%) supera el umbral aceptable (5%)");
            }
            
            // Mensaje de resultado
            if (dentroLimites) {
                cs.setNonStrokingColor(0f, 0.6f, 0f); // Verde
                yPosition = escribirTexto(cs, "✓ ENSAYO APROBADO", margin + 10, yPosition, 
                    PDType1Font.HELVETICA_BOLD, 14, pageWidth - 2 * margin, false);
                cs.setNonStrokingColor(0f, 0f, 0f); // Volver a negro
                yPosition -= 5;
                yPosition = escribirTextoMultilinea(cs, 
                    "Todos los valores medidos se encuentran dentro de los limites especificados. " +
                    "El ensayo cumple con los criterios de aceptacion establecidos.", 
                    margin + 10, yPosition, PDType1Font.HELVETICA, 10, pageWidth - 2 * margin - 10, page, doc);
            } else {
                cs.setNonStrokingColor(0.8f, 0f, 0f); // Rojo
                yPosition = escribirTexto(cs, "✗ ENSAYO NO APROBADO", margin + 10, yPosition, 
                    PDType1Font.HELVETICA_BOLD, 14, pageWidth - 2 * margin, false);
                cs.setNonStrokingColor(0f, 0f, 0f); // Volver a negro
                yPosition -= 5;
                yPosition = escribirTextoMultilinea(cs, 
                    "Se detectaron las siguientes no conformidades:", 
                    margin + 10, yPosition, PDType1Font.HELVETICA, 10, pageWidth - 2 * margin - 10, page, doc);
                yPosition -= 5;
                
                // Listar problemas
                for (String problema : problemas) {
                    yPosition = escribirTextoMultilinea(cs, "• " + problema, 
                        margin + 20, yPosition, PDType1Font.HELVETICA, 9, pageWidth - 2 * margin - 20, page, doc);
                    yPosition -= 3;
                }
            }
            yPosition -= 15;

            // TABLA DE ESTADÍSTICAS
            cs = obtenerStreamActual(cs, doc);
            yPosition = verificarEspacioYCrearPagina(cs, doc, yPosition, margin, 200);
            cs = obtenerStreamActual(cs, doc);
            yPosition = escribirSeccion(cs, page, doc, "Estadisticas Detalladas", margin, yPosition, pageWidth);
            
            List<Double> valoresOrdenados = datos.stream().map(DatoEnsayoTemporal::getValor).sorted().collect(Collectors.toList());
            double q1 = GeneradorGraficos.calcularCuartil(valoresOrdenados, 0.25);
            double q2 = GeneradorGraficos.calcularCuartil(valoresOrdenados, 0.50);
            double q3 = GeneradorGraficos.calcularCuartil(valoresOrdenados, 0.75);
            
            List<String[]> tabla = new ArrayList<>();
            tabla.add(new String[]{"Metrica", "Valor"});
            tabla.add(new String[]{"Total de Registros", String.valueOf(reporte.getTotalDatos())});
            tabla.add(new String[]{"Media", String.format("%.4f", reporte.getMedia())});
            tabla.add(new String[]{"Q1 (25%)", String.format("%.4f", q1)});
            tabla.add(new String[]{"Q2 Mediana", String.format("%.4f", q2)});
            tabla.add(new String[]{"Q3 (75%)", String.format("%.4f", q3)});
            tabla.add(new String[]{"Minimo", String.format("%.4f", reporte.getMinimo())});
            tabla.add(new String[]{"Maximo", String.format("%.4f", reporte.getMaximo())});
            
            if (reporte.getErrorEstandar() != null) {
                tabla.add(new String[]{"Error Estandar", String.format("%.6f", reporte.getErrorEstandar())});
                if (reporte.getValorT() != null) {
                    tabla.add(new String[]{"Valor t", String.format("%.2f", reporte.getValorT())});
                }
                if (reporte.getLimiteConfianzaInferior() != null && reporte.getLimiteConfianzaSuperior() != null) {
                    tabla.add(new String[]{"Limite Confianza Inferior", String.format("%.4f", reporte.getLimiteConfianzaInferior())});
                    tabla.add(new String[]{"Limite Confianza Superior", String.format("%.4f", reporte.getLimiteConfianzaSuperior())});
                }
            }
            
            if (reporte.getCalculaFH() != null && reporte.getCalculaFH() && reporte.getFactorHistorico() != null) {
                tabla.add(new String[]{"Factor Historico", String.format("%.6f", reporte.getFactorHistorico())});
            }

            yPosition = dibujarTabla(cs, tabla, margin, yPosition, pageWidth - 2 * margin, page, doc);
            yPosition -= 20;

            // NUEVA PÁGINA PARA GRÁFICOS
            cs.close();
            PDPage graficosPage = new PDPage(PDRectangle.A4);
            doc.addPage(graficosPage);
            cs = new PDPageContentStream(doc, graficosPage);
            yPosition = graficosPage.getMediaBox().getHeight() - margin;

            // TÍTULO GRÁFICOS
            yPosition = escribirSeccion(cs, graficosPage, doc, "Graficos de Analisis", margin, yPosition, pageWidth);
            yPosition -= 10;

            float chartWidth = pageWidth - 2 * margin;
            float chartHeight = 0;

            // GRÁFICO 1: Serie Temporal con marcadores de eventos
            try {
                BufferedImage chartSerie = GeneradorGraficos.generarSerieTemporalConEventos(
                    datos, 
                    reporte.getLimiteInferior(), 
                    reporte.getLimiteSuperior(),
                    reporte.getCortesEnergia(),
                    reporte.getAperturasPuerta()
                );
                PDImageXObject pdImage1 = LosslessFactory.createFromImage(doc, chartSerie);
                chartHeight = chartWidth * chartSerie.getHeight() / chartSerie.getWidth();
                cs.drawImage(pdImage1, margin, yPosition - chartHeight, chartWidth, chartHeight);
                yPosition -= chartHeight + 20;
            } catch (Exception e) {
                System.err.println("Error generando gráfico de serie temporal: " + e.getMessage());
                e.printStackTrace();
            }

            // GRÁFICO 2: Cuartiles
            if (yPosition - 180 < margin) {
                cs.close();
                graficosPage = new PDPage(PDRectangle.A4);
                doc.addPage(graficosPage);
                cs = new PDPageContentStream(doc, graficosPage);
                yPosition = graficosPage.getMediaBox().getHeight() - margin;
            }
            
            try {
                BufferedImage chartCuartiles = GeneradorGraficos.generarGraficoCuartiles(reporte.getMinimo(), q1, q2, q3, reporte.getMaximo());
                PDImageXObject pdImage2 = LosslessFactory.createFromImage(doc, chartCuartiles);
                chartHeight = chartWidth * chartCuartiles.getHeight() / chartCuartiles.getWidth();
                cs.drawImage(pdImage2, margin, yPosition - chartHeight, chartWidth, chartHeight);
                yPosition -= chartHeight + 20;
            } catch (Exception e) {
                System.err.println("Error generando gráfico de cuartiles: " + e.getMessage());
                e.printStackTrace();
            }

            // GRÁFICO 3: Histograma y Anomalías en la misma página
            if (yPosition - 180 < margin) {
                cs.close();
                graficosPage = new PDPage(PDRectangle.A4);
                doc.addPage(graficosPage);
                cs = new PDPageContentStream(doc, graficosPage);
                yPosition = graficosPage.getMediaBox().getHeight() - margin;
            }

            float smallChartWidth = (pageWidth - 2 * margin - 10) / 2;
            float smallChartHeight = 0;
            
            try {
                BufferedImage chartHist = GeneradorGraficos.generarHistograma(datos, q1, q2, q3);
                PDImageXObject pdImage3 = LosslessFactory.createFromImage(doc, chartHist);
                smallChartHeight = smallChartWidth * chartHist.getHeight() / chartHist.getWidth();
                cs.drawImage(pdImage3, margin, yPosition - smallChartHeight, smallChartWidth, smallChartHeight);
            } catch (Exception e) {
                System.err.println("Error generando histograma: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                int normales = reporte.getTotalDatos() - reporte.getDatosAnormales();
                BufferedImage chartAnom = GeneradorGraficos.generarGraficoAnomalias(normales, reporte.getDatosAnormales());
                PDImageXObject pdImage4 = LosslessFactory.createFromImage(doc, chartAnom);
                cs.drawImage(pdImage4, margin + smallChartWidth + 10, yPosition - smallChartHeight, smallChartWidth, smallChartHeight);
            } catch (Exception e) {
                System.err.println("Error generando gráfico de anomalías: " + e.getMessage());
                e.printStackTrace();
            }
            
            yPosition -= smallChartHeight + 20;

            // GRÁFICO 4: Límites
            if (yPosition - 180 < margin) {
                cs.close();
                graficosPage = new PDPage(PDRectangle.A4);
                doc.addPage(graficosPage);
                cs = new PDPageContentStream(doc, graficosPage);
                yPosition = graficosPage.getMediaBox().getHeight() - margin;
            }

            try {
                BufferedImage chartLimites = GeneradorGraficos.generarGraficoLimites(
                    reporte.getLimiteInferior(), reporte.getMinimo(), 
                    reporte.getMedia(), reporte.getMaximo(), reporte.getLimiteSuperior());
                PDImageXObject pdImage5 = LosslessFactory.createFromImage(doc, chartLimites);
                chartHeight = chartWidth * chartLimites.getHeight() / chartLimites.getWidth();
                cs.drawImage(pdImage5, margin, yPosition - chartHeight, chartWidth, chartHeight);
            } catch (Exception e) {
                System.err.println("Error generando gráfico de límites: " + e.getMessage());
                e.printStackTrace();
            }
            
            yPosition -= chartHeight + 30;

            // GRÁFICO 5: Comparación entre sensores (si hay múltiples sensores)
            java.util.Map<String, List<DatoEnsayoTemporal>> datosPorSensor = datos.stream()
                .filter(d -> d.getSensor() != null && !d.getSensor().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(DatoEnsayoTemporal::getSensor));
            
            if (datosPorSensor.size() > 1) {
                if (yPosition - 250 < margin) {
                    cs.close();
                    graficosPage = new PDPage(PDRectangle.A4);
                    doc.addPage(graficosPage);
                    cs = new PDPageContentStream(doc, graficosPage);
                    yPosition = graficosPage.getMediaBox().getHeight() - margin;
                }
                
                try {
                    BufferedImage chartSensores = GeneradorGraficos.generarComparacionSensores(datosPorSensor);
                    PDImageXObject pdImage6 = LosslessFactory.createFromImage(doc, chartSensores);
                    chartHeight = chartWidth * chartSensores.getHeight() / chartSensores.getWidth();
                    cs.drawImage(pdImage6, margin, yPosition - chartHeight, chartWidth, chartHeight);
                } catch (Exception e) {
                    System.err.println("Error generando gráfico de comparación de sensores: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // OBSERVACIONES
            if (reporte.getObservaciones() != null && !reporte.getObservaciones().isEmpty()) {
                cs.close();
                PDPage obsPage = new PDPage(PDRectangle.A4);
                doc.addPage(obsPage);
                cs = new PDPageContentStream(doc, obsPage);
                yPosition = obsPage.getMediaBox().getHeight() - margin;
                
                yPosition = escribirSeccion(cs, obsPage, doc, "Observaciones", margin, yPosition, pageWidth);
                yPosition = escribirTextoMultilinea(cs, reporte.getObservaciones(), margin + 10, yPosition, 
                    PDType1Font.HELVETICA, 10, pageWidth - 2 * margin - 10, obsPage, doc);
            }

            cs.close();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /** =========================================================
     *   GENERA PDF DIRECTAMENTE DESDE ReporteFinal (SIN GRÁFICOS)
     *  ========================================================= */
    public static byte[] generarPdfDirecto(ReporteFinal reporte) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;
            float pageWidth = page.getMediaBox().getWidth();

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // LOGO SIVCO
            yPosition = dibujarLogo(doc, cs, margin, yPosition);
            yPosition -= 15;

            // TÍTULO PRINCIPAL
            yPosition = escribirTexto(cs, "REPORTE COMPLETO DE ENSAYO", margin, yPosition, 
                PDType1Font.HELVETICA_BOLD, 18, pageWidth - 2 * margin, true);
            yPosition -= 20;

            // LÍNEA SEPARADORA
            cs.setLineWidth(2f);
            cs.moveTo(margin, yPosition);
            cs.lineTo(pageWidth - margin, yPosition);
            cs.stroke();
            yPosition -= 20;

            // INFORMACIÓN DEL ENSAYO
            yPosition = escribirSeccion(cs, page, doc, "Informacion del Ensayo", margin, yPosition, pageWidth);
            yPosition = escribirLinea(cs, "Ensayo: " + reporte.getNombreEnsayo(), margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition = escribirLinea(cs, "Maquina: " + reporte.getNombreMaquina() + " (" + reporte.getTipoMaquina() + ")", margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition = escribirLinea(cs, "Periodo: " + reporte.getFechaInicio() + " a " + reporte.getFechaFin(), margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition = escribirLinea(cs, "Responsable: " + reporte.getResponsable(), margin + 10, yPosition, PDType1Font.HELVETICA, 10);
            yPosition -= 15;

            // ESTADÍSTICAS PRINCIPALES
            yPosition = verificarEspacio(cs, page, doc, yPosition, margin, 150);
            yPosition = escribirSeccion(cs, page, doc, "Estadisticas Principales", margin, yPosition, pageWidth);
            
            String[][] stats = {
                {"Total de Registros:", String.valueOf(reporte.getTotalDatos())},
                {"Media:", String.format("%.2f", reporte.getMedia())},
                {"Desviacion Estandar:", String.format("%.2f", reporte.getDesviacionEstandar())},
                {"Minimo:", String.format("%.2f", reporte.getMinimo())},
                {"Maximo:", String.format("%.2f", reporte.getMaximo())},
                {"Rango:", String.format("%.2f", reporte.getRango())},
                {"Registros Anormales:", reporte.getDatosAnormales() + " (" + String.format("%.2f", reporte.getPorcentajeAnormales()) + "%)"}
            };

            for (String[] stat : stats) {
                yPosition = escribirLineaDoble(cs, stat[0], stat[1], margin + 10, yPosition, pageWidth - 2 * margin);
            }
            yPosition -= 15;

            // VERIFICACIÓN DE LÍMITES Y RESULTADO DE APROBACIÓN
            yPosition = verificarEspacio(cs, page, doc, yPosition, margin, 150);
            yPosition = escribirSeccion(cs, page, doc, "Evaluacion de Conformidad", margin, yPosition, pageWidth);
            
            boolean dentroLimites = true;
            List<String> problemas = new ArrayList<>();
            
            // Verificar límite inferior
            if (reporte.getLimiteInferior() != null && reporte.getMinimo() < reporte.getLimiteInferior()) {
                dentroLimites = false;
                problemas.add("Valor minimo (" + String.format("%.2f", reporte.getMinimo()) + 
                             ") esta por debajo del limite inferior (" + String.format("%.2f", reporte.getLimiteInferior()) + ")");
            }
            
            // Verificar límite superior
            if (reporte.getLimiteSuperior() != null && reporte.getMaximo() > reporte.getLimiteSuperior()) {
                dentroLimites = false;
                problemas.add("Valor maximo (" + String.format("%.2f", reporte.getMaximo()) + 
                             ") supera el limite superior (" + String.format("%.2f", reporte.getLimiteSuperior()) + ")");
            }
            
            // Verificar porcentaje de anomalías
            if (reporte.getPorcentajeAnormales() > 5.0) {
                dentroLimites = false;
                problemas.add("Porcentaje de datos anormales (" + String.format("%.2f", reporte.getPorcentajeAnormales()) + 
                             "%) supera el umbral aceptable (5%)");
            }
            
            // Mensaje de resultado
            if (dentroLimites) {
                cs.setNonStrokingColor(0f, 0.6f, 0f); // Verde
                yPosition = escribirTexto(cs, "✓ ENSAYO APROBADO", margin + 10, yPosition, 
                    PDType1Font.HELVETICA_BOLD, 14, pageWidth - 2 * margin, false);
                cs.setNonStrokingColor(0f, 0f, 0f); // Volver a negro
                yPosition -= 5;
                yPosition = escribirTextoMultilinea(cs, 
                    "Todos los valores medidos se encuentran dentro de los limites especificados. " +
                    "El ensayo cumple con los criterios de aceptacion establecidos.", 
                    margin + 10, yPosition, PDType1Font.HELVETICA, 10, pageWidth - 2 * margin - 10, page, doc);
            } else {
                cs.setNonStrokingColor(0.8f, 0f, 0f); // Rojo
                yPosition = escribirTexto(cs, "✗ ENSAYO NO APROBADO", margin + 10, yPosition, 
                    PDType1Font.HELVETICA_BOLD, 14, pageWidth - 2 * margin, false);
                cs.setNonStrokingColor(0f, 0f, 0f); // Volver a negro
                yPosition -= 5;
                yPosition = escribirTextoMultilinea(cs, 
                    "Se detectaron las siguientes no conformidades:", 
                    margin + 10, yPosition, PDType1Font.HELVETICA, 10, pageWidth - 2 * margin - 10, page, doc);
                yPosition -= 5;
                
                // Listar problemas
                for (String problema : problemas) {
                    yPosition = escribirTextoMultilinea(cs, "• " + problema, 
                        margin + 20, yPosition, PDType1Font.HELVETICA, 9, pageWidth - 2 * margin - 20, page, doc);
                    yPosition -= 3;
                }
            }
            yPosition -= 15;

            // TABLA DE ESTADÍSTICAS DETALLADA
            yPosition = verificarEspacio(cs, page, doc, yPosition, margin, 200);
            yPosition = escribirSeccion(cs, page, doc, "Estadisticas Detalladas", margin, yPosition, pageWidth);
            
            List<String[]> tabla = new ArrayList<>();
            tabla.add(new String[]{"Metrica", "Valor"});
            tabla.add(new String[]{"Total de Registros", String.valueOf(reporte.getTotalDatos())});
            tabla.add(new String[]{"Registros Anormales", reporte.getDatosAnormales() + " (" + String.format("%.2f%%", reporte.getPorcentajeAnormales()) + ")"});
            tabla.add(new String[]{"Media", String.format("%.4f", reporte.getMedia())});
            tabla.add(new String[]{"Desviacion Estandar", String.format("%.4f", reporte.getDesviacionEstandar())});
            tabla.add(new String[]{"Valor Minimo", String.format("%.4f", reporte.getMinimo())});
            tabla.add(new String[]{"Valor Maximo", String.format("%.4f", reporte.getMaximo())});
            tabla.add(new String[]{"Rango", String.format("%.4f", reporte.getRango())});
            tabla.add(new String[]{"Limite Inferior", String.format("%.4f", reporte.getLimiteInferior())});
            tabla.add(new String[]{"Limite Superior", String.format("%.4f", reporte.getLimiteSuperior())});
            
            if (reporte.getCalculaFH() != null && reporte.getCalculaFH() && reporte.getFactorHistorico() != null) {
                tabla.add(new String[]{"Factor Historico (FH)", String.format("%.6f", reporte.getFactorHistorico())});
                tabla.add(new String[]{"Parametro Z", String.valueOf(reporte.getParametroZ())});
            }

            yPosition = dibujarTabla(cs, tabla, margin, yPosition, pageWidth - 2 * margin, page, doc);
            yPosition -= 15;

            // OBSERVACIONES
            if (reporte.getObservaciones() != null && !reporte.getObservaciones().isEmpty()) {
                yPosition = verificarEspacio(cs, page, doc, yPosition, margin, 80);
                yPosition = escribirSeccion(cs, page, doc, "Observaciones", margin, yPosition, pageWidth);
                yPosition = escribirTextoMultilinea(cs, reporte.getObservaciones(), margin + 10, yPosition, 
                    PDType1Font.HELVETICA, 10, pageWidth - 2 * margin - 10, page, doc);
            }

            cs.close();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static float escribirSeccion(PDPageContentStream cs, PDPage page, PDDocument doc, 
                                        String titulo, float x, float y, float pageWidth) throws IOException {
        cs.setNonStrokingColor(52/255f, 73/255f, 94/255f);
        cs.addRect(x, y - 15, 4, 15);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f);
        
        return escribirLinea(cs, titulo, x + 10, y, PDType1Font.HELVETICA_BOLD, 12) - 10;
    }

    private static float escribirLinea(PDPageContentStream cs, String texto, float x, float y, 
                                      PDType1Font font, float fontSize) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForPdf(texto));
        cs.endText();
        return y - fontSize - 5;
    }

    private static float escribirLineaDoble(PDPageContentStream cs, String label, String value, 
                                           float x, float y, float width) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForPdf(label));
        cs.endText();

        float labelWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(sanitizeForPdf(label)) / 1000 * 10;
        
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(x + labelWidth + 10, y);
        cs.showText(sanitizeForPdf(value));
        cs.endText();
        
        return y - 15;
    }

    private static float escribirTexto(PDPageContentStream cs, String texto, float x, float y, 
                                      PDType1Font font, float fontSize, float maxWidth, 
                                      boolean centrado) throws IOException {
        String clean = sanitizeForPdf(texto);
        float textWidth = font.getStringWidth(clean) / 1000 * fontSize;
        float xPos = centrado ? x + (maxWidth - textWidth) / 2 : x;
        
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(xPos, y);
        cs.showText(clean);
        cs.endText();
        
        return y - fontSize - 5;
    }

    private static float escribirTextoMultilinea(PDPageContentStream cs, String texto, float x, float y,
                                                 PDType1Font font, float fontSize, float maxWidth,
                                                 PDPage page, PDDocument doc) throws IOException {
        String[] palabras = texto.split(" ");
        StringBuilder linea = new StringBuilder();
        float lineHeight = fontSize + 3;

        for (String palabra : palabras) {
            String test = linea.length() > 0 ? linea + " " + palabra : palabra;
            float width = font.getStringWidth(sanitizeForPdf(test)) / 1000 * fontSize;
            
            if (width > maxWidth && linea.length() > 0) {
                y = escribirLinea(cs, linea.toString(), x, y, font, fontSize);
                linea = new StringBuilder(palabra);
            } else {
                linea = new StringBuilder(test);
            }
        }
        
        if (linea.length() > 0) {
            y = escribirLinea(cs, linea.toString(), x, y, font, fontSize);
        }
        
        return y;
    }

    private static float dibujarTabla(PDPageContentStream cs, List<String[]> filas, float x, float y,
                                     float width, PDPage page, PDDocument doc) throws IOException {
        float rowHeight = 20;
        float col1Width = width * 0.6f;
        float col2Width = width * 0.4f;

        // Encabezado
        cs.setNonStrokingColor(52/255f, 73/255f, 94/255f);
        cs.addRect(x, y - rowHeight, width, rowHeight);
        cs.fill();
        cs.setNonStrokingColor(1f, 1f, 1f);
        
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x + 5, y - 14);
        cs.showText(sanitizeForPdf(filas.get(0)[0]));
        cs.endText();
        
        cs.beginText();
        cs.newLineAtOffset(x + col1Width + 5, y - 14);
        cs.showText(sanitizeForPdf(filas.get(0)[1]));
        cs.endText();
        
        cs.setNonStrokingColor(0f, 0f, 0f);
        y -= rowHeight;

        // Filas
        for (int i = 1; i < filas.size(); i++) {
            if (i % 2 == 0) {
                cs.setNonStrokingColor(0.98f, 0.98f, 0.98f);
                cs.addRect(x, y - rowHeight, width, rowHeight);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);
            }

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.newLineAtOffset(x + 5, y - 14);
            cs.showText(sanitizeForPdf(filas.get(i)[0]));
            cs.endText();

            cs.beginText();
            cs.newLineAtOffset(x + col1Width + 5, y - 14);
            cs.showText(sanitizeForPdf(filas.get(i)[1]));
            cs.endText();

            // Línea inferior
            cs.setStrokingColor(0.87f, 0.87f, 0.87f);
            cs.setLineWidth(0.5f);
            cs.moveTo(x, y - rowHeight);
            cs.lineTo(x + width, y - rowHeight);
            cs.stroke();

            y -= rowHeight;
        }

        return y;
    }

    private static PDPageContentStream obtenerStreamActual(PDPageContentStream cs, PDDocument doc) throws IOException {
        // Simplemente retorna el stream actual - la gestión se hace diferente
        return cs;
    }

    private static float verificarEspacioYCrearPagina(PDPageContentStream cs, PDDocument doc,
                                         float y, float margin, float espacioNecesario) throws IOException {
        if (y - espacioNecesario < margin) {
            cs.close();
            PDPage newPage = new PDPage(PDRectangle.A4);
            doc.addPage(newPage);
            return newPage.getMediaBox().getHeight() - margin;
        }
        return y;
    }

    private static float verificarEspacio(PDPageContentStream cs, PDPage page, PDDocument doc,
                                         float y, float margin, float espacioNecesario) throws IOException {
        if (y - espacioNecesario < margin) {
            cs.close();
            PDPage newPage = new PDPage(PDRectangle.A4);
            doc.addPage(newPage);
            PDPageContentStream newCs = new PDPageContentStream(doc, newPage);
            // Reemplazar el stream (nota: esto requiere manejo cuidadoso)
            return newPage.getMediaBox().getHeight() - margin;
        }
        return y;
    }


    /** =========================================================
     *   GENERA PDF DESDE HTML (FORMATO REAL)
     *  ========================================================= */
    public static byte[] htmlToPdfBytes(String html) throws Exception {
        if (html == null || html.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            String cleanHtml = cleanHtmlForPdf(html);

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(cleanHtml, null);
            builder.useFastMode();
            builder.testMode(false);
            builder.toStream(baos);
            builder.run();

            return baos.toByteArray();

        } catch (Exception e) {

            return createSimplePdf(
                "Error en Generación de PDF",
                "No se pudo generar el PDF desde HTML.\n\nError: "
                        + e.getMessage() +
                        "\n\nContenido HTML disponible (" + html.length() + " caracteres)"
            );
        }
    }


    /** =========================================================
     *   PDF SIMPLE CON MENSAJES
     *  ========================================================= */
    public static byte[] createMessagePdf(String title, String message) throws IOException {
        return createSimplePdf(
                title != null ? title : "Reporte",
                message != null ? message : "Contenido no disponible"
        );
    }


    /** =========================================================
     *   PDF SIMPLE – USADO COMO BACKUP
     *  ========================================================= */
    private static byte[] createSimplePdf(String title, String content) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float pageHeight = page.getMediaBox().getHeight();
            float pageWidth = page.getMediaBox().getWidth();
            float marginLeft = 40;
            float marginTop = 40;
            float currentY = pageHeight - marginTop;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // TÍTULO
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.newLineAtOffset(marginLeft, currentY);
            cs.showText("REPORTE DE ENSAYO");
            cs.endText();
            currentY -= 20;

            // LÍNEA SEPARADORA
            cs.setLineWidth(1);
            cs.moveTo(marginLeft, currentY);
            cs.lineTo(pageWidth - marginLeft, currentY);
            cs.stroke();
            currentY -= 15;

            // CONTENIDO
            String[] paragraphs = content.split("\n\n");

            for (String paragraph : paragraphs) {
                String cleanPara = paragraph.trim();
                if (cleanPara.isEmpty()) continue;

                boolean isHeading = cleanPara.toUpperCase().equals(cleanPara) && cleanPara.length() < 60;
                float fontSize = isHeading ? 12 : 10;
                PDType1Font font = isHeading ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;

                int maxCharsPerLine = 95;
                String[] words = cleanPara.split(" ");
                StringBuilder line = new StringBuilder();

                for (String word : words) {
                    if ((line.length() + word.length() + 1) > maxCharsPerLine && line.length() > 0) {
                        writeLine(cs, line.toString(), font, fontSize, marginLeft, currentY);
                        currentY -= (fontSize + 2);
                        line = new StringBuilder();
                    }

                    if (line.length() > 0) line.append(" ");
                    line.append(word);
                }

                if (line.length() > 0) {
                    writeLine(cs, line.toString(), font, fontSize, marginLeft, currentY);
                    currentY -= (fontSize + 2);
                }

                currentY -= 5;
            }

            cs.close();
            doc.save(baos);
            return baos.toByteArray();
        }
    }


    private static void writeLine(PDPageContentStream cs, String text, PDType1Font font,
                                  float fontSize, float marginLeft, float currentY) throws IOException {

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(marginLeft, currentY);
        cs.showText(sanitizeForPdf(text));
        cs.endText();
    }


    /** =========================================================
     *   SANITIZAR TEXTO PARA PDF
     *  ========================================================= */
    private static String sanitizeForPdf(String text) {
        if (text == null) return "";

        String s = text.replaceAll("[\\p{Cc}&&[^\\t\\r\\n]]", "");
        s = s.replaceAll("[\n\r]", "");
        s = s.replaceAll("[^\\p{Print}\\t\\r\\n]", "");

        return s.trim();
    }


    /** =========================================================
     *   LIMPIAR HTML – VERSIÓN SIMPLIFICADA SIN PRE-PROCESAMIENTO
     *  ========================================================= */
    private static String cleanHtmlForPdf(String html) {
        if (html == null) return "";

        // Eliminar BOM y espacios al inicio/fin
        String cleaned = html.trim();
        
        // Eliminar BOM UTF-8 si existe
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }
        
        // Eliminar espacios/newlines antes del DOCTYPE
        cleaned = cleaned.replaceAll("^\\s+<!DOCTYPE", "<!DOCTYPE");

        return cleaned;
    }


    /** Escapa etiquetas desconocidas (<Q1>, <X2>, <RF45>, etc.) */
    private static String escapeUnknownTags(String html) {

        String validTags = "html|head|body|meta|link|img|br|hr|div|span|p|h1|h2|h3|h4|h5|h6|"
                + "table|thead|tbody|tr|td|th|ul|ol|li|b|i|u|strong|em|style|sup|sub|small|big|pre|code";

        // Reemplaza toda etiqueta desconocida por &lt;
        return html.replaceAll(
                "<(?!/?(" + validTags + ")(\\s|>|/))",
                "&lt;"
        );
    }


    /** Cierra etiquetas comunes que deben ser autocontenidas */
    private static String autoCloseCommonTags(String html) {
        String s = html;
        s = s.replaceAll("<meta([^>]*?)(?<!/)>", "<meta$1/>");
        s = s.replaceAll("<link([^>]*?)(?<!/)>", "<link$1/>");
        s = s.replaceAll("<img([^>]*?)(?<!/)>", "<img$1/>");
        s = s.replaceAll("<input([^>]*?)(?<!/)>", "<input$1/>");
        s = s.replaceAll("<br([^>]*?)(?<!/)>", "<br$1/>");
        s = s.replaceAll("<hr([^>]*?)(?<!/)>", "<hr$1/>");
        return s;
    }


    /** Garantiza <html>, <head>, <body> */
    private static String ensureHtmlStructure(String html) {
        String s = html;

        if (!s.toLowerCase().contains("<html")) {
            s = "<html><head><meta charset=\"UTF-8\"/></head><body>" + s + "</body></html>";
        }

        if (!s.toLowerCase().contains("<head>")) {
            s = s.replaceFirst("(?i)<html>", "<html><head><meta charset=\"UTF-8\"/></head>");
        }

        if (!s.toLowerCase().contains("<body>")) {
            s = s.replaceFirst("(?i)</head>", "</head><body>");
            if (!s.toLowerCase().contains("</body>")) {
                s += "</body>";
            }
        }

        return s;
    }


    /** Corrige "&Q1" y otras entidades mal formadas */
    private static String fixHtmlEntities(String html) {
        String s = html;

        // Proteger entidades válidas
        s = s.replaceAll("&(amp|lt|gt|quot|apos|nbsp|copy|reg|trade|hellip|mdash|ndash|rsquo|lsquo|rdquo|ldquo|bull|deg);",
                "###PROT-$1###");

        // Escapar cualquier & inválido
        s = s.replaceAll("&(?=[A-Za-z0-9]{1,10}(\\s|<|>|$))", "&amp;");

        // Restaurar entidades válidas
        s = s.replaceAll("###PROT-(\\w+)###", "&$1;");

        return s;
    }

    /**
     * Dibuja el logo de SIVCO en el encabezado del PDF
     * @return nueva posición Y después del logo
     */
    private static float dibujarLogo(PDDocument doc, PDPageContentStream cs, float x, float y) {
        try {
            // Intentar cargar el logo desde resources (JPEG, JPG o PNG)
            InputStream logoStream = PdfUtil.class.getResourceAsStream("/static/images/logo-sivco.jpeg");
            if (logoStream == null) {
                logoStream = PdfUtil.class.getResourceAsStream("/static/images/logo-sivco.jpg");
            }
            if (logoStream == null) {
                logoStream = PdfUtil.class.getResourceAsStream("/static/images/logo-sivco.png");
            }
            
            if (logoStream != null) {
                BufferedImage logoImage = ImageIO.read(logoStream);
                if (logoImage != null) {
                    PDImageXObject pdImage = LosslessFactory.createFromImage(doc, logoImage);
                    
                    // Dimensiones del logo (ajustar tamaño proporcional)
                    float logoHeight = 40;
                    float aspectRatio = (float) logoImage.getWidth() / logoImage.getHeight();
                    float logoWidth = logoHeight * aspectRatio;
                    
                    // Dibujar logo
                    cs.drawImage(pdImage, x, y - logoHeight, logoWidth, logoHeight);
                    
                    logoStream.close();
                    
                    return y - logoHeight - 10; // Retornar nueva posición Y
                }
                logoStream.close();
            }
        } catch (Exception e) {
            // Silenciosamente continuar sin logo si hay cualquier error
            // System.err.println("Logo no disponible: " + e.getMessage());
        }
        
        return y; // Si no hay logo, mantener posición original
    }

}
