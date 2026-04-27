# Calibration System - Usage Examples

## 1. Uploading a Calibration via REST API

### Example 1: Basic Upload (CSV file with patron and instrument readings)

**Request:**
```bash
curl -X POST "http://localhost:8080/api/calibrations/upload/1" \
  -F "file=@calibration_data.csv" \
  -F "modelType=LINEAR" \
  -F "description=Temperature sensor PT100 calibration" \
  -F "uploadedBy=john.doe@example.com"
```

**CSV File Content (calibration_data.csv):**
```csv
patron_reading,instrument_reading
20.0,19.8
25.0,24.7
30.0,29.5
35.0,34.2
40.0,39.1
```

**Response:**
```json
{
  "success": true,
  "calibrationId": 42,
  "deviceId": 1,
  "activeModel": "LINEAR",
  "pointsCount": 5,
  "uploadedAt": "2024-02-21T10:30:00"
}
```

### What Happens Behind the Scenes:
1. Parses 5 calibration points
2. Calculates correction for each point: `correction = 20.0 - 19.8 = 0.2`, etc.
3. Computes LINEAR regression: `correction = m*reading + b`
4. Calculates QUADRATIC model (if enough points)
5. Calculates CUBIC model (if enough points)
6. Persists all data to database
7. Sets LINEAR as active model
8. Archives any previous calibration for device 1

---

## 2. Getting Active Calibration

**Request:**
```bash
curl http://localhost:8080/api/calibrations/active/1
```

**Response:**
```json
{
  "calibrationId": 42,
  "deviceId": 1,
  "calibrationDate": "2024-02-21T10:30:00",
  "uploadedDate": "2024-02-21T10:30:00",
  "activeModel": "LINEAR",
  "pointsCount": 5,
  "expirationDate": null,
  "description": "Temperature sensor PT100 calibration"
}
```

---

## 3. Getting Regression Model Coefficients

**Request:**
```bash
curl http://localhost:8080/api/calibrations/42/model/LINEAR/coefficients
```

**Response:**
```json
{
  "modelType": "LINEAR",
  "formula": "y = m*x + b",
  "coefficients": {
    "c0": 0.95,
    "c1": 0.08,
    "c2": null,
    "c3": null
  },
  "rSquared": 0.9998,
  "standardError": 0.012
}
```

**Interpretation:**
- Model: `correction = 0.95*reading + 0.08`
- R² = 0.9998 means 99.98% variance explained (excellent fit)
- Standard error = 0.012°C average deviation

---

## 4. Applying Correction to a Measurement

**Request (with instrument reading of 25.5):**
```bash
curl http://localhost:8080/api/calibrations/1/apply-correction \
  -X POST \
  -G --data-urlencode "instrumentReading=25.5"
```

**Response:**
```json
{
  "deviceId": 1,
  "instrumentReading": 25.5,
  "correctedValue": 24.36,
  "correction": -1.14
}
```

**Calculation:**
- Using LINEAR model: `corrected = 0.95 * 25.5 + 0.08 = 24.36`
- Instrument read 25.5, but corrected reading is 24.36
- This means instrument reads 1.14 higher than actual

---

## 5. Switching to a Different Model

### Get All Models First
**Request:**
```bash
curl http://localhost:8080/api/calibrations/42/model/QUADRATIC/coefficients
```

**Response:**
```json
{
  "modelType": "QUADRATIC",
  "formula": "y = a*x^2 + b*x + c",
  "coefficients": {
    "c0": 0.001,
    "c1": 0.94,
    "c2": 0.00002,
    "c3": null
  },
  "rSquared": 0.9999,
  "standardError": 0.008
}
```

### Switch to Quadratic Model
**Request:**
```bash
curl -X PUT "http://localhost:8080/api/calibrations/42/set-model" \
  -G --data-urlencode "modelType=QUADRATIC"
```

**Response:**
```json
{
  "calibrationId": 42,
  "activeModel": "QUADRATIC",
  "message": "Active model set successfully"
}
```

### Now Apply Correction with New Model
**Request:**
```bash
curl http://localhost:8080/api/calibrations/1/apply-correction \
  -X POST \
  -G --data-urlencode "instrumentReading=25.5"
```

**Response:**
```json
{
  "deviceId": 1,
  "instrumentReading": 25.5,
  "correctedValue": 24.39,
  "correction": -1.11
}
```

**Calculation:**
- QUADRATIC: `corrected = 0.00002*25.5² + 0.94*25.5 + 0.001 = 24.39`
- More accurate than linear (24.36 vs 24.39)

---

## 6. Checking Calibration History

**Request:**
```bash
curl http://localhost:8080/api/calibrations/history/1
```

**Response:**
```json
[
  {
    "calibrationId": 42,
    "calibrationDate": "2024-02-21T10:30:00",
    "uploadedDate": "2024-02-21T10:30:00",
    "isActive": true,
    "activeModel": "QUADRATIC",
    "pointsCount": 5,
    "description": "Temperature sensor PT100 calibration"
  },
  {
    "calibrationId": 41,
    "calibrationDate": "2024-02-14T09:15:00",
    "uploadedDate": "2024-02-14T09:15:00",
    "isActive": false,
    "activeModel": "LINEAR",
    "pointsCount": 5,
    "description": "Previous calibration (archived)"
  }
]
```

---

## 7. Programmatic Usage in Java Service

```java
@Service
public class MeasurementService {
    
    @Autowired
    private CalibrationManagementService calibrationService;
    
    // Process a measurement through calibration
    public Double processMeasurement(Long deviceId, Double rawReading) {
        try {
            // Apply active calibration for the device
            Double correctedReading = calibrationService.applyCorrection(
                deviceId, 
                rawReading
            );
            
            logger.info("Measurement correction: {} -> {}", 
                rawReading, correctedReading);
            
            return correctedReading;
            
        } catch (IllegalStateException e) {
            logger.error("No active calibration for device {}", deviceId);
            return rawReading;  // Return raw if no calibration
        }
    }
    
    // Check if device is calibrated before processing
    public boolean isDeviceCalibrated(Long deviceId) {
        return calibrationService.hasActiveCalibration(deviceId);
    }
    
    // Get model details for reporting
    public Map<String, Object> getCalibrationDetails(Long deviceId) {
        CalibrationSession session = 
            calibrationService.getActiveCalibration(deviceId);
            
        if (session == null) {
            return Map.of("calibrated", false);
        }
        
        RegressionModel activeModel = session.getActiveRegressionModel();
        
        return Map.of(
            "calibrated", true,
            "modelType", activeModel.getModelType().toString(),
            "formula", activeModel.getModelType().getFormula(),
            "rSquared", activeModel.getRSquared(),
            "pointsCount", session.getCalibrationPoints().size()
        );
    }
}
```

---

## 8. CSV File Examples

### Minimal Format
```csv
patron_reading,instrument_reading
100,99.5
101,100.4
102,101.3
103,102.2
```

### With Metadata
```csv
patron_reading,instrument_reading,metadata
100,99.5,room_temp_20C
101,100.4,room_temp_21C
102,101.3,room_temp_22C
103,102.2,room_temp_23C
```

### With Header Row (auto-detected as header)
```csv
Standard Reading,Device Reading
100,99.5
101,100.4
102,101.3
103,102.2
```

---

## 9. Error Scenarios

### Missing Active Calibration

**Request:**
```bash
curl http://localhost:8080/api/calibrations/active/999
```

**Response (404):**
```json
{
  "error": "No active calibration found for device: 999"
}
```

### Applying Correction Without Calibration

**Request:**
```bash
curl http://localhost:8080/api/calibrations/999/apply-correction \
  -X POST \
  -G --data-urlencode "instrumentReading=25.5"
```

**Response (404):**
```json
{
  "error": "No active calibration found for device: 999"
}
```

### Invalid Model Type

**Request:**
```bash
curl -X POST "http://localhost:8080/api/calibrations/upload/1" \
  -F "file=@calibration_data.csv" \
  -F "modelType=INVALID"
```

**Response (400):**
```json
{
  "error": "Invalid model type. Must be LINEAR, QUADRATIC, or CUBIC"
}
```

---

## 10. Advanced: Comparing Models

### Upload with Multiple Models Calculated

When you upload calibration data with enough points, all applicable models are calculated:

- **2-5 points**: LINEAR model calculated
- **3-5 points**: LINEAR + QUADRATIC models calculated  
- **4+ points**: LINEAR + QUADRATIC + CUBIC models calculated

### Get All Three Models

```bash
# Linear
curl http://localhost:8080/api/calibrations/42/model/LINEAR/coefficients

# Quadratic
curl http://localhost:8080/api/calibrations/42/model/QUADRATIC/coefficients

# Cubic
curl http://localhost:8080/api/calibrations/42/model/CUBIC/coefficients
```

### Compare R² Values

```json
// LINEAR R² = 0.9998
// QUADRATIC R² = 0.9999
// CUBIC R² = 0.9999

// Quadratic provides minimal improvement over Linear
// Choose LINEAR for simplicity (Occam's Razor)
```

---

## 11. Batch Processing Measurements

```java
@Service
public class BatchMeasurementProcessor {
    
    @Autowired
    private CalibrationManagementService calibrationService;
    
    public List<MeasurementResult> processBatch(
            Long deviceId, 
            List<Double> rawReadings) {
        
        // Get active calibration once
        CalibrationSession calibration = 
            calibrationService.getActiveCalibration(deviceId);
            
        if (calibration == null) {
            throw new IllegalStateException(
                "No active calibration for device: " + deviceId);
        }
        
        // Apply correction to all readings (efficient batch processing)
        return rawReadings.stream()
            .map(reading -> {
                Double corrected = calibration.applyCorrection(reading);
                return new MeasurementResult(reading, corrected);
            })
            .collect(Collectors.toList());
    }
}
```

---

## 12. Database Queries

### Find Devices Without Active Calibration

```sql
SELECT DISTINCT d.id, d.codigo, d.ubicacion
FROM sensores d
LEFT JOIN calibration_sessions c ON d.id = c.device_id AND c.is_active = TRUE
WHERE c.id IS NULL;
```

### Get R² Statistics for All Calibrations

```sql
SELECT 
    cs.id,
    cs.device_id,
    rm.model_type,
    rm.r_squared,
    COUNT(cp.id) as point_count
FROM calibration_sessions cs
JOIN regression_models rm ON cs.id = rm.calibration_id
JOIN calibration_points cp ON cs.id = cp.calibration_id
WHERE cs.is_active = TRUE
GROUP BY cs.id, rm.model_type;
```

---

## Summary

The regression-based calibration system provides:

✅ **Automatic Model Calculation** - Choose best fit from LINEAR/QUADRATIC/CUBIC
✅ **Easy Integration** - Simple REST API and Java methods
✅ **High Precision** - Mathematical least-squares fitting
✅ **Transparency** - R² and standard error metrics
✅ **Flexibility** - Switch models anytime without re-uploading
✅ **Reliability** - Full error handling and fallback mechanisms
✅ **Performance** - O(1) correction application per measurement
