package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.Sensor;
import com.sivco.gestion_archivos.repositorios.SensorRepositorio;
import com.sivco.gestion_archivos.repositorios.CalibrationCorrectionRepositorio;
import com.sivco.gestion_archivos.repositorios.CalibrationAlertRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SensorServicio {
    
    @Autowired
    private SensorRepositorio sensorRepositorio;

    @Autowired
    private CalibrationCorrectionRepositorio calibrationCorrectionRepositorio;

    @Autowired
    private CalibrationAlertRepositorio calibrationAlertRepositorio;
    
    public List<Sensor> listarTodos() {
        return sensorRepositorio.findAll();
    }
    
    public List<Sensor> listarActivos() {
        return sensorRepositorio.findByActivoTrue();
    }
    
    public Optional<Sensor> obtenerPorId(Long id) {
        return sensorRepositorio.findById(id);
    }
    
    public Optional<Sensor> obtenerPorCodigo(String codigo) {
        return sensorRepositorio.findByCodigo(codigo);
    }
    
    public Sensor crear(Sensor sensor) {
        // Basic validation: required fields
        if (sensor == null) throw new IllegalArgumentException("Sensor is null");
        if (sensor.getCodigo() == null || sensor.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'codigo' es obligatorio para crear un sensor");
        }
        if (sensor.getTipoSonda() == null || sensor.getTipoSonda().trim().isEmpty()) {
            // provide a sensible default to avoid DB NOT NULL errors
            sensor.setTipoSonda("DESCONOCIDO");
        }
        if (sensor.getModelo() == null || sensor.getModelo().trim().isEmpty()) {
            sensor.setModelo("-");
        }

        // Ensure ubicacion satisfies DB NOT NULL constraint: default to modelo or codigo
        if (sensor.getUbicacion() == null || sensor.getUbicacion().trim().isEmpty()) {
            String fallback = sensor.getModelo() != null && !sensor.getModelo().trim().isEmpty()
                    ? sensor.getModelo()
                    : sensor.getCodigo();
            sensor.setUbicacion(fallback);
        }

        // Calcular próxima calibración si se proporciona última calibración
        if (sensor.getUltimaCalibracion() != null && sensor.getProximaCalibracion() == null) {
            sensor.setProximaCalibracion(
                sensor.getUltimaCalibracion().plusDays(sensor.getFrecuenciaCalibracionDias())
            );
        }

        // Ensure frecuencia has a sensible default
        if (sensor.getFrecuenciaCalibracionDias() == null || sensor.getFrecuenciaCalibracionDias() <= 0) {
            sensor.setFrecuenciaCalibracionDias(365);
        }

        return sensorRepositorio.save(sensor);
    }
    
    public Sensor actualizar(Long id, Sensor sensorActualizado) {
        return sensorRepositorio.findById(id)
            .map(sensor -> {
                // Update only when provided to avoid writing NULL for removed UI fields
                if (sensorActualizado.getCodigo() != null && !sensorActualizado.getCodigo().trim().isEmpty()) {
                    sensor.setCodigo(sensorActualizado.getCodigo());
                }
                if (sensorActualizado.getUbicacion() != null && !sensorActualizado.getUbicacion().trim().isEmpty()) {
                    sensor.setUbicacion(sensorActualizado.getUbicacion());
                }
                if (sensorActualizado.getTipoSonda() != null && !sensorActualizado.getTipoSonda().trim().isEmpty()) {
                    sensor.setTipoSonda(sensorActualizado.getTipoSonda());
                }
                if (sensorActualizado.getModelo() != null && !sensorActualizado.getModelo().trim().isEmpty()) {
                    sensor.setModelo(sensorActualizado.getModelo());
                }
                if (sensorActualizado.getFabricante() != null && !sensorActualizado.getFabricante().trim().isEmpty()) {
                    sensor.setFabricante(sensorActualizado.getFabricante());
                }
                if (sensorActualizado.getActivo() != null) {
                    sensor.setActivo(sensorActualizado.getActivo());
                }

                if (sensorActualizado.getUltimaCalibracion() != null) {
                    sensor.setUltimaCalibracion(sensorActualizado.getUltimaCalibracion());
                    // Set next calibration to one year from now to clear alerts until next year
                    sensor.setProximaCalibracion(LocalDate.now().plusYears(1));

                    // Clear pending calibration alerts for this sensor because it's been recalibrated
                    try {
                        var pending = calibrationAlertRepositorio.findBySensorIdAndStatus(sensor.getId(), "PENDING");
                        if (pending != null && !pending.isEmpty()) {
                            calibrationAlertRepositorio.deleteAll(pending);
                        }
                    } catch (Exception ignore) {}
                }
                if (sensorActualizado.getProximaCalibracion() != null) {
                    sensor.setProximaCalibracion(sensorActualizado.getProximaCalibracion());
                }
                if (sensorActualizado.getFrecuenciaCalibracionDias() != null && sensorActualizado.getFrecuenciaCalibracionDias() > 0) {
                    sensor.setFrecuenciaCalibracionDias(sensorActualizado.getFrecuenciaCalibracionDias());
                }
                if (sensorActualizado.getRangoMinimo() != null) {
                    sensor.setRangoMinimo(sensorActualizado.getRangoMinimo());
                }
                if (sensorActualizado.getRangoMaximo() != null) {
                    sensor.setRangoMaximo(sensorActualizado.getRangoMaximo());
                }
                if (sensorActualizado.getPrecision() != null) {
                    sensor.setPrecision(sensorActualizado.getPrecision());
                }
                if (sensorActualizado.getObservaciones() != null) {
                    sensor.setObservaciones(sensorActualizado.getObservaciones());
                }

                return sensorRepositorio.save(sensor);
            })
            .orElseThrow(() -> new RuntimeException("Sensor no encontrado con id: " + id));
    }
    
    public void eliminar(Long id) {
        // Delete legacy calibration correction records associated with this sensor
        try {
            var calList = calibrationCorrectionRepositorio.findBySensorId(id);
            if (calList != null && !calList.isEmpty()) {
                calibrationCorrectionRepositorio.deleteAll(calList);
            }
        } catch (Exception e) {
            // Log and continue; let deleteById surface any integrity errors if they remain
            // (we intentionally do not rethrow to provide a best-effort cleanup)
        }

        sensorRepositorio.deleteById(id);
    }
    
    public void desactivar(Long id) {
        sensorRepositorio.findById(id).ifPresent(sensor -> {
            sensor.setActivo(false);
            sensorRepositorio.save(sensor);
        });
    }
    
    public List<Sensor> buscarPorUbicacion(String ubicacion) {
        return sensorRepositorio.findByUbicacionContainingIgnoreCase(ubicacion);
    }
    
    public List<Sensor> buscarPorTipoSonda(String tipoSonda) {
        return sensorRepositorio.findByTipoSonda(tipoSonda);
    }
    
    /**
     * Obtiene sensores que necesitan calibración en los próximos N días
     */
    public List<Sensor> obtenerSensoresConCalibracionPendiente(int diasAnticipacion) {
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAnticipacion);
        return sensorRepositorio.findSensoresConCalibracionPendiente(fechaLimite);
    }
    
    /**
     * Obtiene sensores que nunca han sido calibrados
     */
    public List<Sensor> obtenerSensoresSinCalibrar() {
        return sensorRepositorio.findSensoresSinCalibrar();
    }
    
    /**
     * Registra una nueva calibración para un sensor
     */
    public Sensor registrarCalibracion(Long id, LocalDate fechaCalibracion) {
        return sensorRepositorio.findById(id)
            .map(sensor -> {
                sensor.setUltimaCalibracion(fechaCalibracion);
                sensor.setProximaCalibracion(
                    fechaCalibracion.plusDays(sensor.getFrecuenciaCalibracionDias())
                );
                return sensorRepositorio.save(sensor);
            })
            .orElseThrow(() -> new RuntimeException("Sensor no encontrado con id: " + id));
    }
}
