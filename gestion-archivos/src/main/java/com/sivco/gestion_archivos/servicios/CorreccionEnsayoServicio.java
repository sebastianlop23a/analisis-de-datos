package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.CorreccionEnsayo;
import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.repositorios.CorreccionEnsayoRepositorio;
import com.sivco.gestion_archivos.repositorios.EnsayoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CorreccionEnsayoServicio {
    
    @Autowired
    private CorreccionEnsayoRepositorio correccionRepositorio;
    
    @Autowired
    private EnsayoRepositorio ensayoRepositorio;
    
    @Value("${app.correcciones.upload-dir:correcciones}")
    private String uploadDir;
    
    private static final String[] TIPOS_PERMITIDOS = {"XML", "PDF", "CSV", "EXCEL"};
    private static final long TAMANIO_MAXIMO = 50 * 1024 * 1024; // 50 MB
    
    /**
     * Subir archivo de correcciones para un ensayo
     */
    public CorreccionEnsayo subirCorreccion(Long ensayoId, MultipartFile archivo, String descripcion, String subidoPor) throws IOException {
        // Validar ensayo
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        
        // Validar archivo
        if (archivo.isEmpty()) {
            throw new RuntimeException("Archivo vacío");
        }
        
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new RuntimeException("Archivo demasiado grande (máximo 50 MB)");
        }
        
        // Detectar tipo de archivo
        String tipoArchivo = detectarTipo(archivo.getOriginalFilename());
        validarTipo(tipoArchivo);
        
        // Crear directorio si no existe
        Path dirPath = Paths.get(uploadDir, "ensayo_" + ensayoId);
        Files.createDirectories(dirPath);
        
        // Generar nombre único
        String nombreUnico = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
        Path rutaArchivo = dirPath.resolve(nombreUnico);
        
        // Guardar archivo
        Files.write(rutaArchivo, archivo.getBytes());
        
        // Crear entidad
        CorreccionEnsayo correccion = new CorreccionEnsayo();
        correccion.setEnsayo(ensayo);
        correccion.setNombreArchivo(archivo.getOriginalFilename());
        correccion.setTipoArchivo(tipoArchivo);
        correccion.setRutaArchivo(rutaArchivo.toString());
        correccion.setTamanioBytes(archivo.getSize());
        correccion.setFechaSubida(LocalDateTime.now());
        correccion.setDescripcion(descripcion);
        correccion.setSubidoPor(subidoPor);
        
        return correccionRepositorio.save(correccion);
    }
    
    /**
     * Obtener correcciones por ensayo
     */
    public List<CorreccionEnsayo> obtenerCorreccionesPorEnsayo(Long ensayoId) {
        return correccionRepositorio.findByEnsayoId(ensayoId);
    }
    
    /**
     * Obtener corrección por ID
     */
    public Optional<CorreccionEnsayo> obtenerCorreccion(Long id) {
        return correccionRepositorio.findById(id);
    }
    
    /**
     * Descargar archivo
     */
    public byte[] descargarArchivo(Long id) throws IOException {
        CorreccionEnsayo correccion = correccionRepositorio.findById(id)
            .orElseThrow(() -> new RuntimeException("Corrección no encontrada"));
        
        Path rutaArchivo = Paths.get(correccion.getRutaArchivo());
        if (!Files.exists(rutaArchivo)) {
            throw new RuntimeException("Archivo no encontrado en el sistema");
        }
        
        return Files.readAllBytes(rutaArchivo);
    }
    
    /**
     * Eliminar corrección
     */
    public void eliminarCorreccion(Long id) throws IOException {
        CorreccionEnsayo correccion = correccionRepositorio.findById(id)
            .orElseThrow(() -> new RuntimeException("Corrección no encontrada"));
        
        // Eliminar archivo del sistema
        Path rutaArchivo = Paths.get(correccion.getRutaArchivo());
        if (Files.exists(rutaArchivo)) {
            Files.delete(rutaArchivo);
        }
        
        // Eliminar registro de BD
        correccionRepositorio.deleteById(id);
    }
    
    /**
     * Obtener correcciones por tipo
     */
    public List<CorreccionEnsayo> obtenerCorreccionesPorTipo(String tipo) {
        return correccionRepositorio.findByTipoArchivo(tipo);
    }
    
    /**
     * Detectar tipo de archivo por extensión
     */
    private String detectarTipo(String nombreArchivo) {
        if (nombreArchivo == null) {
            throw new RuntimeException("Nombre de archivo inválido");
        }
        
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toUpperCase();
        
        if (extension.equals("XML")) return "XML";
        if (extension.equals("PDF")) return "PDF";
        if (extension.equals("CSV")) return "CSV";
        if (extension.equals("XLS") || extension.equals("XLSX")) return "EXCEL";
        
        throw new RuntimeException("Tipo de archivo no soportado: " + extension);
    }
    
    /**
     * Validar tipo permitido
     */
    private void validarTipo(String tipo) {
        for (String tipoPermitido : TIPOS_PERMITIDOS) {
            if (tipoPermitido.equals(tipo)) {
                return;
            }
        }
        throw new RuntimeException("Tipo no permitido: " + tipo);
    }
}
