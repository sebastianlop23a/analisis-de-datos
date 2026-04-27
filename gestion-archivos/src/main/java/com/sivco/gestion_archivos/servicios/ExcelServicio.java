package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelServicio {
    
    public byte[] generarExcelDatos(List<DatoEnsayoTemporal> datos, String nombreEnsayo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos Ensayo");
            
            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Secuencia", "Timestamp", "Valor", "Anormal", "Fuente"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Agregar datos
            CellStyle anormalStyle = workbook.createCellStyle();
            anormalStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            anormalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            int rowNum = 1;
            for (DatoEnsayoTemporal dato : datos) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(dato.getNumeroSecuencia());
                row.createCell(1).setCellValue(dato.getTimestamp().toString());
                row.createCell(2).setCellValue(dato.getValor());
                
                Cell anormalCell = row.createCell(3);
                anormalCell.setCellValue(dato.getAnormal() ? "SÍ" : "NO");
                if (dato.getAnormal()) {
                    anormalCell.setCellStyle(anormalStyle);
                }
                
                row.createCell(4).setCellValue(dato.getFuente());
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    public byte[] generarExcelReporte(
            String nombreEnsayo,
            String maquina,
            String tipo,
            Double temperatura,
            Double temperaturaCorporal,
            Double media,
            Double desviacion,
            Integer datosAnormales,
            Integer totalDatos,
            Double factorHistorico,
            Double parametroZ) throws IOException {
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte");
            
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            
            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelStyle.setFont(labelFont);
            
            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE ENSAYO");
            titleCell.setCellStyle(titleStyle);
            
            rowNum++; // Saltar fila
            
            // Información general
            addRow(sheet, rowNum++, "Nombre del Ensayo:", nombreEnsayo, labelStyle);
            addRow(sheet, rowNum++, "Máquina:", maquina, labelStyle);
            addRow(sheet, rowNum++, "Tipo de Máquina:", tipo, labelStyle);
            
            rowNum++; // Saltar fila
            
            // Estadísticas
            addRow(sheet, rowNum++, "Temperatura Promedio:", String.format("%.2f", media), labelStyle);
            addRow(sheet, rowNum++, "Temperatura Máxima:", String.format("%.2f", temperatura), labelStyle);
            addRow(sheet, rowNum++, "Temperatura Mínima:", String.format("%.2f", temperaturaCorporal), labelStyle);
            addRow(sheet, rowNum++, "Desviación Estándar:", String.format("%.2f", desviacion), labelStyle);
            addRow(sheet, rowNum++, "Total de Registros:", totalDatos.toString(), labelStyle);
            addRow(sheet, rowNum++, "Registros Anormales:", datosAnormales.toString(), labelStyle);
            
            // Factor Histórico (si aplica)
            if (factorHistorico != null) {
                rowNum++; // Saltar fila
                
                CellStyle fhStyle = workbook.createCellStyle();
                Font fhFont = workbook.createFont();
                fhFont.setBold(true);
                fhFont.setColor(IndexedColors.DARK_RED.getIndex());
                fhStyle.setFont(fhFont);
                fhStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                fhStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                
                addRow(sheet, rowNum++, "Factor Histórico (FH):", String.format("%.6f", factorHistorico), fhStyle);
                if (parametroZ != null) {
                    addRow(sheet, rowNum++, "Parámetro Z:", String.format("%.2f", parametroZ), labelStyle);
                }
                addRow(sheet, rowNum++, "Fórmula:", "FH = Σ(10^((Ti - 250)/z) · Δt)", labelStyle);
            }
            
            // Ajustar columnas
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private void addRow(Sheet sheet, int rowNum, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(style);
        row.createCell(1).setCellValue(value);
    }
}
