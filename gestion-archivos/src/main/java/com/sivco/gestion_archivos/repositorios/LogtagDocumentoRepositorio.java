package com.sivco.gestion_archivos.repositorios;

import com.sivco.gestion_archivos.modelos.LogtagDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogtagDocumentoRepositorio extends JpaRepository<LogtagDocumento, Long> {
    
    List<LogtagDocumento> findByEnsayoId(Long ensayoId);
    List<LogtagDocumento> findByCategoria(String categoria);
    List<LogtagDocumento> findByTipoDocumento(String tipoDocumento);
    List<LogtagDocumento> findByNombreArchivoContaining(String nombre);
    List<LogtagDocumento> findByProcesadoFalse();
    List<LogtagDocumento> findBySubidoPor(String subidoPor);
    long countByCategoria(String categoria);
    long countByEnsayoId(Long ensayoId);
    List<LogtagDocumento> findByEnsayoIdAndCategoria(Long ensayoId, String categoria);
    boolean existsByEnsayoIdAndNombreArchivo(Long ensayoId, String nombreArchivo);
}