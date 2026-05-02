package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Entidad para almacenar documentos Logtag (PDFs de datos para analizar)
 * diferentes a los PDFs de sensores normales.
 */
@Entity
@Table(name = "logtag_documentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogtagDocumento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "ensayo_id", nullable = true)
    private Ensayo ensayo;
    
    @NotBlank(message = "El nombre del archivo es requerido")
    @Column(nullable = false)
    private String nombreArchivo;
    
    @NotBlank(message = "El tipo de documento es requerido")
    @Column(nullable = false)
    private String tipoDocumento; // PDF, CSV, XLSX, etc.
    
    @NotBlank(message = "La categoría es requerida")
    @Column(nullable = false)
    private String categoria; // LOGTAG, SENSORES
    
    @Column(columnDefinition = "LONGTEXT")
    private String contenidoTexto; // Texto extraído del PDF
    
    @Column(nullable = false)
    private Long tamanioBytes;
    
    @Column(nullable = false)
    private LocalDateTime fechaSubida;
    
    private String descripcion;
    
    private String subidoPor;
    
    @Column(nullable = false)
    private Boolean procesado = false;
    
    @Column(columnDefinition = "LONGTEXT")
    private String notas;
    
    @Column(columnDefinition = "LONGTEXT")
    private String sensoresDetectados;
    
    // Identificador de lote para agrupar subidas relacionadas (opcional)
    private String lote;
}