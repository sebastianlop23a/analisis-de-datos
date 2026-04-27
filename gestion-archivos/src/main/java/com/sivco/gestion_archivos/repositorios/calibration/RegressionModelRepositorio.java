package com.sivco.gestion_archivos.repositorios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.RegressionModel;
import com.sivco.gestion_archivos.modelos.calibration.RegressionModelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegressionModelRepositorio extends JpaRepository<RegressionModel, Long> {
    
    /**
     * Finds all regression models for a given calibration session
     */
    List<RegressionModel> findByCalibrationSessionId(Long calibrationSessionId);
    
    /**
     * Finds a specific regression model by session and type
     */
    Optional<RegressionModel> findByCalibrationSessionIdAndModelType(Long calibrationSessionId, RegressionModelType modelType);
}
