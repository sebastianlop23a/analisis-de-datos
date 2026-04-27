package com.sivco.gestion_archivos.servicios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.*;
import com.sivco.gestion_archivos.repositorios.calibration.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.sivco.gestion_archivos.servicios.SensorServicio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Gestiona el ciclo de vida completo de las calibraciones para dispositivos.
 *
 * Responsabilidades:
 * - Parsear datos de calibración desde ficheros subidos (formato CSV/Excel)
 * - Crear puntos de calibración a partir de los datos
 * - Calcular modelos de regresión usando los puntos de calibración
 * - Gestionar calibraciones activas/archivadas por dispositivo
 * - Persistir metadatos y coeficientes de las calibraciones
 * - Aplicar correcciones a medidas usando el modelo activo
 */
@Service
public class CalibrationManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(CalibrationManagementService.class);
    
    @Autowired
    private CalibrationSessionRepositorio calibrationSessionRepo;
    
    @Autowired
    private CalibrationPointRepositorio calibrationPointRepo;
    
    @Autowired
    private RegressionModelRepositorio regressionModelRepo;
    
    @Autowired
    private RegressionCalculationService regressionCalculationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SensorServicio sensorServicio;
    
    private final String uploadDirectory = "calibrations";
    
    /**
     * Sube y procesa una nueva calibración para un dispositivo.
     * Archiva automáticamente la calibración activa previa si existe.
     *
     * @param deviceId el id del dispositivo a calibrar
     * @param file el fichero de datos de calibración (CSV/Excel)
     * @param activeModelType el tipo de modelo de regresión solicitado (opcional)
     * @param description descripción opcional de la calibración
     * @param uploadedBy usuario que realiza la subida
     * @return la sesión de calibración creada
     */
    public CalibrationSession uploadAndProcessCalibration(
            Long deviceId,
            MultipartFile file,
            RegressionModelType activeModelType,
            String description,
            String uploadedBy) throws IOException {
        
        logger.info("Starting calibration upload for device {}", deviceId);
        
        // Leer el fichero subido en memoria para poder intentar varios parsers sin volver a leer el stream
        byte[] fileBytes = file.getBytes();

        // Parsear puntos de calibración desde el fichero subido (CSV o Excel)
        List<CalibrationPoint> calibrationPoints = parseCalibrationFile(new java.io.ByteArrayInputStream(fileBytes), file.getOriginalFilename());

        // If not enough calibration points, attempt to parse the file as a coefficients CSV
        final Map<String, double[]> parsedCoefficients;
        final boolean usedCoefficientsFallback;
        if (calibrationPoints.size() < 2) {
            logger.info("Parsed {} calibration points from CSV - attempting coefficient-file fallback", calibrationPoints.size());
            Map<String, double[]> tmp = parseCoefficientsCSV(new java.io.ByteArrayInputStream(fileBytes));
            if (!tmp.isEmpty()) {
                parsedCoefficients = tmp;
                usedCoefficientsFallback = true;
                logger.info("Detected {} coefficient rows in uploaded file - will create regression models from coefficients", parsedCoefficients.size());
            } else {
                // throw the same spanish message as the underlying computation service
                throw new IllegalArgumentException("Se requieren al menos 2 puntos de calibración para calcular un modelo de regresión");
            }
        } else {
            parsedCoefficients = Collections.emptyMap();
            usedCoefficientsFallback = false;
        }

        // Crear nueva sesión de calibración (aún no persistida)
        CalibrationSession session = new CalibrationSession();
        session.setDeviceId(deviceId);
        session.setCalibrationDate(LocalDateTime.now());
        session.setUploadedDate(LocalDateTime.now());
        session.setIsActive(true);
        // Active model will be determined after models are calculated when possible
        session.setActiveModelType(null);
        session.setDescription(description);
        session.setUploadedBy(uploadedBy != null ? uploadedBy : "System");
        session.setSourceFileName(file.getOriginalFilename());
        session.setFileSizeBytes(file.getSize());

        // Guardar el fichero subido en disco (solo IO)
        String filePath = storeCalibrationFile(deviceId, file);
        session.setFilePath(filePath);

        // Para subidas por sensor, establecer expiración a un año desde ahora
        if (deviceId != null && deviceId != 0L) {
            session.setExpirationDate(LocalDateTime.now().plusYears(1));
        }

        // Si usamos el fallback de coeficientes, imponer una carga anual y cobertura para todos los sensores
        if (usedCoefficientsFallback) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfYear = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
            LocalDateTime endOfYear = LocalDateTime.of(now.getYear(), 12, 31, 23, 59, 59);

                // Exigir que las cargas de coeficientes sean siempre por sensor (deviceId).
                // No se permiten cargas globales que contengan coeficientes para varios sensores.
                if (deviceId == null || deviceId == 0L) {
                    throw new IllegalArgumentException("Las cargas de coeficientes deben realizarse por sensor (proveer deviceId). No se permiten cargas globales para todos los sensores.");
                }

                // Comprobar existencia por (device_id, año).
                boolean alreadyUploaded;
                int year = now.getYear();
                // Usar la columna generada uploaded_year via método de conveniencia
                alreadyUploaded = calibrationSessionRepo.existsByDeviceIdAndUploadedYear(deviceId, year);

                if (alreadyUploaded) {
                    throw new IllegalStateException("Ya existe una carga de correcciones para el año " + now.getYear() + ". Solo se permite una carga anual.");
            }

            // Verificar que los coeficientes cubren todos los sensores activos
            List<com.sivco.gestion_archivos.modelos.Sensor> activeSensors = sensorServicio.listarActivos();
            Set<String> missing = new LinkedHashSet<>();
            Set<String> provided = new HashSet<>();
            for (String key : parsedCoefficients.keySet()) provided.add(key.trim());
            // Si el fichero de coeficientes incluye una fila por defecto (sin sensor) y la subida es por dispositivo,
            // tratar la entrada por defecto como los coeficientes del sensor objetivo.
            if (provided.contains("_default")) {
                String defaultKey = "_default";
                // resolve target sensor code if possible
                if (deviceId != null && deviceId != 0L) {
                    try {
                        var optSensor = sensorServicio.obtenerPorId(deviceId);
                        if (optSensor.isPresent()) {
                            String targetCode = optSensor.get().getCodigo();
                            if (targetCode != null) {
                                provided.add(targetCode);
                            }
                        }
                    } catch (Exception ex) {
                        // ignorar errores de resolución aquí; la comprobación de cobertura fallará más adelante si hace falta
                    }
                }
            }
            if (deviceId != null && deviceId != 0L) {
                // Subida por dispositivo: sólo requerir coeficientes para el sensor objetivo
                try {
                    var optSensor = sensorServicio.obtenerPorId(deviceId);
                    if (optSensor.isPresent()) {
                        String targetCode = optSensor.get().getCodigo();
                        if (targetCode == null) targetCode = "<sin_codigo>";
                        if (!provided.contains(targetCode)) {
                            throw new IllegalArgumentException("El archivo de coeficientes no contiene entrada para el sensor objetivo: " + targetCode);
                        }
                    } else {
                        throw new IllegalArgumentException("No se pudo resolver el sensor objetivo para deviceId=" + deviceId);
                    }
                } catch (IllegalArgumentException iae) {
                    throw iae;
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Error verificando cobertura de coeficientes: " + ex.getMessage());
                }
            } else {
                // Validación legacy: requerir para todos los sensores activos
                for (com.sivco.gestion_archivos.modelos.Sensor s : activeSensors) {
                    String code = s.getCodigo();
                    if (code == null || !provided.contains(code)) {
                        missing.add(code == null ? "<sin_codigo>" : code);
                    }
                }
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("El archivo de coeficientes no contiene entradas para todos los sensores activos. Faltan: " + String.join(", ", missing));
                }
            }
        }

        // Persistir sesión, puntos y modelos dentro de una transacción corta
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CalibrationSession savedSession = tx.execute(status -> {
            // Intentar reutilizar una sesión activa existente para este dispositivo
            Optional<CalibrationSession> optExisting = calibrationSessionRepo.findByDeviceIdAndIsActiveTrue(deviceId);

            boolean mergingIntoExisting = optExisting.isPresent() && !usedCoefficientsFallback;

            CalibrationSession s;
            if (mergingIntoExisting) {
                // Usar la sesión activa existente (merge): no archivamos ni creamos una nueva
                s = optExisting.get();
                // actualizar algunos metadatos de subida
                s.setUploadedDate(LocalDateTime.now());
                s.setSourceFileName(file.getOriginalFilename());
                s.setFileSizeBytes(file.getSize());
                // guardar cambios mínimos ahora (se finalizará al final)
                s = calibrationSessionRepo.save(s);
            } else {
                // Archivar la calibración activa previa y crear una nueva sesión
                archivePreviousCalibration(deviceId);

                // Save session
                s = calibrationSessionRepo.save(session);
            }

            // Preparar contenedores para modelos/puntos que se usarán abajo
            List<RegressionModel> savedModels = new ArrayList<>();
            Map<RegressionModelType, RegressionModel> models = null;
            List<CalibrationPoint> savedPoints = null;
            if (usedCoefficientsFallback) {
                    // Si es una subida por sensor, resolver el código del sensor objetivo e ignorar otras filas
                    String targetSensorCode = null;
                    if (deviceId != null && deviceId != 0L) {
                        try {
                            var optSensor = sensorServicio.obtenerPorId(deviceId);
                            if (optSensor.isPresent()) targetSensorCode = optSensor.get().getCodigo();
                        } catch (Exception ex) {
                            logger.warn("No se pudo resolver el sensor {} para fallback de coeficientes", deviceId);
                        }
                    }

                    // Crear modelos de regresión directamente a partir de los coeficientes proporcionados
                    for (Map.Entry<String, double[]> entry : parsedCoefficients.entrySet()) {
                        String sensorCode = entry.getKey().trim();
                        // si la fila usa la forma por defecto (sin sensor), mapearla al código del sensor objetivo si está disponible
                        if ("_default".equals(sensorCode) && deviceId != null && deviceId != 0L) {
                            try {
                                var optSensor = sensorServicio.obtenerPorId(deviceId);
                                if (optSensor.isPresent()) sensorCode = optSensor.get().getCodigo();
                            } catch (Exception ex) {
                                // conservar _default si la resolución falla
                            }
                        }
                        // if targetSensorCode is set, only process matching row
                        if (targetSensorCode != null && !targetSensorCode.equals(sensorCode)) continue;
                        double[] c = entry.getValue(); // expected order: A (const), B (x), C (x^2), D (x^3)

                    // Crear modelo lineal (si B o A están presentes)
                    RegressionModel linear = new RegressionModel();
                    linear.setCalibrationSession(s);
                    linear.setSensorCode(sensorCode);
                    linear.setModelType(RegressionModelType.LINEAR);
                    linear.setCoefficient0(c.length > 1 ? c[1] : 0.0); // slope = B
                    linear.setCoefficient1(c.length > 0 ? c[0] : 0.0); // intercept = A
                    linear.setRSquared(1.0);
                    linear.setStandardError(0.0);
                    savedModels.add(regressionModelRepo.save(linear));

                    // Crear modelo cuadrático
                    RegressionModel quad = new RegressionModel();
                    quad.setCalibrationSession(s);
                    quad.setSensorCode(sensorCode);
                    quad.setModelType(RegressionModelType.QUADRATIC);
                    quad.setCoefficient2(c.length > 2 ? c[2] : 0.0); // a (x^2)
                    quad.setCoefficient0(c.length > 1 ? c[1] : 0.0); // b (x)
                    quad.setCoefficient1(c.length > 0 ? c[0] : 0.0); // c (const)
                    quad.setRSquared(1.0);
                    quad.setStandardError(0.0);
                    savedModels.add(regressionModelRepo.save(quad));

                    // Crear modelo cúbico
                    RegressionModel cubic = new RegressionModel();
                    cubic.setCalibrationSession(s);
                    cubic.setSensorCode(sensorCode);
                    cubic.setModelType(RegressionModelType.CUBIC);
                    cubic.setCoefficient3(c.length > 3 ? c[3] : 0.0); // a x^3
                    cubic.setCoefficient2(c.length > 2 ? c[2] : 0.0); // b x^2
                    cubic.setCoefficient0(c.length > 1 ? c[1] : 0.0); // c x
                    cubic.setCoefficient1(c.length > 0 ? c[0] : 0.0); // d const
                    cubic.setRSquared(1.0);
                    cubic.setStandardError(0.0);
                    savedModels.add(regressionModelRepo.save(cubic));
                }

                s.setRegressionModels(savedModels);
            } else {
                // Asociar y guardar puntos: si existe una sesión previa activa, añadimos (merge)
                List<CalibrationPoint> existingPoints = calibrationPointRepo.findByCalibrationSessionIdOrderByPointOrderAsc(s.getId());
                int pointOrder = existingPoints != null ? existingPoints.size() : 0;

                for (CalibrationPoint point : calibrationPoints) {
                    point.setCalibrationSession(s);
                    point.setPointOrder(pointOrder++);
                    point.calculateCorrection();
                }

                List<CalibrationPoint> newlySaved = calibrationPointRepo.saveAll(calibrationPoints);

                // Construir lista combinada de puntos
                List<CalibrationPoint> allPoints = new ArrayList<>();
                if (existingPoints != null) allPoints.addAll(existingPoints);
                allPoints.addAll(newlySaved);

                s.setCalibrationPoints(allPoints);
                savedPoints = newlySaved;
                logger.info("Appended {} calibration points to session {} (total now={})", newlySaved.size(), s.getId(), allPoints.size());

                // Calcular y guardar modelos de regresión sobre el conjunto combinado
                models = regressionCalculationService.calculateAllModels(s, allPoints);

                // Reemplazar modelos anteriores por los recalculados
                List<RegressionModel> existingModels = regressionModelRepo.findByCalibrationSessionId(s.getId());
                if (existingModels != null && !existingModels.isEmpty()) {
                    regressionModelRepo.deleteAll(existingModels);
                }

                for (RegressionModel model : models.values()) {
                    model.setCalibrationSession(s);
                    savedModels.add(regressionModelRepo.save(model));
                    logger.info("Calculated {} model with R² = {}", model.getModelType(), model.getRSquared());
                }
                s.setRegressionModels(savedModels);
            }

            // Determinar qué modelo activar:
            RegressionModelType selected = null;
            if (activeModelType != null) {
                // Si el usuario solicitó un modelo específico, asegurarse de que esté presente en savedModels
                boolean hasRequested = savedModels.stream().anyMatch(m -> m.getModelType() == activeModelType);
                if (hasRequested) selected = activeModelType;
            }

            if (selected == null) {
                if (models != null && savedPoints != null) {
                    // Preferir el mejor R² ajustado cuando calculamos modelos a partir de puntos
                    selected = chooseBestModelByAdjustedRSquared(models, savedPoints.size());
                } else {
                    // Fallback: preferir LINEAR si está presente
                    boolean hasLinear = savedModels.stream().anyMatch(m -> m.getModelType() == RegressionModelType.LINEAR);
                    selected = hasLinear ? RegressionModelType.LINEAR : (savedModels.isEmpty() ? null : savedModels.get(0).getModelType());
                }
            }
            s.setActiveModelType(selected);

            // Guardado final con relaciones
            return calibrationSessionRepo.save(s);
        });

        logger.info("Calibration upload completed successfully for device {}", deviceId);
        return savedSession;
    }
    
    /**
     * Gets the active calibration for a device
     * 
     * @param deviceId the device ID
     * @return the active calibration session, or null if none exists
     */
    @Transactional
    public CalibrationSession getActiveCalibration(Long deviceId) {
        // Prefer device-specific active calibration; archive expired sessions and fall back to a global session (deviceId = 0)
        LocalDateTime now = LocalDateTime.now();

        Optional<CalibrationSession> opt = calibrationSessionRepo.findByDeviceIdAndIsActiveTrue(deviceId);
        if (opt.isPresent()) {
            CalibrationSession s = opt.get();
            if (s.getExpirationDate() != null && s.getExpirationDate().isBefore(now)) {
                s.setIsActive(false);
                calibrationSessionRepo.save(s);
            } else {
                initializeRegressionModels(s);
                return s;
            }
        }

        Optional<CalibrationSession> optGlobal = calibrationSessionRepo.findByDeviceIdAndIsActiveTrue(0L);
        if (optGlobal.isPresent()) {
            CalibrationSession g = optGlobal.get();
            if (g.getExpirationDate() != null && g.getExpirationDate().isBefore(now)) {
                g.setIsActive(false);
                calibrationSessionRepo.save(g);
                return null;
            }
            initializeRegressionModels(g);
            return g;
        }

        return null;
    }

    private void initializeRegressionModels(CalibrationSession session) {
        if (session != null && session.getRegressionModels() != null) {
            session.getRegressionModels().size();
        }
    }
    
    /**
     * Gets all calibrations (active and archived) for a device
     * 
     * @param deviceId the device ID
     * @return list of calibration sessions, sorted by date descending
     */
    public List<CalibrationSession> getCalibrationHistory(Long deviceId) {
        return calibrationSessionRepo.findByDeviceIdOrderByCalibrationDateDesc(deviceId);
    }
    
    /**
     * Checks if a device has an active calibration
     * 
     * @param deviceId the device ID
     * @return true if device has an active calibration
     */
    public boolean hasActiveCalibration(Long deviceId) {
        return calibrationSessionRepo.existsByDeviceIdAndIsActiveTrue(deviceId);
    }
    
    /**
     * Applies correction to a measurement using the active calibration
     * 
     * @param deviceId the device ID
     * @param instrumentReading the raw measurement from the instrument
     * @return the corrected measurement
     * @throws IllegalStateException if no active calibration exists
     */
    public Double applyCorrection(Long deviceId, Double instrumentReading) {
        CalibrationSession activeCalibration = getActiveCalibration(deviceId);
        if (activeCalibration == null) {
            throw new IllegalStateException("No active calibration found for device: " + deviceId);
        }
        return activeCalibration.applyCorrection(instrumentReading);
    }
    
    /**
     * Sets the active regression model for a calibration session
     * 
     * @param calibrationId the calibration session ID
     * @param modelType the regression model type to activate
     */
    public void setActiveModelType(Long calibrationId, RegressionModelType modelType) {
        CalibrationSession session = calibrationSessionRepo.findById(calibrationId)
            .orElseThrow(() -> new IllegalArgumentException("Calibration not found: " + calibrationId));
        
        // Verify model exists
        regressionModelRepo.findByCalibrationSessionIdAndModelType(calibrationId, modelType)
            .orElseThrow(() -> new IllegalArgumentException("Model " + modelType + " not found for calibration " + calibrationId));
        
        session.setActiveModelType(modelType);
        calibrationSessionRepo.save(session);
        logger.info("Set active model to {} for calibration {}", modelType, calibrationId);
    }
    
    /**
     * Retrieves a specific regression model
     * 
     * @param calibrationId the calibration session ID
     * @param modelType the regression model type
     * @return the regression model
     */
    public RegressionModel getRegressionModel(Long calibrationId, RegressionModelType modelType) {
        return regressionModelRepo.findByCalibrationSessionIdAndModelType(calibrationId, modelType)
            .orElse(null);
    }
    
    /**
     * Archives the previous active calibration for a device
     */
    private void archivePreviousCalibration(Long deviceId) {
        calibrationSessionRepo.findByDeviceIdAndIsActiveTrue(deviceId)
            .ifPresent(previous -> {
                previous.setIsActive(false);
                calibrationSessionRepo.save(previous);
                logger.info("Archived previous calibration {} for device {}", previous.getId(), deviceId);
            });
    }

    /**
     * Choose the best model by adjusted R-squared to avoid selecting
     * higher-degree models that overfit when data is scarce.
     */
    private RegressionModelType chooseBestModelByAdjustedRSquared(Map<RegressionModelType, RegressionModel> models, int nPoints) {
        if (models == null || models.isEmpty()) return null;

        double bestAdj = Double.NEGATIVE_INFINITY;
        RegressionModelType bestType = null;

        for (Map.Entry<RegressionModelType, RegressionModel> e : models.entrySet()) {
            RegressionModelType type = e.getKey();
            RegressionModel model = e.getValue();
            Double r2 = model.getRSquared();
            if (r2 == null) r2 = Double.NEGATIVE_INFINITY;

            int p = type.getCoefficientCount(); // number of coefficients

            double adjR2;
            // adjusted R2 = 1 - (1-R2)*(n-1)/(n-p-1)
            double denom = (nPoints - p - 1);
            if (denom <= 0) {
                // not enough degrees of freedom to compute adjusted R2 reliably
                adjR2 = r2;
            } else {
                adjR2 = 1.0 - (1.0 - r2) * (nPoints - 1) / denom;
            }

            if (adjR2 > bestAdj) {
                bestAdj = adjR2;
                bestType = type;
            }
        }

        logger.info("Selected active model {} by adjusted R² (score={})", bestType, bestAdj);
        return bestType;
    }
    
    /**
     * Stores the calibration file on disk
     * 
     * @param deviceId the device ID
     * @param file the uploaded file
     * @return the file path
     */
    private String storeCalibrationFile(Long deviceId, MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(uploadDirectory, "device_" + deviceId);
        Files.createDirectories(uploadDir);
        
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());
        
        logger.info("Stored calibration file at {}", filePath);
        return filePath.toString();
    }

    /**
     * Chooses parser based on uploaded file type (CSV or Excel)
     */
    private List<CalibrationPoint> parseCalibrationFile(java.io.InputStream inputStream, String filename) throws IOException {
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
                return parseExcelFile(inputStream);
            }
        }
        return parseCalibrationCSV(inputStream);
    }

    /**
     * Attempts to parse a coefficient-style CSV where rows contain:
     * "SENSOR","A","B","C","D",... and returns a map sensor -> [A,B,C,D]
     */
    private Map<String, double[]> parseCoefficientsCSV(java.io.InputStream inputStream) throws IOException {
        Map<String, double[]> result = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (!headerSkipped && isHeaderLine(line)) {
                    headerSkipped = true;
                    continue;
                }

                String[] parts;
                if (line.contains(";")) {
                    parts = line.split(";(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
                } else {
                    parts = line.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
                }
                for (int i = 0; i < parts.length; i++) parts[i] = cleanValue(parts[i]);

                // Accept either: SENSOR,A,B,C,D  OR  A,B,C,D (coefficients-only row)
                if (parts.length >= 4) {
                    try {
                        // If the first part parses as a number, assume the row is coefficients-only
                        double first = parseNumber(parts[0]);
                        double a = first;
                        double b = parseNumber(parts[1]);
                        double c = parseNumber(parts[2]);
                        double d = parseNumber(parts[3]);
                        // use a sentinel key for default/no-sensor rows
                        result.put("_default", new double[] { a, b, c, d });
                    } catch (Exception e1) {
                        // first part is not numeric; treat parts[0] as sensor id if we have at least 5 columns
                        if (parts.length >= 5) {
                            try {
                                String sensor = parts[0].trim().replaceAll("\"", "");
                                double a = parseNumber(parts[1]);
                                double b = parseNumber(parts[2]);
                                double c = parseNumber(parts[3]);
                                double d = parseNumber(parts[4]);
                                result.put(sensor, new double[] { a, b, c, d });
                            } catch (Exception e2) {
                                // ignore non-numeric rows
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Parses an Excel workbook (.xls/.xlsx). Reads all sheets and extracts
     * the first two numeric-like values in each row as (patron,instrument).
     */
    private List<CalibrationPoint> parseExcelFile(InputStream inputStream) throws IOException {
        List<CalibrationPoint> points = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(inputStream)) {
            for (Sheet sheet : wb) {
                int rowNum = 0;
                boolean headerSkipped = false;
                for (Row row : sheet) {
                    rowNum++;
                    // build a quick concatenation for header detection
                    StringBuilder rowText = new StringBuilder();
                    for (Cell cell : row) rowText.append(cell.toString()).append(' ');
                    if (!headerSkipped && isHeaderLine(rowText.toString())) {
                        headerSkipped = true;
                        continue;
                    }

                    Double v1 = null, v2 = null;
                    short last = row.getLastCellNum();
                    if (last < 0) continue;
                    for (int c = 0; c < last; c++) {
                        Cell cell = row.getCell(c);
                        if (cell == null) continue;
                        try {
                            double val;
                            if (cell.getCellType() == CellType.NUMERIC) {
                                val = cell.getNumericCellValue();
                            } else {
                                String s = cleanValue(cell.toString());
                                val = parseNumber(s);
                            }
                            if (v1 == null) v1 = val; else if (v2 == null) { v2 = val; break; }
                        } catch (Exception ex) {
                            // non-numeric cell, skip
                        }
                    }

                    if (v1 != null && v2 != null) {
                        CalibrationPoint point = new CalibrationPoint();
                        point.setPatronReading(v1);
                        point.setInstrumentReading(v2);
                        point.setMetadata("sheet:" + sheet.getSheetName());
                        point.calculateCorrection();
                        points.add(point);
                    } else {
                        logger.debug("Skipping non-numeric row {} in sheet {}", rowNum, sheet.getSheetName());
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        logger.info("Parsed {} calibration points from Excel", points.size());
        return points;
    }
    
    /**
     * Parses calibration data from CSV format.
     * Expected format: patron_reading,instrument_reading[,metadata]
     * 
     * @param inputStream the input stream of the CSV file
     * @return list of parsed calibration points
     */
    private List<CalibrationPoint> parseCalibrationCSV(InputStream inputStream) throws IOException {
        List<CalibrationPoint> points = new ArrayList<>();

        // Read all lines first (no DB interaction)
        List<String[]> rows = new ArrayList<>();
        boolean headerSkipped = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                if (!headerSkipped && isHeaderLine(line)) {
                    headerSkipped = true;
                    logger.debug("Skipped CSV header: {}", line);
                    continue;
                }

                // support both comma and semicolon delimiters; keep quoted values intact
                String[] parts;
                if (line.contains(";")) {
                    parts = line.split(";(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
                } else {
                    parts = line.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
                }
                // trim and clean each value
                for (int i = 0; i < parts.length; i++) parts[i] = cleanValue(parts[i]);
                rows.add(parts);
            }
        }

        if (rows.isEmpty()) {
            logger.info("Parsed 0 calibration points from CSV");
            return points;
        }

        // Detect best column pair that parse as doubles in most rows
        int maxCols = rows.stream().mapToInt(r -> r.length).max().orElse(0);
        int bestI = -1, bestJ = -1, bestCount = 0;

        for (int i = 0; i < maxCols; i++) {
            for (int j = i + 1; j < Math.min(maxCols, i + 6); j++) {
                int count = 0;
                int checked = 0;
                for (String[] parts : rows) {
                    if (parts.length <= j) continue;
                    checked++;
                    try {
                        Double.parseDouble(parts[i]);
                        Double.parseDouble(parts[j]);
                        count++;
                    } catch (Exception e) {
                        // not a pair for this row
                    }
                }
                if (count > bestCount && checked >= 2) {
                    bestCount = count;
                    bestI = i;
                    bestJ = j;
                }
            }
        }

        if (bestI == -1 || bestCount < 2) {
            // fallback: try (0,1)
            bestI = 0; bestJ = 1;
        }

        int rowNum = headerSkipped ? 2 : 1;
        for (String[] parts : rows) {
            try {
                if (parts.length <= Math.max(bestI, bestJ)) {
                    rowNum++;
                    continue;
                }
                double patronReading = Double.parseDouble(parts[bestI]);
                double instrumentReading = Double.parseDouble(parts[bestJ]);

                CalibrationPoint point = new CalibrationPoint();
                point.setPatronReading(patronReading);
                point.setInstrumentReading(instrumentReading);
                if (parts.length > Math.max(bestJ, 2)) {
                    point.setMetadata(parts[Math.min(parts.length - 1, Math.max(bestJ + 1, 2))]);
                }
                point.calculateCorrection();
                points.add(point);
            } catch (Exception e) {
                logger.warn("Failed to parse calibration point at line {}: {}", rowNum, Arrays.toString(parts));
            }
            rowNum++;
        }

        logger.info("Parsed {} calibration points from CSV", points.size());
        return points;
    }
    
    /**
     * Tries to parse a number, allowing comma as decimal separator.
     */
    private double parseNumber(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // try replacing comma with dot if appropriate
            String alt = value.replace(',', '.');
            return Double.parseDouble(alt);
        }
    }

    /**
     * Determines if a line is a CSV header
     */
    private boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("patron") || lower.contains("instrument") || lower.contains("reading") 
            || lower.contains("sensor") || lower.contains("coeficiente");
    }
    
    /**
     * Cleans a CSV value by removing quotes and whitespace
     */
    private String cleanValue(String value) {
        return value.trim().replaceAll("^['\\\"]|['\\\"]$", "");
    }
}
