package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatoEnsayoTemporalRepositorio extends JpaRepository<DatoEnsayoTemporal, Long> {
    
    /**
     * Busca todos los datos temporales de un ensayo específico
     */
    List<DatoEnsayoTemporal> findByEnsayoId(Long ensayoId);
    
    /**
     * Busca datos temporales por ensayo y sensor
     */
    List<DatoEnsayoTemporal> findByEnsayoIdAndSensor(Long ensayoId, String sensor);
    
    /**
     * Elimina todos los datos temporales de un ensayo
     */
    void deleteByEnsayoId(Long ensayoId);
}
