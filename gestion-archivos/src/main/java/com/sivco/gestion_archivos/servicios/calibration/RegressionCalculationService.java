package com.sivco.gestion_archivos.servicios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Computes regression models from calibration points using the least squares method.
 * 
 * This service calculates linear, quadratic, and cubic regression models
 * from the calibration points (instrument_reading -> correction mappings).
 */
@Service
public class RegressionCalculationService {
    
    /**
     * Calculates all three regression models (linear, quadratic, cubic)
     * from a set of calibration points
     * 
     * @param calibrationPoints list of calibration points with instrument readings and corrections
     * @return a map of model types to their calculated regression models
     * @throws IllegalArgumentException if insufficient calibration points are provided
     */
    public Map<RegressionModelType, RegressionModel> calculateAllModels(
            CalibrationSession session,
            List<CalibrationPoint> calibrationPoints) {
        
        if (calibrationPoints == null || calibrationPoints.size() < 2) {
            // message shown to client when upload fails with too few points
            throw new IllegalArgumentException("Se requieren al menos 2 puntos de calibración para calcular un modelo de regresión");
        }
        
        Map<RegressionModelType, RegressionModel> models = new LinkedHashMap<>();
        
        // Extract x (instrument readings) and y (corrections) values
        double[] x = new double[calibrationPoints.size()];
        double[] y = new double[calibrationPoints.size()];
        
        for (int i = 0; i < calibrationPoints.size(); i++) {
            CalibrationPoint point = calibrationPoints.get(i);
            x[i] = point.getInstrumentReading();
            y[i] = point.getCorrection();
        }
        
        // Calculate linear regression
        if (calibrationPoints.size() >= 2) {
            RegressionModel linearModel = calculateLinearRegression(session, x, y);
            models.put(RegressionModelType.LINEAR, linearModel);
        }
        
        // Calculate quadratic regression
        if (calibrationPoints.size() >= 3) {
            RegressionModel quadraticModel = calculateQuadraticRegression(session, x, y);
            models.put(RegressionModelType.QUADRATIC, quadraticModel);
        }
        
        // Calculate cubic regression
        if (calibrationPoints.size() >= 4) {
            RegressionModel cubicModel = calculateCubicRegression(session, x, y);
            models.put(RegressionModelType.CUBIC, cubicModel);
        }
        
        return models;
    }
    
    /**
     * Calculates linear regression: y = m*x + b
     * Using least squares method
     */
    private RegressionModel calculateLinearRegression(CalibrationSession session, double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            throw new IllegalArgumentException("Cannot calculate linear regression: insufficient variation in x values");
        }
        
        double m = (n * sumXY - sumX * sumY) / denominator;
        double b = (sumY - m * sumX) / n;
        
        RegressionModel model = new RegressionModel();
        model.setCalibrationSession(session);
        model.setModelType(RegressionModelType.LINEAR);
        model.setCoefficient0(m);      // slope
        model.setCoefficient1(b);      // intercept
        model.setRSquared(calculateRSquared(x, y, i -> m * x[i] + b));
        model.setStandardError(calculateStandardError(x, y, i -> m * x[i] + b));
        
        return model;
    }
    
    /**
     * Calculates quadratic regression: y = a*x^2 + b*x + c
     * Using least squares method with matrix solution
     */
    private RegressionModel calculateQuadraticRegression(CalibrationSession session, double[] x, double[] y) {
        int n = x.length;
        
        // Build Vandermonde matrix
        double[][] A = new double[3][3];
        double[] b = new double[3];
        
        for (int i = 0; i < n; i++) {
            double xi = x[i];
            double xi2 = xi * xi;
            double xi3 = xi2 * xi;
            double xi4 = xi3 * xi;
            
            A[0][0] += xi4;
            A[0][1] += xi3;
            A[0][2] += xi2;
            A[1][1] += xi2;
            A[1][2] += xi;
            A[2][2] += 1;
            
            b[0] += y[i] * xi2;
            b[1] += y[i] * xi;
            b[2] += y[i];
        }
        
        A[1][0] = A[0][1];
        A[2][0] = A[0][2];
        A[2][1] = A[1][2];
        
        // Solve system using Gaussian elimination
        double[] coeffs = solveSystemGaussian(A, b);
        
        RegressionModel model = new RegressionModel();
        model.setCalibrationSession(session);
        model.setModelType(RegressionModelType.QUADRATIC);
        model.setCoefficient2(coeffs[0]); // a (x^2)
        model.setCoefficient0(coeffs[1]); // b (x)
        model.setCoefficient1(coeffs[2]); // c (constant)
        model.setRSquared(calculateRSquared(x, y, i -> coeffs[0] * x[i] * x[i] + coeffs[1] * x[i] + coeffs[2]));
        model.setStandardError(calculateStandardError(x, y, i -> coeffs[0] * x[i] * x[i] + coeffs[1] * x[i] + coeffs[2]));
        
        return model;
    }
    
    /**
     * Calculates cubic regression: y = a*x^3 + b*x^2 + c*x + d
     * Using least squares method with matrix solution
     */
    private RegressionModel calculateCubicRegression(CalibrationSession session, double[] x, double[] y) {
        int n = x.length;
        
        // Build Vandermonde matrix for cubic
        double[][] A = new double[4][4];
        double[] b = new double[4];
        
        for (int i = 0; i < n; i++) {
            double xi = x[i];
            double xi2 = xi * xi;
            double xi3 = xi2 * xi;
            double xi4 = xi3 * xi;
            double xi5 = xi4 * xi;
            double xi6 = xi5 * xi;
            
            A[0][0] += xi6;
            A[0][1] += xi5;
            A[0][2] += xi4;
            A[0][3] += xi3;
            A[1][1] += xi4;
            A[1][2] += xi3;
            A[1][3] += xi2;
            A[2][2] += xi2;
            A[2][3] += xi;
            A[3][3] += 1;
            
            b[0] += y[i] * xi3;
            b[1] += y[i] * xi2;
            b[2] += y[i] * xi;
            b[3] += y[i];
        }
        
        A[1][0] = A[0][1];
        A[2][0] = A[0][2];
        A[2][1] = A[1][2];
        A[3][0] = A[0][3];
        A[3][1] = A[1][3];
        A[3][2] = A[2][3];
        
        // Solve system using Gaussian elimination
        double[] coeffs = solveSystemGaussian(A, b);
        
        RegressionModel model = new RegressionModel();
        model.setCalibrationSession(session);
        model.setModelType(RegressionModelType.CUBIC);
        model.setCoefficient3(coeffs[0]); // a (x^3)
        model.setCoefficient2(coeffs[1]); // b (x^2)
        model.setCoefficient0(coeffs[2]); // c (x)
        model.setCoefficient1(coeffs[3]); // d (constant)
        model.setRSquared(calculateRSquared(x, y, i -> coeffs[0] * x[i] * x[i] * x[i] + coeffs[1] * x[i] * x[i] + coeffs[2] * x[i] + coeffs[3]));
        model.setStandardError(calculateStandardError(x, y, i -> coeffs[0] * x[i] * x[i] * x[i] + coeffs[1] * x[i] * x[i] + coeffs[2] * x[i] + coeffs[3]));
        
        return model;
    }
    
    /**
     * Solves a linear system Ax = b using Gaussian elimination with partial pivoting
     */
    private double[] solveSystemGaussian(double[][] A, double[] b) {
        int n = A.length;
        
        // Create augmented matrix
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                aug[i][j] = A[i][j];
            }
            aug[i][n] = b[i];
        }
        
        // Forward elimination with partial pivoting
        for (int col = 0; col < n; col++) {
            // Find pivot
            int pivotRow = col;
            double maxVal = Math.abs(aug[col][col]);
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(aug[row][col]) > maxVal) {
                    maxVal = Math.abs(aug[row][col]);
                    pivotRow = row;
                }
            }
            
            // Swap rows
            double[] temp = aug[col];
            aug[col] = aug[pivotRow];
            aug[pivotRow] = temp;
            
            // Check for singular matrix
            if (Math.abs(aug[col][col]) < 1e-10) {
                throw new IllegalArgumentException("Matrix is singular and cannot be solved");
            }
            
            // Eliminate column
            for (int row = col + 1; row < n; row++) {
                double factor = aug[row][col] / aug[col][col];
                for (int j = col; j <= n; j++) {
                    aug[row][j] -= factor * aug[col][j];
                }
            }
        }
        
        // Back substitution
        double[] solution = new double[n];
        for (int row = n - 1; row >= 0; row--) {
            solution[row] = aug[row][n];
            for (int col = row + 1; col < n; col++) {
                solution[row] -= aug[row][col] * solution[col];
            }
            solution[row] /= aug[row][row];
        }
        
        return solution;
    }
    
    /**
     * Calculates R-squared (coefficient of determination)
     * Measures goodness of fit between 0 and 1
     */
    private Double calculateRSquared(double[] x, double[] y, java.util.function.IntFunction<Double> predictedValueFn) {
        double meanY = Arrays.stream(y).average().orElse(0);
        double ssTotal = 0;
        double ssResidual = 0;
        
        for (int i = 0; i < x.length; i++) {
            double predicted = predictedValueFn.apply(i);
            double residual = y[i] - predicted;
            ssResidual += residual * residual;
            ssTotal += (y[i] - meanY) * (y[i] - meanY);
        }
        
        if (ssTotal == 0) {
            return 1.0;
        }
        
        return 1.0 - (ssResidual / ssTotal);
    }
    
    /**
     * Calculates standard error of regression
     */
    private Double calculateStandardError(double[] x, double[] y, java.util.function.IntFunction<Double> predictedValueFn) {
        double sumSquaredErrors = 0;
        
        for (int i = 0; i < x.length; i++) {
            double predicted = predictedValueFn.apply(i);
            double error = y[i] - predicted;
            sumSquaredErrors += error * error;
        }
        
        int degreesOfFreedom = Math.max(x.length - 2, 1);
        double meanSquaredError = sumSquaredErrors / degreesOfFreedom;
        
        return Math.sqrt(meanSquaredError);
    }
}
