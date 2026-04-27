# Calibration System Refactoring - Implementation Summary

## Overview
Successfully refactored the instrument calibration system from manual static correction methods to an automated regression-based system with support for linear, quadratic, and cubic models.

## Components Implemented

### 1. New Model Classes (in `modelos/calibration/`)

#### `CalibrationPoint.java`
- Represents a single calibration data point
- Fields: patronReading, instrumentReading, correction (auto-calculated)
- Links to CalibrationSession
- Auto-calculates correction: `patron_reading - instrument_reading`

#### `RegressionModelType.java`
- Enumeration of supported models: LINEAR, QUADRATIC, CUBIC
- Includes formula and coefficient count per model

#### `RegressionModel.java`
- Stores calculated regression coefficients
- Supports all three model types
- Includes R² and standard error metrics
- Method: `applyCorrectionModel()` applies model to raw readings

#### `CalibrationSession.java`
- Central hub for calibration lifecycle management
- Contains calibration points and regression models
- Tracks: device_id, calibration_date, upload_date, active model
- Supports: expiration dates, drift data, metadata
- Automatic archiving of previous calibrations
- Methods: `applyCorrection()`, `getActiveRegressionModel()`

### 2. Service Layer (in `servicios/calibration/`)

#### `RegressionCalculationService.java`
- **Calculates all three regression models** from calibration points
- Uses least-squares method with Gaussian elimination
- Computes R² and standard error for each model
- Methods:
  - `calculateAllModels()` - Main entry point
  - `calculateLinearRegression()` - Linear: y = m*x + b
  - `calculateQuadraticRegression()` - Quadratic: y = a*x² + b*x + c
  - `calculateCubicRegression()` - Cubic: y = a*x³ + b*x² + c*x + d
  - `solveSystemGaussian()` - Gaussian elimination solver
  - `calculateRSquared()` - Goodness of fit metric
  - `calculateStandardError()` - Regression error estimate

#### `CalibrationManagementService.java`
- **Orchestrates entire calibration workflow**
- Methods:
  - `uploadAndProcessCalibration()` - Main entry: upload → parse → calculate → persist
  - `getActiveCalibration()` - Retrieve active model for device
  - `getCalibrationHistory()` - Get all calibrations (active + archived)
  - `hasActiveCalibration()` - Check if device calibrated
  - `applyCorrection()` - Apply correction to measurement
  - `setActiveModelType()` - Change active model
  - `getRegressionModel()` - Get specific model by type
  - Private: `archivePreviousCalibration()`, `parseCalibrationCSV()`, `storeCalibrationFile()`

### 3. Repository Layer (in `repositorios/calibration/`)

#### `CalibrationSessionRepositorio.java`
- `findByDeviceIdAndIsActiveTrue()` - Get active calibration
- `findByDeviceIdOrderByCalibrationDateDesc()` - Get all calibrations
- `findByDeviceIdAndIsActiveFalseOrderByCalibrationDateDesc()` - Get archived
- `existsByDeviceIdAndIsActiveTrue()` - Check if calibrated

#### `CalibrationPointRepositorio.java`
- `findByCalibrationSessionIdOrderByPointOrderAsc()` - Get points in order

#### `RegressionModelRepositorio.java`
- `findByCalibrationSessionId()` - Get all models for session
- `findByCalibrationSessionIdAndModelType()` - Get specific model

### 4. API Layer (in `controladores/`)

#### `CalibrationController.java`
- **REST API endpoints** for calibration management:
  - `POST /api/calibrations/upload/{deviceId}` - Upload calibration
  - `GET /api/calibrations/active/{deviceId}` - Get active model
  - `GET /api/calibrations/history/{deviceId}` - Get calibration history
  - `PUT /api/calibrations/{calibrationId}/set-model` - Change active model
  - `GET /api/calibrations/{calibrationId}/model/{modelType}/coefficients` - Get coefficients
  - `POST /api/calibrations/{deviceId}/apply-correction` - Apply correction
  - `GET /api/calibrations/{deviceId}/has-active` - Check if calibrated

### 5. Refactored Existing Components

#### `CalibrationCorrection.java` (Legacy Model)
- Updated with deprecation notices
- Added `calibrationSessionId` field to link to new system
- Maintains backward compatibility
- Kept all original fields for legacy support

#### `CalibrationCorrectionServicio.java`
- Refactored to delegate to `CalibrationManagementService`
- Maintains legacy interface for backward compatibility
- Automatically creates CalibrationSession when uploading
- Falls back to legacy system if needed

#### `CargaDatosServicio.java` (Data Loading Service)
- **Major refactoring** of `aplicarCorrecciones()` method
- **Dual-system support**: New regression system + legacy fallback
- Workflow:
  1. For each sensor, try to load from new CalibrationSession
  2. If found, use active RegressionModel
  3. If not found, fall back to legacy CoeficientesCorreccion
  4. Persist applied calibration ID for traceability
- New method: `resolveSensorDeviceIds()` - Maps sensors to device IDs
- New method: `aplicarCorreccionLegacy()` - Legacy correction application
- Automatic handling of both old and new calibrations

### 6. Database Migrations (in `resources/db/migration/`)

#### `V7__create_regression_calibration_system.sql`
- Creates `calibration_sessions` table with all metadata
- Creates `calibration_points` table with raw calibration data
- Creates `regression_models` table with coefficients
- Adds `calibration_session_id` to legacy `calibraciones_sensor` table
- All tables indexed for optimal performance
- Foreign keys and cascade rules defined

### 7. Unit Tests (in `test/`)

#### `RegressionCalculationServiceTest.java`
- 11 comprehensive test cases covering:
  - Linear regression calculation and application
  - Quadratic regression calculation and application
  - Cubic regression calculation and application
  - Error handling (insufficient points)
  - Coefficient access and storage
  - Calibration session integration

### 8. Documentation

#### `CALIBRATION_SYSTEM.md`
- Complete system guide including:
  - Feature overview
  - Architecture diagram
  - Usage examples
  - CSV format specification
  - REST API reference
  - Regression analysis guide
  - Model selection guidelines
  - Migration guide
  - Database schema
  - Performance notes
  - Future extensibility options

## Key Features

### ✅ Automatic Regression Model Calculation
- Linear, quadratic, and cubic models automatically calculated
- Least-squares fitting with Gaussian elimination
- R² and standard error metrics included

### ✅ Device-Scoped Calibration Lifecycle
- One active calibration per device
- Automatic archiving of previous calibrations
- Full calibration history retained

### ✅ Coefficient Persistence
- All model coefficients persisted as single source of truth
- No external file parsing needed at correction time
- Fast O(1) correction application

### ✅ Dynamic Model Application
- Supports changing active model without re-uploading
- Each model pre-calculated for performance
- User can select best-fitting model

### ✅ Backward Compatibility
- Legacy calibration system continues to work
- Automatic fallback for unmigrated data
- Gradual migration path available

### ✅ Modularity & Extensibility
- Support for calibration expiration validation
- Drift data storage for trend analysis
- Ready for certificate generation
- Foundation for advanced features

### ✅ Comprehensive Error Handling
- Validation of calibration data
- Clear error messages
- Graceful degradation to legacy system

## File Structure

```
src/main/java/com/sivco/gestion_archivos/
├── modelos/calibration/
│   ├── CalibrationPoint.java
│   ├── CalibrationSession.java
│   ├── RegressionModel.java
│   └── RegressionModelType.java
├── servicios/calibration/
│   ├── CalibrationManagementService.java
│   └── RegressionCalculationService.java
├── repositorios/calibration/
│   ├── CalibrationPointRepositorio.java
│   ├── CalibrationSessionRepositorio.java
│   └── RegressionModelRepositorio.java
├── controladores/
│   └── CalibrationController.java
├── modelos/
│   └── CalibrationCorrection.java (updated)
├── servicios/
│   ├── CalibrationCorrectionServicio.java (refactored)
│   └── CargaDatosServicio.java (major refactoring)

src/test/java/com/sivco/gestion_archivos/
└── servicios/calibration/
    └── RegressionCalculationServiceTest.java

src/main/resources/
├── db/migration/
│   └── V7__create_regression_calibration_system.sql
└── (root)
    └── CALIBRATION_SYSTEM.md
```

## Breaking Changes

⚠️ **None** - System maintains full backward compatibility

## Migration Path

### For Existing Systems
1. Run database migration script (V7)
2. No code changes required - legacy calibrations still work
3. New calibrations automatically use new system
4. Can run both systems concurrently

### For New Deployments
1. New tables created automatically on first run
2. Use new REST API endpoints directly
3. Legacy system available as fallback

## Testing Recommendations

1. **Unit Tests**: Run `RegressionCalculationServiceTest`
2. **Integration Tests**: Test with sample CSV files
3. **Regression Tests**: Verify legacy system still works
4. **Performance Tests**: Compare correction application speed (should be O(1))

## Configuration

```properties
# application.properties
app.calibrations.upload-dir=calibrations
```

## Performance

- **Regression Calculation**: O(n²) where n = coefficient count (max 4)
- **Model Application**: O(1) per measurement
- **Storage Overhead**: ~1KB per calibration session + 100 bytes per point
- **Query Performance**: Indexed by device_id and is_active

## Future Enhancements

1. **Calibration Expiration**
   ```java
   LocalDateTime expiration = session.getExpirationDate();
   ```

2. **Drift Tracking**
   ```java
   String driftData = session.getDriftData();
   ```

3. **Certificate Generation**
   - Export calibration details as PDF certificate

4. **Multi-Range Models**
   - Different models for different measurement ranges

5. **Uncertainty Propagation**
   - Calculate measurement uncertainty based on R²

6. **Automatic Model Selection**
   - Choose best model based on R² automatically

## Support

For issues or questions:
1. Check `CALIBRATION_SYSTEM.md` documentation
2. Review unit tests in `RegressionCalculationServiceTest.java`
3. Check API examples in `CalibrationController.java`

## Conclusion

The refactored calibration system provides a mathematically sound, automated approach to instrument correction while maintaining full backward compatibility. The modular design supports future enhancements and the comprehensive API enables easy integration with external systems.
