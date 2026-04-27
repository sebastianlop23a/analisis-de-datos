package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.Maquina;
import com.sivco.gestion_archivos.repositorios.MaquinaRepositorio;
import com.sivco.gestion_archivos.repositorios.EnsayoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
public class MaquinaServicio {
    
    private static final Logger logger = LoggerFactory.getLogger(MaquinaServicio.class);
    
    @Autowired
    private MaquinaRepositorio maquinaRepositorio;

    @Autowired
    private EnsayoRepositorio ensayoRepositorio;
    
    @Transactional
    public Maquina crearMaquina(Maquina maquina) {
        logger.info("Iniciando creación de máquina: {}", maquina.getNombre());
        logger.debug("Detalles de máquina: limiteSuperior={}, limiteInferior={}", 
            maquina.getLimiteSuperior(), maquina.getLimiteInferior());
        
        validarLimites(maquina);
        logger.info("Validación de límites pasada correctamente");
        
        try {
            Maquina maquinaGuardada = maquinaRepositorio.save(maquina);
            logger.info("Máquina creada exitosamente con ID: {}", maquinaGuardada.getId());
            return maquinaGuardada;
        } catch (Exception e) {
            logger.error("Error al guardar máquina: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear máquina: " + e.getMessage(), e);
        }
    }
    
    public Optional<Maquina> obtenerMaquina(Long id) {
        Objects.requireNonNull(id);
        return maquinaRepositorio.findById(id);
    }
    
    public List<Maquina> obtenerTodasLasMaquinas() {
        return maquinaRepositorio.findAll();
    }
    
    public List<Maquina> obtenerMaquinasActivas() {
        return maquinaRepositorio.findByActiva(true);
    }
    
    public List<Maquina> obtenerMaquinasPorTipo(String tipo) {
        return maquinaRepositorio.findByTipo(tipo);
    }
    
    @Transactional
    public Maquina actualizarMaquina(Long id, Maquina maquinaActualizada) {
        Objects.requireNonNull(id);
        return maquinaRepositorio.findById(id).map(maquina -> {
            validarLimites(maquinaActualizada);
            maquina.setNombre(maquinaActualizada.getNombre());
            maquina.setTipo(maquinaActualizada.getTipo());
            maquina.setDescripcion(maquinaActualizada.getDescripcion());
            maquina.setLimiteInferior(maquinaActualizada.getLimiteInferior());
            maquina.setLimiteSuperior(maquinaActualizada.getLimiteSuperior());
            maquina.setUnidadMedida(maquinaActualizada.getUnidadMedida());
            maquina.setActiva(maquinaActualizada.getActiva());
            maquina.setUbicacion(maquinaActualizada.getUbicacion());
            maquina.setModelo(maquinaActualizada.getModelo());
            maquina.setNumeroSerie(maquinaActualizada.getNumeroSerie());
            return maquinaRepositorio.save(maquina);
        }).orElseThrow(() -> new RuntimeException("Máquina no encontrada"));
    }
    
    @Transactional
    public void eliminarMaquina(Long id) {
        // Comprobar si hay ensayos asociados a la máquina antes de eliminar
        Objects.requireNonNull(id);
        var ensayos = ensayoRepositorio.findByMaquinaId(id);
        if (ensayos != null && !ensayos.isEmpty()) {
            throw new RuntimeException("No se puede eliminar la máquina: existen ensayos asociados. Elimine o finalice primero los ensayos.");
        }

        maquinaRepositorio.deleteById(id);
    }
    
    private void validarLimites(Maquina maquina) {
        if (maquina.getLimiteInferior() == null || maquina.getLimiteSuperior() == null) {
            throw new IllegalArgumentException("Los límites inferior y superior son requeridos");
        }
        if (maquina.getLimiteSuperior() <= maquina.getLimiteInferior()) {
            throw new IllegalArgumentException("El límite superior debe ser mayor al límite inferior");
        }
    }
    
    public boolean estaValorDentroDelRango(Long maquinaId, Double valor) {
        Objects.requireNonNull(maquinaId);
        return maquinaRepositorio.findById(maquinaId).map(maquina ->
            valor >= maquina.getLimiteInferior() && valor <= maquina.getLimiteSuperior()
        ).orElse(false);
    }
}
