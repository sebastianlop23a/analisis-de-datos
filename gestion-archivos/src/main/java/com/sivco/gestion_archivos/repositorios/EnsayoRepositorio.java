package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.EstadoEnsayo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EnsayoRepositorio extends JpaRepository<Ensayo, Long> {
    
    List<Ensayo> findByEstado(EstadoEnsayo estado);
    
    List<Ensayo> findByMaquinaId(Long maquinaId);
    
    List<Ensayo> findByResponsable(String responsable);
}
