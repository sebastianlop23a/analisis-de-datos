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

    private DatoEnsayoTemporal buildDato(String sensor, double valor) {
        DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
        dato.setEnsayoId(1L);
        dato.setSensor(sensor);
        dato.setValor(valor);
        return dato;
    }
}
