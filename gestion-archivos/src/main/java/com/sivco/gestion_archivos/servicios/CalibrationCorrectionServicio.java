package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.CalibrationCorrection;
import com.sivco.gestion_archivos.modelos.Sensor;
import com.sivco.gestion_archivos.modelos.calibration.CalibrationSession;
import com.sivco.gestion_archivos.modelos.calibration.RegressionModelType;
import com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio;
import com.sivco.gestion_archivos.repositorios.SensorRepositorio;
import com.sivco.gestion_archivos.servicios.calibration.CalibrationManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling calibration uploads and management.
 * 
 * REFACTORED: This service now delegates to the new CalibrationManagementService
 * for regression-based calibration calculations while maintaining backward compatibility
 * with the legacy CalibrationCorrection model.
 * 
 * New workflow:
 * 1. Receives calibration file upload
 * 2. Delegates to CalibrationManagementService for processing
 * 3. Creates CalibrationSession with regression models
 * 4. Optionally creates CalibrationCorrection record for backward compatibility
 */
@Service
public class CalibrationCorrectionServicio {
    
    private static final Logger logger = LoggerFactory.getLogger(CalibrationCorrectionServicio.class);

    @Autowired
    private CalibrationCorrectionRepositorio repo;

    @Autowired
    private SensorRepositorio sensorRepositorio;
    
    @Autowired
    private CalibrationManagementService calibrationManagementService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${app.correcciones.upload-dir:correcciones}")
    private String uploadDir;

    /**
     * Uploads a new calibration file and processes it using the regression-based system.
     * The active regression model is automatically set to LINEAR by default.
     * 
     * DEPRECATED for new implementations: Use CalibrationManagementService directly
     * 
     * @param sensorId the sensor being calibrated
     * @param archivo the calibration file
     * @param descripcion optional description
     * @param subidoPor who is uploading
     * @return the legacy CalibrationCorrection record (for backward compatibility)
     */
    public CalibrationCorrection uploadCalibration(
            Long sensorId, 
            MultipartFile archivo, 
            String descripcion, 
            String subidoPor) throws IOException {
        
        Sensor sensor = sensorRepositorio.findById(sensorId)
                .orElseThrow(() -> new RuntimeException("Sensor no encontrado"));

        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("Archivo vacío");
        }

        logger.info("Starting calibration upload via legacy service for sensor {}", sensorId);
        
        // Process using the new regression-based system (no DB connection held after this returns)
        CalibrationSession newCalibration = calibrationManagementService.uploadAndProcessCalibration(
            sensorId,
            archivo,
            RegressionModelType.LINEAR,  // Default to linear model
            descripcion,
            subidoPor
        );

        // Store file on disk (IO only, no DB)
        Path dir = Paths.get(uploadDir, "sensores", "sensor_" + sensor.getId());
        Files.createDirectories(dir);
        String filename = java.util.UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
        Path target = dir.resolve(filename);
        Files.write(target, archivo.getBytes());

        // Persist legacy record in a separate, short transaction
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CalibrationCorrection savedLegacy = tx.execute(status -> {
            // Deactivate previous active calibrations
            repo.findBySensorIdAndActiveTrue(sensor.getId()).ifPresent(prev -> {
                prev.setActive(false);
                repo.save(prev);
            });

            CalibrationCorrection c = new CalibrationCorrection();
            c.setSensor(sensor);
            c.setNombreArchivo(archivo.getOriginalFilename());
            c.setTipoArchivo(detectType(archivo.getOriginalFilename()));
            c.setRutaArchivo(target.toString());
            c.setTamanioBytes(archivo.getSize());
            c.setFechaSubida(LocalDateTime.now());
            LocalDate calDate = LocalDate.now();
            c.setCalibrationDate(calDate);
            c.setValidUntil(calDate.plusDays(sensor.getFrecuenciaCalibracionDias()));
            c.setActive(true);
            c.setDescripcion(descripcion);
            c.setSubidoPor(subidoPor != null ? subidoPor : "Sistema");
            c.setCalibrationSessionId(newCalibration.getId());

            return repo.save(c);
        });
        
        logger.info("Created legacy CalibrationCorrection {} linked to CalibrationSession {}", 
                   savedLegacy.getId(), newCalibration.getId());
        return savedLegacy;
    }

    /**
     * Gets the active calibration for a sensor
     * 
     * DEPRECATED: Use CalibrationManagementService.getActiveCalibration() instead
     */
    public CalibrationCorrection getActiveBySensorId(Long sensorId) {
        return repo.findBySensorIdAndActiveTrue(sensorId).orElse(null);
    }

    /**
     * Gets the active calibration by sensor codigo
     * 
     * DEPRECATED: Use CalibrationManagementService.getActiveCalibration() instead
     */
    public CalibrationCorrection getActiveBySensorCodigo(String codigo) {
        return repo.findBySensorCodigoAndActiveTrue(codigo).orElse(null);
    }

    /**
     * Gets calibration history for a sensor
     * 
     * DEPRECATED: Use CalibrationManagementService.getCalibrationHistory() instead
     */
    public List<CalibrationCorrection> historyForSensor(Long sensorId) {
        return repo.findBySensorId(sensorId);
    }

    private String detectType(String nombreArchivo) {
        if (nombreArchivo == null) return "UNKNOWN";
        String ext = "";
        int idx = nombreArchivo.lastIndexOf('.');
        if (idx >= 0) ext = nombreArchivo.substring(idx + 1).toUpperCase();
        switch (ext) {
            case "CSV": return "CSV";
            case "XML": return "XML";
            case "PDF": return "PDF";
            case "XLS": case "XLSX": return "EXCEL";
            default: return "OTHER";
        }
    }
}
