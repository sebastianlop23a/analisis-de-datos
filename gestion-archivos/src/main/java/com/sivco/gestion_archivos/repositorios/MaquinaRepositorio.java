package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaquinaRepositorio extends JpaRepository<Maquina, Long> {
    
    List<Maquina> findByActiva(Boolean activa);
    
    List<Maquina> findByTipo(String tipo);
    
    Maquina findByNombre(String nombre);
}
