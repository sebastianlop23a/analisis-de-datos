package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.Sensor;
import com.sivco.gestion_archivos.servicios.SensorServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api/sensores")
@CrossOrigin(origins = "*")
public class SensorControlador {
    
    private static final Logger logger = LoggerFactory.getLogger(SensorControlador.class);
    
    @Autowired
    private SensorServicio sensorServicio;

    @Autowired
    private com.sivco.gestion_archivos.servicios.CalibrationCorrectionServicio calibrationServicio;

    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * GET /api/sensores - Listar todos los sensores
     */
    @GetMapping
    public ResponseEntity<List<Sensor>> listarTodos(@RequestParam(required = false) Boolean activos) {
        logger.info("GET /api/sensores - activos={}", activos);
        List<Sensor> sensores = (activos != null && activos) 
            ? sensorServicio.listarActivos() 
            : sensorServicio.listarTodos();
        return ResponseEntity.ok(sensores);
    }
    
    /**
     * GET /api/sensores/{id} - Obtener sensor por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Sensor> obtenerPorId(@PathVariable Long id) {
        logger.info("GET /api/sensores/{}", id);
        return sensorServicio.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/sensores/codigo/{codigo} - Obtener sensor por código
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Sensor> obtenerPorCodigo(@PathVariable String codigo) {
        logger.info("GET /api/sensores/codigo/{}", codigo);
        return sensorServicio.obtenerPorCodigo(codigo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * POST /api/sensores - Crear nuevo sensor
     */
    @PostMapping
    public ResponseEntity<Sensor> crear(@RequestBody Sensor sensor) {
        // Creation of sensors requires uploading a calibration CSV to ensure the sensor
        // is fully configured. Reject simple JSON creation and direct clients to
        // POST /api/sensores/crear-con-calibracion (multipart) which creates the sensor
        // and associates the calibration in a single atomic operation.
        logger.warn("Rejected plain sensor creation for {} - use crear-con-calibracion", sensor.getCodigo());
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * POST /api/sensores/crear-con-calibracion - Crear sensor y subir CSV de calibración obligatoria
     * Recibe un campo `sensor` con JSON y `archivo` con el CSV de coeficientes (o puntos).
     */
    @PostMapping(path = "/crear-con-calibracion", consumes = {"multipart/form-data"})
    public ResponseEntity<?> crearConCalibracion(
            @RequestParam("sensor") String sensorJson,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "subidoPor", required = false) String subidoPor) {

        try {
            // Parsear JSON del sensor
            Sensor sensor = objectMapper.readValue(sensorJson, Sensor.class);

            // Crear sensor
            Sensor nuevo = sensorServicio.crear(sensor);

            // Subir calibración mediante el servicio (legacy wrapper delega al nuevo sistema)
            com.sivco.gestion_archivos.modelos.CalibrationCorrection cal = calibrationServicio.uploadCalibration(
                    nuevo.getId(), archivo, descripcion, subidoPor);

            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                    .body(Map.of("sensor", nuevo, "calibracion", cal));

        } catch (JsonProcessingException jpe) {
            return ResponseEntity.badRequest().body(Map.of("error", "JSON de sensor inválido: " + jpe.getMessage()));
        } catch (IOException ioe) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error guardando archivo: " + ioe.getMessage()));
        } catch (RuntimeException re) {
            return ResponseEntity.badRequest().body(Map.of("error", re.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * PUT /api/sensores/{id} - Actualizar sensor
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Sensor sensor) {
        logger.info("PUT /api/sensores/{}", id);
        try {
            Sensor sensorActualizado = sensorServicio.actualizar(id, sensor);
            return ResponseEntity.ok(sensorActualizado);
        } catch (RuntimeException e) {
            logger.error("Error actualizando sensor", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/sensores/{id} - Eliminar sensor
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        logger.info("DELETE /api/sensores/{}", id);
        try {
            sensorServicio.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error eliminando sensor", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * PATCH /api/sensores/{id}/desactivar - Desactivar sensor (soft delete)
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        logger.info("PATCH /api/sensores/{}/desactivar", id);
        sensorServicio.desactivar(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * GET /api/sensores/ubicacion/{ubicacion} - Buscar por ubicación
     */
    @GetMapping("/ubicacion/{ubicacion}")
    public ResponseEntity<List<Sensor>> buscarPorUbicacion(@PathVariable String ubicacion) {
        logger.info("GET /api/sensores/ubicacion/{}", ubicacion);
        List<Sensor> sensores = sensorServicio.buscarPorUbicacion(ubicacion);
        return ResponseEntity.ok(sensores);
    }
    
    /**
     * GET /api/sensores/tipo/{tipo} - Buscar por tipo de sonda
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Sensor>> buscarPorTipo(@PathVariable String tipo) {
        logger.info("GET /api/sensores/tipo/{}", tipo);
        List<Sensor> sensores = sensorServicio.buscarPorTipoSonda(tipo);
        return ResponseEntity.ok(sensores);
    }
    
    /**
     * GET /api/sensores/alertas/calibracion - Obtener sensores con calibración pendiente
     */
    @GetMapping("/alertas/calibracion")
    public ResponseEntity<List<Sensor>> obtenerAlertasCalibracion(
            @RequestParam(defaultValue = "30") int diasAnticipacion) {
        logger.info("GET /api/sensores/alertas/calibracion?diasAnticipacion={}", diasAnticipacion);
        List<Sensor> sensores = sensorServicio.obtenerSensoresConCalibracionPendiente(diasAnticipacion);
        return ResponseEntity.ok(sensores);
    }
    
    /**
     * GET /api/sensores/sin-calibrar - Obtener sensores sin calibrar
     */
    @GetMapping("/sin-calibrar")
    public ResponseEntity<List<Sensor>> obtenerSinCalibrar() {
        logger.info("GET /api/sensores/sin-calibrar");
        List<Sensor> sensores = sensorServicio.obtenerSensoresSinCalibrar();
        return ResponseEntity.ok(sensores);
    }
    
    /**
     * POST /api/sensores/{id}/calibrar - Registrar calibración
     */
    @PostMapping("/{id}/calibrar")
    public ResponseEntity<Sensor> registrarCalibracion(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        logger.info("POST /api/sensores/{}/calibrar", id);
        try {
            LocalDate fecha = payload.containsKey("fecha") 
                ? LocalDate.parse(payload.get("fecha"))
                : LocalDate.now();
            Sensor sensor = sensorServicio.registrarCalibracion(id, fecha);
            return ResponseEntity.ok(sensor);
        } catch (RuntimeException e) {
            logger.error("Error registrando calibración", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/sensores/{id}/recalibrar - Subir archivo de calibración para el sensor y marcar activa
     */
    @PostMapping("/{id}/recalibrar")
    public ResponseEntity<?> recalibrarSensor(
            @PathVariable Long id,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "subidoPor", required = false) String subidoPor) {
        try {
            com.sivco.gestion_archivos.modelos.CalibrationCorrection c = calibrationServicio.uploadCalibration(id, archivo, descripcion, subidoPor);
            return ResponseEntity.ok(c);
        } catch (IOException ioe) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("Error guardando archivo: " + ioe.getMessage());
        } catch (RuntimeException re) {
            return ResponseEntity.badRequest().body(re.getMessage());
        }
    }
}
