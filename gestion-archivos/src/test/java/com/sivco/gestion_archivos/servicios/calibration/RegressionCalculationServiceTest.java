package com.sivco.gestion_archivos.servicios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Regression Calculation Service Tests")
class RegressionCalculationServiceTest {
    
    private RegressionCalculationService regressionCalculationService;
    private CalibrationSession testSession;
    
    @BeforeEach
    void setup() {
        regressionCalculationService = new RegressionCalculationService();
        testSession = new CalibrationSession();
        testSession.setId(1L);
        testSession.setDeviceId(100L);
    }
    
    @Test
    @DisplayName("Calculate Linear Regression from Calibration Points")
    void testLinearRegressionCalculation() {
        // Create test calibration points: correction = 2x + 3
        List<CalibrationPoint> points = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double instrumentReading = 10.0 + i * 5;
            double correction = 2 * instrumentReading + 3;
            
            CalibrationPoint point = new CalibrationPoint();
            point.setCalibrationSession(testSession);
            point.setPointOrder(i);
            point.setInstrumentReading(instrumentReading);
            point.setCorrection(correction);
            point.setPatronReading(instrumentReading + correction);
            
            points.add(point);
        }
        
        // Calculate regression model
        Map<RegressionModelType, RegressionModel> models = 
            regressionCalculationService.calculateAllModels(testSession, points);
        
        assertTrue(models.containsKey(RegressionModelType.LINEAR));
        
        RegressionModel linearModel = models.get(RegressionModelType.LINEAR);
        assertNotNull(linearModel);
        assertEquals(RegressionModelType.LINEAR, linearModel.getModelType());
        
        // Coefficients should approximate m=2, b=3
        assertEquals(2.0, linearModel.getCoefficient0(), 0.01);  // slope
        assertEquals(3.0, linearModel.getCoefficient1(), 0.01);  // intercept
        
        // R-squared should be very high (close to 1) for perfect data
        assertTrue(linearModel.getRSquared() > 0.99);
    }
    
    @Test
    @DisplayName("Calculate Quadratic Regression")
    void testQuadraticRegressionCalculation() {
        // Create test calibration points: correction = x^2 + 2x + 1
        List<CalibrationPoint> points = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double x = 5.0 + i * 2;
            double correction = x * x + 2 * x + 1;
            
            CalibrationPoint point = new CalibrationPoint();
            point.setCalibrationSession(testSession);
            point.setPointOrder(i);
            point.setInstrumentReading(x);
            point.setCorrection(correction);
            point.setPatronReading(x + correction);
            
            points.add(point);
        }
        
        Map<RegressionModelType, RegressionModel> models = 
            regressionCalculationService.calculateAllModels(testSession, points);
        
        assertTrue(models.containsKey(RegressionModelType.QUADRATIC));
        
        RegressionModel quadModel = models.get(RegressionModelType.QUADRATIC);
        assertNotNull(quadModel);
        
        // Coefficients: a=1, b=2, c=1
        assertEquals(1.0, quadModel.getCoefficient2(), 0.01);   // x^2 coefficient
        assertEquals(2.0, quadModel.getCoefficient0(), 0.01);   // x coefficient
        assertEquals(1.0, quadModel.getCoefficient1(), 0.01);   // constant
        
        assertTrue(quadModel.getRSquared() > 0.99);
    }
    
    @Test
    @DisplayName("Apply Linear Model Correction")
    void testApplyLinearModelCorrection() {
        RegressionModel model = new RegressionModel();
        model.setModelType(RegressionModelType.LINEAR);
        model.setCoefficient0(1.5);  // slope
        model.setCoefficient1(2.0);  // intercept
        
        // y = 1.5*x + 2.0
        Double corrected = model.applyCorrectionModel(10.0);
        assertEquals(17.0, corrected, 0.01);
        
        corrected = model.applyCorrectionModel(0.0);
        assertEquals(2.0, corrected, 0.01);
    }
    
    @Test
    @DisplayName("Apply Quadratic Model Correction")
    void testApplyQuadraticModelCorrection() {
        RegressionModel model = new RegressionModel();
        model.setModelType(RegressionModelType.QUADRATIC);
        model.setCoefficient2(0.5);  // x^2 coefficient (a)
        model.setCoefficient0(1.0);  // x coefficient (b)
        model.setCoefficient1(2.0);  // constant (c)
        
        // y = 0.5*x^2 + 1.0*x + 2.0
        Double corrected = model.applyCorrectionModel(4.0);
        assertEquals(14.0, corrected, 0.01);  // 0.5*16 + 1*4 + 2 = 14
    }
    
    @Test
    @DisplayName("Apply Cubic Model Correction")
    void testApplyCubicModelCorrection() {
        RegressionModel model = new RegressionModel();
        model.setModelType(RegressionModelType.CUBIC);
        model.setCoefficient3(0.1);  // x^3 coefficient (a)
        model.setCoefficient2(0.2);  // x^2 coefficient (b)
        model.setCoefficient0(0.3);  // x coefficient (c)
        model.setCoefficient1(1.0);  // constant (d)
        
        // y = 0.1*x^3 + 0.2*x^2 + 0.3*x + 1.0
        Double corrected = model.applyCorrectionModel(2.0);
        // 0.1*8 + 0.2*4 + 0.3*2 + 1.0 = 0.8 + 0.8 + 0.6 + 1.0 = 3.2
        assertEquals(3.2, corrected, 0.01);
    }
    
    @Test
    @DisplayName("Insufficient Calibration Points for Linear")
    void testInsufficientPointsForLinear() {
        List<CalibrationPoint> points = new ArrayList<>();
        CalibrationPoint point = new CalibrationPoint();
        point.setInstrumentReading(10.0);
        point.setCorrection(20.0);
        points.add(point);
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> regressionCalculationService.calculateAllModels(testSession, points));
        assertEquals("Se requieren al menos 2 puntos de calibración para calcular un modelo de regresión", ex.getMessage());
    }
    
    @Test
    @DisplayName("Regression Model Coefficients Access")
    void testGetAllCoefficients() {
        RegressionModel model = new RegressionModel();
        model.setModelType(RegressionModelType.CUBIC);
        model.setCoefficient0(1.0);
        model.setCoefficient1(2.0);
        model.setCoefficient2(3.0);
        model.setCoefficient3(4.0);
        
        List<Double> coefficients = model.getAllCoefficients();
        assertEquals(4, coefficients.size());
        assertEquals(1.0, coefficients.get(0));
        assertEquals(2.0, coefficients.get(1));
        assertEquals(3.0, coefficients.get(2));
        assertEquals(4.0, coefficients.get(3));
    }
    
    @Test
    @DisplayName("Calibration Session Active Model")
    void testCalibrationSessionActiveModel() {
        CalibrationSession session = new CalibrationSession();
        session.setId(1L);
        session.setActiveModelType(RegressionModelType.LINEAR);
        
        RegressionModel linearModel = new RegressionModel();
        linearModel.setModelType(RegressionModelType.LINEAR);
        linearModel.setCoefficient0(1.5);
        linearModel.setCoefficient1(2.0);
        
        RegressionModel quadModel = new RegressionModel();
        quadModel.setModelType(RegressionModelType.QUADRATIC);
        
        session.setRegressionModels(Arrays.asList(linearModel, quadModel));
        
        RegressionModel activeModel = session.getActiveRegressionModel();
        assertNotNull(activeModel);
        assertEquals(RegressionModelType.LINEAR, activeModel.getModelType());
    }
    
    @Test
    @DisplayName("Calibration Session Apply Correction")
    void testCalibrationSessionApplyCorrection() {
        CalibrationSession session = new CalibrationSession();
        session.setId(1L);
        session.setActiveModelType(RegressionModelType.LINEAR);
        
        RegressionModel linearModel = new RegressionModel();
        linearModel.setModelType(RegressionModelType.LINEAR);
        linearModel.setCoefficient0(2.0);   // slope
        linearModel.setCoefficient1(5.0);   // intercept
        
        session.setRegressionModels(Collections.singletonList(linearModel));
        
        // y = 2*x + 5
        Double corrected = session.applyCorrection(10.0);
        assertEquals(25.0, corrected, 0.01);
    }
}
