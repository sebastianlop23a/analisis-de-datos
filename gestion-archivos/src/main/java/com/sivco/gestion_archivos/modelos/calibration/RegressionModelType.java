package com.sivco.gestion_archivos.modelos.calibration;

/**
 * Enumeration of supported regression models for calibration correction
 */
public enum RegressionModelType {
    /**
     * Linear regression: y = m*x + b
     * where m is the slope and b is the intercept
     */
    LINEAR("Linear", "y = m*x + b", 2),
    
    /**
     * Quadratic regression: y = a*x^2 + b*x + c
     * second-degree polynomial
     */
    QUADRATIC("Quadratic", "y = a*x^2 + b*x + c", 3),
    
    /**
     * Cubic regression: y = a*x^3 + b*x^2 + c*x + d
     * third-degree polynomial
     */
    CUBIC("Cubic", "y = a*x^3 + b*x^2 + c*x + d", 4);
    
    private final String displayName;
    private final String formula;
    private final int coefficientCount;
    
    RegressionModelType(String displayName, String formula, int coefficientCount) {
        this.displayName = displayName;
        this.formula = formula;
        this.coefficientCount = coefficientCount;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFormula() {
        return formula;
    }
    
    public int getCoefficientCount() {
        return coefficientCount;
    }
}
