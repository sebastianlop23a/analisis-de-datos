package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.calibration.*;
import com.sivco.gestion_archivos.servicios.calibration.CalibrationManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for the regression-based calibration system.
 * 
 * Provides endpoints for:
 * - Uploading and processing calibration data
 * - Retrieving calibration history
 * - Managing active calibrations per device
 * - Querying regression model coefficients
 * - Applying corrections to measurements
 */
@RestController
@RequestMapping("/api/calibrations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CalibrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(CalibrationController.class);
    
    @Autowired
    private CalibrationManagementService calibrationManagementService;
    
    /**
     * Uploads a new calibration for a device.
     * Automatically archives the previous active calibration.
     * 
     * @param deviceId the device being calibrated
     * @param file the calibration data file (CSV format)
     * @param modelType the regression model to use (LINEAR, QUADRATIC, or CUBIC)
     * @param description optional description
     * @param uploadedBy who is uploading
     * @return the created calibration session
     */
    @PostMapping("/upload/{deviceId}")
        public ResponseEntity<?> uploadCalibration(
            @PathVariable Long deviceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        
        try {
            logger.info("Uploading calibration for device {}", deviceId);
            
            // The client no longer provides a requested model type. The service
            // will determine and store the models based on the uploaded file.
            CalibrationSession session = calibrationManagementService.uploadAndProcessCalibration(
                deviceId,
                file,
                null,
                description,
                uploadedBy
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("calibrationId", session.getId());
            response.put("deviceId", deviceId);
            response.put("activeModel", session.getActiveModelType());
            response.put("pointsCount", session.getCalibrationPoints().size());
            response.put("uploadedAt", session.getUploadedDate());
            
            logger.info("Calibration uploaded successfully: {}", session.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // return the original exception message so clients get precise feedback
            logger.warn("Bad request during calibration upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading calibration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload calibration: " + e.getMessage()));
        }
    }

    // Bulk upload endpoint removed: functionality deprecated by user requirement
    
    /**
     * Gets the active calibration for a device
     */
    @GetMapping("/active/{deviceId}")
    public ResponseEntity<?> getActiveCalibration(@PathVariable Long deviceId) {
        try {
            CalibrationSession session = calibrationManagementService.getActiveCalibration(deviceId);
            
            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active calibration found for device: " + deviceId));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("calibrationId", session.getId());
            response.put("deviceId", session.getDeviceId());
            response.put("calibrationDate", session.getCalibrationDate());
            response.put("uploadedDate", session.getUploadedDate());
            response.put("activeModel", session.getActiveModelType());
            response.put("pointsCount", session.getCalibrationPoints().size());
            response.put("expirationDate", session.getExpirationDate());
            response.put("description", session.getDescription());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving active calibration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve calibration: " + e.getMessage()));
        }
    }
    
    /**
     * Gets all calibrations (active and archived) for a device
     */
    @GetMapping("/history/{deviceId}")
    public ResponseEntity<?> getCalibrationHistory(@PathVariable Long deviceId) {
        try {
            List<CalibrationSession> history = calibrationManagementService.getCalibrationHistory(deviceId);
            
            List<Map<String, Object>> response = history.stream()
                .map(session -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("calibrationId", session.getId());
                    item.put("calibrationDate", session.getCalibrationDate());
                    item.put("uploadedDate", session.getUploadedDate());
                    item.put("isActive", session.getIsActive());
                    item.put("activeModel", session.getActiveModelType());
                    item.put("pointsCount", session.getCalibrationPoints().size());
                    item.put("description", session.getDescription());
                    return item;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving calibration history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve calibration history: " + e.getMessage()));
        }
    }
    
    /**
     * Gets details of a specific calibration session
     */
    @GetMapping("/{calibrationId}")
    public ResponseEntity<?> getCalibrationDetails(@PathVariable Long calibrationId) {
        try {
            // This would need to be added to CalibrationManagementService
            return ResponseEntity.ok(Map.of("calibrationId", calibrationId));
        } catch (Exception e) {
            logger.error("Error retrieving calibration details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve calibration details: " + e.getMessage()));
        }
    }
    
    /**
     * Sets the active regression model for a calibration
     */
    @PutMapping("/{calibrationId}/set-model")
    public ResponseEntity<?> setActiveModel(
            @PathVariable Long calibrationId,
            @RequestParam String modelType) {
        
        try {
            RegressionModelType model = RegressionModelType.valueOf(modelType.toUpperCase());
            calibrationManagementService.setActiveModelType(calibrationId, model);
            
            return ResponseEntity.ok(Map.of(
                "calibrationId", calibrationId,
                "activeModel", model,
                "message", "Active model set successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid model type: {}", modelType);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid model type: " + modelType));
        } catch (Exception e) {
            logger.error("Error setting active model", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to set active model: " + e.getMessage()));
        }
    }
    
    /**
     * Gets the coefficients of a specific regression model
     */
    @GetMapping("/{calibrationId}/model/{modelType}/coefficients")
    public ResponseEntity<?> getModelCoefficients(
            @PathVariable Long calibrationId,
            @PathVariable String modelType) {
        
        try {
            RegressionModelType model = RegressionModelType.valueOf(modelType.toUpperCase());
            RegressionModel regressionModel = calibrationManagementService.getRegressionModel(calibrationId, model);
            
            if (regressionModel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model not found for calibration: " + calibrationId));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("modelType", regressionModel.getModelType());
            response.put("formula", regressionModel.getModelType().getFormula());
            response.put("coefficients", Map.of(
                "c0", regressionModel.getCoefficient0(),
                "c1", regressionModel.getCoefficient1(),
                "c2", regressionModel.getCoefficient2(),
                "c3", regressionModel.getCoefficient3()
            ));
            response.put("rSquared", regressionModel.getRSquared());
            response.put("standardError", regressionModel.getStandardError());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid model type: " + modelType));
        } catch (Exception e) {
            logger.error("Error retrieving model coefficients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve coefficients: " + e.getMessage()));
        }
    }
    
    /**
     * Applies correction to a measurement
     */
    @PostMapping("/{deviceId}/apply-correction")
    public ResponseEntity<?> applyCorrection(
            @PathVariable Long deviceId,
            @RequestParam Double instrumentReading) {
        
        try {
            Double correctedValue = calibrationManagementService.applyCorrection(deviceId, instrumentReading);
            
            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", deviceId);
            response.put("instrumentReading", instrumentReading);
            response.put("correctedValue", correctedValue);
            response.put("correction", correctedValue - instrumentReading);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.error("No active calibration for device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error applying correction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to apply correction: " + e.getMessage()));
        }
    }
    
    /**
     * Checks if a device has an active calibration
     */
    @GetMapping("/{deviceId}/has-active")
    public ResponseEntity<?> hasActiveCalibration(@PathVariable Long deviceId) {
        try {
            boolean hasActive = calibrationManagementService.hasActiveCalibration(deviceId);
            
            return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "hasActiveCalibration", hasActive
            ));
            
        } catch (Exception e) {
            logger.error("Error checking active calibration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check active calibration: " + e.getMessage()));
        }
    }
}
