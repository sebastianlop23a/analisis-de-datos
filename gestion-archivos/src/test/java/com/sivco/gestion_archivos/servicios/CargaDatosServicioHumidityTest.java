package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.CalibrationCorrection;
import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.Maquina;
import com.sivco.gestion_archivos.modelos.Sensor;
import com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio;
import com.sivco.gestion_archivos.servicios.calibration.CalibrationManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class CargaDatosServicioHumidityTest {

    @Autowired
    private CargaDatosServicio cargaDatosServicio;

    @MockBean
    private EnsayoServicio ensayoServicio;

    @MockBean
    private CalibrationCorrectionServicio calibrationServicio;

    @MockBean
    private CalibrationManagementService calibrationManagementService;

    @MockBean
    private SensorServicio sensorServicio;

    @MockBean
    private CalibrationCorrectionRepositorio calibrationCorrectionRepositorio;

    @MockBean
    private PdfParsingService pdfParsingService;

    @MockBean
    private SivcoLoggerPdfService sivcoLoggerPdfService;

    @MockBean
    private com.sivco.gestion_archivos.repositorios.DatoEnsayoTemporalRepositorio datoEnsayoTemporalRepositorio;

    @Test
    void shouldResolveHumiditySensorToBaseSensorDeviceId() throws Exception {
        Sensor baseSensor = new Sensor();
        baseSensor.setId(1L);
        baseSensor.setCodigo("sensor_1");
        baseSensor.setActivo(true);

        when(sensorServicio.listarActivos()).thenReturn(List.of(baseSensor));

        Method method = CargaDatosServicio.class.getDeclaredMethod("resolveSensorDeviceIds", Set.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Long> resolved = (Map<String, Long>) method.invoke(cargaDatosServicio, Set.of("sensor_1_hr"));

        assertEquals(1L, resolved.get("sensor_1_hr"));
        assertEquals(1L, resolved.get("sensor_1"));
    }

    @Test
    void shouldApplyLegacyCorrectionToHumidityWhenOnlyBaseSensorCalibrationExists() throws Exception {
        Sensor baseSensor = new Sensor();
        baseSensor.setId(1L);
        baseSensor.setCodigo("sensor_1");
        baseSensor.setActivo(true);

        when(sensorServicio.listarActivos()).thenReturn(List.of(baseSensor));
        when(calibrationManagementService.getActiveCalibration(1L)).thenReturn(null);
        when(ensayoServicio.obtenerEnsayo(1L)).thenReturn(Optional.empty());

        Path tempFile = Files.createTempFile("calib-humidity", ".csv");
        Files.writeString(tempFile, "sensor,A,B,C,D\nsensor_1,0.5,0,0,0\n");

        CalibrationCorrection correction = new CalibrationCorrection();
        correction.setSensor(baseSensor);
        correction.setRutaArchivo(tempFile.toString());
        correction.setFechaSubida(LocalDateTime.now());

        when(calibrationCorrectionRepositorio.findAll()).thenReturn(List.of(correction));

        List<DatoEnsayoTemporal> datos = List.of(
                buildDato("sensor_1", 10.0),
                buildDato("sensor_1_hr", 50.0)
        );

        Method method = CargaDatosServicio.class.getDeclaredMethod("aplicarCorrecciones", List.class, Long.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<DatoEnsayoTemporal> result = (List<DatoEnsayoTemporal>) method.invoke(cargaDatosServicio, datos, 1L);

        assertEquals(10.5, result.get(0).getValor());
        assertEquals(50.5, result.get(1).getValor());
    }

    @Test
    void shouldRecalculateAnormalAfterCorrection() throws Exception {
        Sensor baseSensor = new Sensor();
        baseSensor.setId(1L);
        baseSensor.setCodigo("sensor_1");
        baseSensor.setActivo(true);

        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setNombre("Maquina 1");
        maquina.setTipo("Tipo");
        maquina.setLimiteInferior(20.0);
        maquina.setLimiteSuperior(40.0);

        Ensayo ensayo = new Ensayo();
        ensayo.setId(1L);
        ensayo.setNombre("Ensayo 1");
        ensayo.setMaquina(maquina);
        ensayo.setFechaInicio(LocalDateTime.now());

        when(sensorServicio.listarActivos()).thenReturn(List.of(baseSensor));
        when(calibrationManagementService.getActiveCalibration(1L)).thenReturn(null);
        when(ensayoServicio.obtenerEnsayo(1L)).thenReturn(Optional.of(ensayo));

        Path tempFile = Files.createTempFile("calib-anormal", ".csv");
        Files.writeString(tempFile, "sensor,A,B,C,D\n sensor_1,15,0,0,0\n");

        CalibrationCorrection correction = new CalibrationCorrection();
        correction.setSensor(baseSensor);
        correction.setRutaArchivo(tempFile.toString());
        correction.setFechaSubida(LocalDateTime.now());

        when(calibrationCorrectionRepositorio.findAll()).thenReturn(List.of(correction));

        List<DatoEnsayoTemporal> datos = List.of(buildDato("sensor_1", 30.0));

        Method method = CargaDatosServicio.class.getDeclaredMethod("aplicarCorrecciones", List.class, Long.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<DatoEnsayoTemporal> result = (List<DatoEnsayoTemporal>) method.invoke(cargaDatosServicio, datos, 1L);

        assertEquals(45.0, result.get(0).getValor());
        assertEquals(true, result.get(0).getAnormal());
    }

    @Test
    void shouldApplyCalibrationToLogTagExcelAndUseHumidityLimits() throws Exception {
        Sensor sensor = new Sensor();
        sensor.setId(1L);
        sensor.setCodigo("sensor_1");
        sensor.setActivo(true);

        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setNombre("Maquina 1");
        maquina.setTipo("Tipo");
        maquina.setLimiteInferior(0.0);
        maquina.setLimiteSuperior(100.0);
        maquina.setLimiteInferiorHumedad(0.0);
        maquina.setLimiteSuperiorHumedad(50.0);

        Ensayo ensayo = new Ensayo();
        ensayo.setId(1L);
        ensayo.setNombre("Ensayo 1");
        ensayo.setMaquina(maquina);
        ensayo.setFechaInicio(LocalDateTime.now());

        when(sensorServicio.listarActivos()).thenReturn(List.of(sensor));
        when(ensayoServicio.obtenerEnsayo(1L)).thenReturn(Optional.of(ensayo));

        Path tempFile = Files.createTempFile("calib-logtag", ".csv");
        Files.writeString(tempFile, "sensor,A,B,C,D\nsensor_1,0.5,0,0,0\n");

        CalibrationCorrection correction = new CalibrationCorrection();
        correction.setSensor(sensor);
        correction.setRutaArchivo(tempFile.toString());
        correction.setFechaSubida(LocalDateTime.now());
        when(calibrationCorrectionRepositorio.findAll()).thenReturn(List.of(correction));

        byte[] excelBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("LogTag");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha");
            header.createCell(1).setCellValue("Hora");
            header.createCell(2).setCellValue("Número de serie");
            header.createCell(3).setCellValue("Temperatura °C");
            header.createCell(4).setCellValue("%HR");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("2025-01-01");
            data.createCell(1).setCellValue("10:00");
            data.createCell(2).setCellValue("sensor_1");
            data.createCell(3).setCellValue("20");
            data.createCell(4).setCellValue("50");

            workbook.write(output);
            excelBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "archivo", "logtag.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        List<DatoEnsayoTemporal> result = cargaDatosServicio.cargarDatosExcel(file, 1L);

        assertEquals(2, result.size());
        assertEquals(20.5, result.get(0).getValor());
        assertEquals(50.5, result.get(1).getValor());
        assertEquals(Boolean.FALSE, result.get(0).getAnormal());
        assertEquals(Boolean.TRUE, result.get(1).getAnormal());
    }

    @Test
    void shouldParseLogTagSemicolonCsvWithSequenceAndCommaDecimals() throws Exception {
        Sensor sensor = new Sensor();
        sensor.setId(2L);
        sensor.setCodigo("sensor_2");
        sensor.setActivo(true);

        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setNombre("Maquina 1");
        maquina.setTipo("Tipo");
        maquina.setLimiteInferior(0.0);
        maquina.setLimiteSuperior(100.0);
        maquina.setLimiteInferiorHumedad(0.0);
        maquina.setLimiteSuperiorHumedad(100.0);

        Ensayo ensayo = new Ensayo();
        ensayo.setId(1L);
        ensayo.setNombre("Ensayo 1");
        ensayo.setMaquina(maquina);
        ensayo.setFechaInicio(LocalDateTime.now());

        when(ensayoServicio.obtenerEnsayo(1L)).thenReturn(Optional.of(ensayo));
        when(sensorServicio.listarActivos()).thenReturn(List.of(sensor));

        String csv = String.valueOf((char) 0xFEFF).repeat(2)
            + "1;2026-09-07;14:25:26;20,1;61,8;\r\n"
                + "2;2026-09-07;14:30:26;19,7;61,3;\r\n";
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "S2.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        List<DatoEnsayoTemporal> result = cargaDatosServicio.cargarDatosCSV(file, 1L);

        assertEquals(4, result.size());
        assertEquals("sensor_2", result.get(0).getSensor());
        assertEquals(20.1, result.get(0).getValor());
        assertEquals(1, result.get(0).getNumeroSecuencia());
        assertEquals(LocalDateTime.of(2026, 9, 7, 14, 25, 26), result.get(0).getTimestamp());
        assertEquals("sensor_2_hr", result.get(1).getSensor().toLowerCase());
        assertEquals(61.8, result.get(1).getValor());
        assertEquals(2, result.get(2).getNumeroSecuencia());
    }

    private DatoEnsayoTemporal buildDato(String sensor, double valor) {
        DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
        dato.setEnsayoId(1L);
        dato.setSensor(sensor);
        dato.setValor(valor);
        return dato;
    }
}
