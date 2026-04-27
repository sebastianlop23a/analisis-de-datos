package com.sivco.gestion_archivos.modelos.calibration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete calibration session for a device.
 * Contains calibration points, calculated regression models, and metadata.
 * 
 * This entity replaces the old CalibrationCorrection and serves as the
 * central hub for all calibration-related data.
 */
@Entity
@Table(name = "calibration_sessions",
    indexes = {
        @Index(name = "idx_device_id", columnList = "device_id"),
        @Index(name = "idx_device_active", columnList = "device_id,is_active"),
        @Index(name = "idx_calibration_date", columnList = "calibration_date")
    },
    uniqueConstraints = {
        // Restricción única por combinación (device_id, uploaded_year)
        @UniqueConstraint(name = "ux_calibration_device_year", columnNames = {"device_id", "uploaded_year"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The device/sensor being calibrated (identified by device_id)
     * References the Sensor entity
     */
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    /**
     * Date when this calibration was performed
     */
    @Column(name = "calibration_date", nullable = false)
    private LocalDateTime calibrationDate;
    
    /**
     * Date when this calibration was uploaded to the system
     */
    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;

    /**
     * Año en que se subió la calibración. Columna generada en la base de datos
     * (ej: `YEAR(uploaded_date)`) para permitir índices/constraints por año.
     * Mapeada como no insertable/actualizable desde JPA.
     */
    @Column(name = "uploaded_year", insertable = false, updatable = false)
    private Integer uploadedYear;
    
    /**
     * Whether this is the currently active calibration for the device
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * The model type selected as the active correction model
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "active_model_type")
    private RegressionModelType activeModelType;
    
    /**
     * Name/description of this calibration session
     */
    private String name;
    
    /**
     * Detailed description of the calibration
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * Name of the person who uploaded this calibration
     */
    private String uploadedBy;
    
    /**
     * Original calibration file name
     */
    private String sourceFileName;
    
    /**
     * Path to the stored calibration file
     */
    private String filePath;
    
    /**
     * File size in bytes
     */
    private Long fileSizeBytes;
    
    /**
     * Calibration points collected for this session
     */
    @OneToMany(mappedBy = "calibrationSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CalibrationPoint> calibrationPoints = new ArrayList<>();
    
    /**
     * Calculated regression models
     */
    @OneToMany(mappedBy = "calibrationSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RegressionModel> regressionModels = new ArrayList<>();
    
    /**
     * Optional drift data for future drift-based recalibration intervals
     */
    @Column(length = 2000)
    private String driftData;
    
    /**
     * Metadata including additional calibration details
     */
    @Column(columnDefinition = "LONGTEXT")
    private String metadata;
    
    /**
     * When this calibration becomes invalid/expires
     */
    private LocalDateTime expirationDate;
    
    /**
     * Gets the active regression model for applying corrections
     */
    @Transient
    public RegressionModel getActiveRegressionModel() {
        if (activeModelType == null || regressionModels == null) {
            return null;
        }
        
        return regressionModels.stream()
            .filter(m -> m.getModelType() == activeModelType)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Applies the correction using the active regression model.
     * The regression model stores the correction offset, so the final corrected
     * measurement is the raw instrument reading plus that offset.
     * 
     * @param instrumentReading the raw measurement from the instrument
     * @return the corrected measurement value
     */
    @Transient
    public Double applyCorrection(Double instrumentReading) {
        RegressionModel activeModel = getActiveRegressionModel();
        if (activeModel == null) {
            throw new IllegalStateException("No active regression model configured for this calibration session");
        }
        return instrumentReading + activeModel.applyCorrectionModel(instrumentReading);
    }
}
