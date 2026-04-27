package com.sivco.gestion_archivos.repositorios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.CalibrationPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalibrationPointRepositorio extends JpaRepository<CalibrationPoint, Long> {
    
    /**
     * Finds all calibration points for a given calibration session
     */
    List<CalibrationPoint> findByCalibrationSessionIdOrderByPointOrderAsc(Long calibrationSessionId);
}
