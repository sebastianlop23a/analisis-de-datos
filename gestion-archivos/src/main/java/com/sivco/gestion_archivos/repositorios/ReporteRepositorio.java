package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReporteRepositorio extends JpaRepository<Reporte, Long> {
    
    Optional<Reporte> findByEnsayoId(Long ensayoId);
}
