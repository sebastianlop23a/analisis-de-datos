package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepositorio extends JpaRepository<Sensor, Long> {
    
    Optional<Sensor> findByCodigo(String codigo);
    
    List<Sensor> findByActivoTrue();
    
    List<Sensor> findByUbicacionContainingIgnoreCase(String ubicacion);
    
    List<Sensor> findByTipoSonda(String tipoSonda);
    
    @Query("SELECT s FROM Sensor s WHERE s.activo = true AND s.proximaCalibracion <= :fechaLimite")
    List<Sensor> findSensoresConCalibracionPendiente(LocalDate fechaLimite);
    
    @Query("SELECT s FROM Sensor s WHERE s.activo = true AND s.ultimaCalibracion IS NULL")
    List<Sensor> findSensoresSinCalibrar();
}
