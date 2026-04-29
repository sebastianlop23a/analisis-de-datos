package com.sivco.gestion_archivos.modelos.calibration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single calibration point: a pair of patron_reading (standard instrument) 
 * and instrument_reading (device being calibrated).
 * 
 * The correction value is calculated as: correction = patron_reading - instrument_reading
 */
@Entity
@Table(name = "calibration_points", indexes = {
    @Index(name = "idx_calibration_id", columnList = "calibration_id"),
    @Index(name = "idx_device_point", columnList = "calibration_id,point_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationPoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_id", nullable = false)
    private CalibrationSession calibrationSession;
    
    /**
     * Order of this calibration point in the series
     */
    @Column(name = "point_order", nullable = false)
    private Integer pointOrder;
    
    /**
     * Standard/reference instrument reading (patron reading)
     */
    @Column(nullable = false)
    private Double patronReading;
    
    /**
     * Device being calibrated instrument reading
     */
    @Column(nullable = false)
    private Double instrumentReading;
    
    /**
     * Pre-calculated correction value: patron_reading - instrument_reading
     */
    @Column(nullable = false)
    private Double correction;
    
    /**
     * Optional metadata about the calibration point
     */
    private String metadata;
    
    /**
     * Calculates the correction value from the readings
     */
    @PrePersist
    @PreUpdate
    public void calculateCorrection() {
        if (patronReading != null && instrumentReading != null) {
            this.correction = patronReading - instrumentReading;
        }
    }

    // Explicit setters to ensure methods exist even if Lombok annotation processing is unavailable
    public void setCalibrationSession(CalibrationSession calibrationSession) {
        this.calibrationSession = calibrationSession;
    }

    public void setPointOrder(int pointOrder) {
        this.pointOrder = Integer.valueOf(pointOrder);
    }

    public void setPointOrder(Integer pointOrder) {
        this.pointOrder = pointOrder;
    }

    public void setPatronReading(double patronReading) {
        this.patronReading = Double.valueOf(patronReading);
    }

    public void setPatronReading(Double patronReading) {
        this.patronReading = patronReading;
    }

    public void setInstrumentReading(double instrumentReading) {
        this.instrumentReading = Double.valueOf(instrumentReading);
    }

    public void setInstrumentReading(Double instrumentReading) {
        this.instrumentReading = instrumentReading;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
