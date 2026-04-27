package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.CalibrationCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationCorrectionRepositorio extends JpaRepository<CalibrationCorrection, Long> {
    Optional<CalibrationCorrection> findBySensorIdAndActiveTrue(Long sensorId);
    Optional<CalibrationCorrection> findBySensorCodigoAndActiveTrue(String codigo);
    List<CalibrationCorrection> findBySensorId(Long sensorId);
}
