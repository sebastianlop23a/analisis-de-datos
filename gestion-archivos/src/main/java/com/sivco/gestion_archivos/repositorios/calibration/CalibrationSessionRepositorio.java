package com.sivco.gestion_archivos.repositorios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.CalibrationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationSessionRepositorio extends JpaRepository<CalibrationSession, Long> {
    
    /**
     * Encuentra la calibración activa para un dispositivo dado, cargando los modelos de regresión.
     */
    @Query("select s from CalibrationSession s left join fetch s.regressionModels where s.deviceId = :deviceId and s.isActive = true")
    Optional<CalibrationSession> findByDeviceIdAndIsActiveTrue(@Param("deviceId") Long deviceId);
    
    /**
     * Encuentra todas las calibraciones de un dispositivo (activa y archivadas)
     */
    List<CalibrationSession> findByDeviceIdOrderByCalibrationDateDesc(Long deviceId);
    
    /**
     * Encuentra todas las calibraciones archivadas (inactivas) para un dispositivo
     */
    List<CalibrationSession> findByDeviceIdAndIsActiveFalseOrderByCalibrationDateDesc(Long deviceId);
    
    /**
     * Comprueba si un dispositivo tiene una calibración activa
     */
    boolean existsByDeviceIdAndIsActiveTrue(Long deviceId);

    /**
     * Comprueba si se subió alguna sesión de calibración entre dos instantes
     * Útil para imponer una carga global anual
     */
    boolean existsByUploadedDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * Comprueba si se subió una sesión de calibración para un dispositivo específico entre dos instantes
     * Útil para imponer una carga anual por dispositivo
     */
    boolean existsByDeviceIdAndUploadedDateBetween(Long deviceId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * Comprueba si existe una sesión de calibración para un dispositivo en un año dado.
     * Conveniencia que usa la columna generada `uploaded_year`.
     */
    boolean existsByDeviceIdAndUploadedYear(Long deviceId, Integer uploadedYear);
}
