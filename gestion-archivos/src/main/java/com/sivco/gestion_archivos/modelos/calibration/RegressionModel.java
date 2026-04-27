package com.sivco.gestion_archivos.modelos.calibration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the calculated regression model coefficients for a specific model type.
 * The coefficients persist the regression calculation results and become the
 * single source of truth for instrument correction.
 */
@Entity
@Table(name = "regression_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegressionModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_id", nullable = false)
    private CalibrationSession calibrationSession;

    /**
     * Optional sensor code this model applies to when using a global calibration upload.
     * If null, model is assumed to apply to the session's deviceId.
     */
    @Column(name = "sensor_code")
    private String sensorCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    private RegressionModelType modelType;
    
    /**
     * Coefficient for linear term (m or b depending on model)
     * Linear: m (slope)
     * Quadratic: b
     * Cubic: c
     */
    @Column(name = "coefficient0", nullable = false)
    private Double coefficient0;
    
    /**
     * Coefficient for x term or first power term
     * Linear: b (intercept)
     * Quadratic: c
     * Cubic: d
     */
    @Column(name = "coefficient1", nullable = false)
    private Double coefficient1;
    
    /**
     * Coefficient for x^2 term (quadratic and cubic only)
     * Quadratic: a
     * Cubic: b
     */
    @Column(name = "coefficient2")
    private Double coefficient2;
    
    /**
     * Coefficient for x^3 term (cubic only)
     * Cubic: a
     */
    @Column(name = "coefficient3")
    private Double coefficient3;
    
    /**
     * Goodness of fit metric (R-squared value between 0 and 1)
     */
    @Column(name = "r_squared", nullable = false)
    private Double rSquared;
    
    /**
     * Standard error of the regression estimate
     */
    @Column(name = "standard_error")
    private Double standardError;
    
    /**
     * Applies the regression model to a given instrument reading to get the corrected value
     * 
     * @param instrumentReading the raw instrument reading (x value)
     * @return the corrected value based on the regression model
     */
    public Double applyCorrectionModel(Double instrumentReading) {
        if (instrumentReading == null) {
            return null;
        }
        
        double x = instrumentReading;
        double result = 0.0;
        
        switch (modelType) {
            case LINEAR:
                // y = m*x + b
                result = coefficient0 * x + coefficient1;
                break;
                
            case QUADRATIC:
                // y = a*x^2 + b*x + c
                result = coefficient2 * x * x + coefficient0 * x + coefficient1;
                break;
                
            case CUBIC:
                // y = a*x^3 + b*x^2 + c*x + d
                result = coefficient3 * x * x * x + 
                         coefficient2 * x * x + 
                         coefficient0 * x + 
                         coefficient1;
                break;
        }
        
        return result;
    }
    
    /**
     * Gets all coefficients as a list in order of polynomial degree
     */
    public List<Double> getAllCoefficients() {
        List<Double> coefficients = new ArrayList<>();
        coefficients.add(coefficient0);
        coefficients.add(coefficient1);
        if (coefficient2 != null) {
            coefficients.add(coefficient2);
        }
        if (coefficient3 != null) {
            coefficients.add(coefficient3);
        }
        return coefficients;
    }
}
