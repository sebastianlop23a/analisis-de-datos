package com.sivco.gestion_archivos.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Legacy calibration correction model.
 * DEPRECATED: Use CalibrationSession and related models in calibration package instead.
 * 
 * This model is maintained for backward compatibility only.
 * New calibration implementations should use the regression-based system:
 * - CalibrationSession
 * - CalibrationPoint
 * - RegressionModel
 * 
 * The new system provides:
 * - Automatic regression coefficient calculation
 * - Support for multiple regression models (linear, quadratic, cubic)
 * - Proper device-scoped calibration lifecycle management
 * - Calibration expiration and drift tracking capabilities
 */
@Entity
@Table(name = "calibraciones_sensor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column(nullable = false)
    private String nombreArchivo;

    @Column(nullable = false)
    private String tipoArchivo;

    @Column(nullable = false)
    private String rutaArchivo;

    @Column(nullable = false)
    private Long tamanioBytes;

    @Column(nullable = false)
    private LocalDateTime fechaSubida;

    @Column(nullable = false)
    private LocalDate calibrationDate;

    private LocalDate validUntil;

    @Column(nullable = false)
    private Boolean active = true;

    private String descripcion;

    private String subidoPor;

    @Column(columnDefinition = "LONGTEXT")
    private String notas;
    
    /**
     * Reference to the new calibration session system
     * Links this legacy record to its replacement in the new system
     */
    @Column(name = "calibration_session_id")
    private Long calibrationSessionId;
}
