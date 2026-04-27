# Regression-Based Calibration System

## Overview

The refactored calibration system is based on automatic regression modeling from calibration points. This replaces the manual static correction methods with a robust, mathematically-sound approach.

## Key Features

### 1. Automatic Regression Model Calculation
The system automatically calculates three types of regression models from uploaded calibration data:

#### Linear: `y = m*x + b`
- Uses least-squares method
- Requires minimum 2 calibration points
- Best for linear instrument behavior

#### Quadratic: `y = a*x² + b*x + c`
- Second-degree polynomial fit
- Requires minimum 3 calibration points
- Suitable for slightly nonlinear instruments

#### Cubic: `y = a*x³ + b*x² + c*x + d`
- Third-degree polynomial fit
- Requires minimum 4 calibration points
- Handles complex nonlinear corrections

### 2. Device-Scoped Calibration Lifecycle

Each device maintains a single active calibration at any time:

```
┌─────────────────────┐
│  Upload New Cal.    │
│  ↓                  │
│  Archive Previous   │ (automatic)
│  ↓                  │
│  Activate New       │ (becomes active)
│  ↓                  │
│  Apply to Measurements
```

Features:
- **Automatic Archiving**: Previous calibration automatically marked as inactive
- **Single Active Per Device**: Ensures consistent measurement correction
- **Persistent History**: All calibrations retained for audit trail
- **Expiration Support**: Optional calibration expiration dates

### 3. Calibration Data Structure

#### Calibration Points
Each calibration point records:
- `patronReading`: Reference instrument reading (standard)
- `instrumentReading`: Device reading (to be calibrated)
- `correction`: Automatically calculated as `patron_reading - instrument_reading`

#### Regression Models
Each model stores:
- Coefficients (c0, c1, c2, c3) depending on model type
- R-squared value (goodness of fit)
- Standard error
- Model type designation

## Architecture

### Core Components

```
CalibrationManagementService (Orchestrator)
    ├── RegressionCalculationService (Math engine)
    ├── CalibrationSessionRepositorio
    ├── CalibrationPointRepositorio
    └── RegressionModelRepositorio

Models:
    ├── CalibrationSession (container)
    ├── CalibrationPoint (data)
    └── RegressionModel (result)
```

### Data Flow

```
Upload CSV
    ↓
Parse Calibration Points
    ↓
Calculate Regression Models (Linear, Quadratic, Cubic)
    ↓
Persist Session, Points, and Models
    ↓
Set Active Model
    ↓
Apply to Incoming Measurements
```

## Usage

### 1. Uploading a Calibration

```java
CalibrationSession session = calibrationManagementService.uploadAndProcessCalibration(
    deviceId,                           // Device identifier
    multipartFile,                      // CSV calibration file
    RegressionModelType.LINEAR,         // Active model
    "Temperature sensor calibration",   // Description
    "john.doe@example.com"             // Uploaded by
);
```

### 2. CSV Format

Expected CSV format (with or without header):
```csv
patron_reading,instrument_reading
100.5,100.2
101.0,100.7
102.5,102.0
103.0,102.5
```

Or with metadata:
```csv
patron_reading,instrument_reading,metadata
100.5,100.2,point_1
101.0,100.7,point_2
```

### 3. Applying Corrections

```java
// Apply using device ID
Double correctedValue = calibrationManagementService.applyCorrection(deviceId, rawReading);

// Or using session directly
Double corrected = calibrationSession.applyCorrection(rawReading);
```

### 4. Querying Regression Models

```java
// Get active model
RegressionModel activeModel = calibrationSession.getActiveRegressionModel();

// Get specific model
RegressionModel quadModel = calibrationManagementService.getRegressionModel(
    calibrationId, 
    RegressionModelType.QUADRATIC
);

// Access coefficients
List<Double> coeffs = quadModel.getAllCoefficients();
double rSquared = quadModel.getRSquared();
```

### 5. Managing Calibrations

```java
// Check if device has active calibration
boolean hasActive = calibrationManagementService.hasActiveCalibration(deviceId);

// Get active calibration
CalibrationSession active = calibrationManagementService.getActiveCalibration(deviceId);

// Get calibration history
List<CalibrationSession> history = calibrationManagementService.getCalibrationHistory(deviceId);

// Change active model
calibrationManagementService.setActiveModelType(calibrationId, RegressionModelType.QUADRATIC);
```

## REST API Endpoints

### Upload Calibration
```
POST /api/calibrations/upload/{deviceId}
  ?modelType=LINEAR
  &description=Optional%20description
  &uploadedBy=user@example.com
  
Body: multipart/form-data with 'file' parameter
```

### Get Active Calibration
```
GET /api/calibrations/active/{deviceId}
```

### Get Calibration History
```
GET /api/calibrations/history/{deviceId}
```

### Get Model Coefficients
```
GET /api/calibrations/{calibrationId}/model/{modelType}/coefficients
```

### Apply Correction
```
POST /api/calibrations/{deviceId}/apply-correction
  ?instrumentReading=100.5
```

### Set Active Model
```
PUT /api/calibrations/{calibrationId}/set-model
  ?modelType=QUADRATIC
```

## Regression Analysis

### Goodness of Fit Metrics

#### R-squared (R²)
- Value range: 0 to 1
- Interpretation:
  - R² > 0.95: Excellent fit
  - 0.80 < R² ≤ 0.95: Good fit
  - 0.50 < R² ≤ 0.80: Fair fit
  - R² ≤ 0.50: Poor fit

#### Standard Error
- Measures average deviation from the model
- Lower is better
- Units match the correction values

### Model Selection Guidelines

| Scenario | Recommended Model |
|----------|-------------------|
| Linear instrument behavior | LINEAR |
| Slight nonlinearity | QUADRATIC |
| Complex nonlinearity | CUBIC |
| Uncertain | Start with LINEAR, increase complexity as needed |

## Backward Compatibility

The old `CalibrationCorrection` model and `CoeficientesCorreccion` are maintained for:
- Existing data migration
- Legacy application integration
- Gradual system transition

Both systems work together:
- New calibrations use the regression system
- Legacy calibrations still function via fallback mechanism
- Data can be migrated incrementally

## Migration Guide

### Existing Calibrations
1. Existing `CalibrationCorrection` records remain valid
2. Legacy coefficient-based corrections continue to work
3. New uploads automatically use regression system

### For Application Code

**Old (Legacy):**
```java
CoeficientesCorreccion coef = ...;
double corrected = coef.aplicarCorreccion(rawValue);
```

**New (Recommended):**
```java
double corrected = calibrationSession.applyCorrection(rawValue);
```

## Modularity & Future Features

The system is designed to support:

### 1. Calibration Expiration Validation
```java
LocalDateTime expirationDate = session.getExpirationDate();
if (LocalDateTime.now().isAfter(expirationDate)) {
    // Trigger recalibration alert
}
```

### 2. Drift-Based Recalibration Intervals
```java
String driftData = session.getDriftData();
// Analyze drift trends over time
// Recommend recalibration based on drift
```

### 3. Certificate Generation
```java
// Generate calibration certificate with:
// - Model type and formula
// - Coefficients and R²
// - Calibration points
// - Acceptance criteria
```

### 4. Multi-Range Calibration
```java
// Support different models for different ranges
Map<RegressionModelType, RegressionModel> segmentedModels = ...;
```

## Database Schema

### calibration_sessions
- Contains session metadata and configuration
- Tracks device, dates, active model, description

### calibration_points
- Raw calibration data (patron and instrument readings)
- Correction calculation
- Metadata per point

### regression_models
- Calculated coefficients for each model type
- Goodness-of-fit metrics
- One entry per model type per session

## Performance Considerations

1. **Regression Calculation**: O(n²) for Gaussian elimination, where n = number of coefficients
2. **Model Application**: O(1) per measurement (simple polynomial evaluation)
3. **Storage**: Minimal overhead (one session + 3-4 model records per calibration)

## Error Handling

- Insufficient calibration points → `IllegalArgumentException`
- Singular matrix in regression → `IllegalArgumentException`
- No active calibration for device → `IllegalStateException`
- Invalid model type → `IllegalArgumentException`

## Testing

Unit tests included in `RegressionCalculationServiceTest`:
- Linear, quadratic, and cubic regression calculations
- Model application to measurements
- R² calculation verification
- Edge cases and error scenarios

## Configuration

In `application.properties`:
```properties
# Calibration file upload directory
app.calibrations.upload-dir=calibrations
```

## References

### Regression Mathematics
- Least-squares method: Fit polynomial coefficients to minimize sum of squared residuals
- R-squared calculation: Coefficient of determination = 1 - (SS_residual / SS_total)
- Gaussian elimination: Solve system of linear equations from normal equations

### Quality Standards
- Calibration documentation: ISO 17025
- Measurement uncertainty: ISO Guide to the Expression of Uncertainty in Measurement (GUM)
- Equipment calibration: ISO 10012
