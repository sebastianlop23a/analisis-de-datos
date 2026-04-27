package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.CalibrationAlert;
import com.sivco.gestion_archivos.modelos.Sensor;
import com.sivco.gestion_archivos.repositorios.CalibrationAlertRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CalibrationAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CalibrationAlertScheduler.class);

    @Autowired
    private SensorServicio sensorServicio;

    @Autowired
    private CalibrationAlertRepositorio alertRepositorio;

    /**
     * Runs daily at 08:00 to generate alerts for sensors whose next calibration
     * is within the next 30 days.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void generateDailyAlerts() {
        int diasAnticipacion = 30;
        logger.info("Running daily calibration alert job (next {} days)", diasAnticipacion);

        List<Sensor> proximos = sensorServicio.obtenerSensoresConCalibracionPendiente(diasAnticipacion);
        for (Sensor s : proximos) {
            try {
                LocalDate due = s.getProximaCalibracion();
                if (due == null) continue;

                CalibrationAlert a = new CalibrationAlert();
                a.setSensorId(s.getId());
                a.setDueDate(due);
                a.setStatus("PENDING");
                a.setCreatedAt(LocalDateTime.now());
                a.setMessage("Calibración próxima para sensor " + s.getCodigo());
                alertRepositorio.save(a);
                logger.info("Created calibration alert for sensor {} due {}", s.getCodigo(), due);
            } catch (Exception e) {
                logger.warn("Failed to create alert for sensor {}: {}", s.getId(), e.getMessage());
            }
        }
    }
}
