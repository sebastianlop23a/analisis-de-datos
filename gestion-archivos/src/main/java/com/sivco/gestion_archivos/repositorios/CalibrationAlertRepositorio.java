package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.CalibrationAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalibrationAlertRepositorio extends JpaRepository<CalibrationAlert, Long> {
    List<CalibrationAlert> findByDueDateBeforeAndStatus(LocalDate dueDate, String status);
    List<CalibrationAlert> findBySensorIdAndStatus(Long sensorId, String status);
}
