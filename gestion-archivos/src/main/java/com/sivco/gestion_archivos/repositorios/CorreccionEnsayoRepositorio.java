package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.CorreccionEnsayo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CorreccionEnsayoRepositorio extends JpaRepository<CorreccionEnsayo, Long> {
    List<CorreccionEnsayo> findByEnsayoId(Long ensayoId);
    List<CorreccionEnsayo> findByTipoArchivo(String tipoArchivo);
}
