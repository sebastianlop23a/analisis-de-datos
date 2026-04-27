package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CargaDatosServicioTest {

    @Autowired
    private CargaDatosServicio cargaDatosServicio;

    @Test
    void testCargarDatosCSV_WithRealFile() throws IOException {
        // Simular carga del archivo CSV real
        String filePath = "C:\\Users\\sebastisan lopez\\Downloads\\PQ_1.CSV";
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        MultipartFile mockFile = new MockMultipartFile("archivo", "PQ_1.CSV", "text/csv", fileContent);

        // Cargar datos con nuevo parser
        List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosCSV(mockFile, 1L);

        // Verificar cantidad de registros leídos
        System.out.println("\n=== RESULTADO DE CARGA ===");
        System.out.println("Total registros leídos: " + datos.size());
        System.out.println("Primeros 3 registros:");
        datos.stream().limit(3).forEach(d -> System.out.println("  - Sensor: " + d.getSensor() + ", Valor: " + d.getValor() + ", Timestamp: " + d.getTimestamp() + ", Fecha: " + d.getTimestamp().toLocalDate()));
        
        System.out.println("Últimos 3 registros:");
        datos.stream().skip(Math.max(0, datos.size() - 3)).forEach(d -> System.out.println("  - Sensor: " + d.getSensor() + ", Valor: " + d.getValor() + ", Timestamp: " + d.getTimestamp() + ", Fecha: " + d.getTimestamp().toLocalDate()));

        // Afirmaciones básicas
        assertNotNull(datos, "Los datos no deben ser nulos");
        assertTrue(datos.size() > 0, "Debe haber al menos 1 registro");
        
        // Verificar que las fechas NO son todas hoy (eso indicaría que se está usando la fecha actual)
        LocalDate today = LocalDate.now();
        long countWithTodayDate = datos.stream().filter(d -> d.getTimestamp().toLocalDate().equals(today)).count();
        long totalRecords = datos.size();
        
        System.out.println("\nVerificación de fechas:");
        System.out.println("  Total registros: " + totalRecords);
        System.out.println("  Registros con fecha de hoy: " + countWithTodayDate);
        
        if (countWithTodayDate == totalRecords) {
            System.out.println("  ✗ PROBLEMA: Todos los registros tienen la fecha de hoy (probablemente se está ignorando la fecha del archivo)");
        } else if (countWithTodayDate == 0) {
            System.out.println("  ✓ ÉXITO: Se están leyendo las fechas del archivo correctamente");
        }

        // El archivo PQ_1.CSV tiene ~316 líneas de datos * 4 sensores = ~1264 registros esperados
        int expectedMinimum = 1200;
        System.out.println("\nExpectativa: >= " + expectedMinimum + " registros");
        if (datos.size() >= expectedMinimum) {
            System.out.println("✓ ÉXITO: Se leyeron " + datos.size() + " registros (más de lo esperado)");
        } else {
            System.out.println("⚠ ADVERTENCIA: Se leyeron " + datos.size() + " registros (menos de " + expectedMinimum + ")");
        }
    }

    @Test
    void testCargarDatosCSV_CountFormatIssues() throws IOException {
        String filePath = "C:\\Users\\sebastisan lopez\\Downloads\\PQ_1.CSV";
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        
        // Contar líneas manuales en el archivo
        String content = new String(fileContent);
        String[] lines = content.split("\n");
        System.out.println("Total de líneas en archivo: " + lines.length);
        
        int dataLines = 0;
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith(";")) {
                dataLines++;
            }
        }
        System.out.println("Total de líneas con datos: " + dataLines);
        System.out.println("Primeras 10 líneas:");
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            String lineContent = lines[i].trim();
            int maxLen = Math.min(100, lineContent.length());
            System.out.println("  [" + i + "] " + lineContent.substring(0, maxLen));
        }
    }
}
