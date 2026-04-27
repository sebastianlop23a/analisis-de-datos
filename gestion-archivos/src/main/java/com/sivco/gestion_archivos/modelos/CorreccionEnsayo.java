package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "correcciones_ensayo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorreccionEnsayo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "El ensayo es requerido")
    @ManyToOne
    @JoinColumn(name = "ensayo_id", nullable = false)
    private Ensayo ensayo;
    
    @NotBlank(message = "El nombre del archivo es requerido")
    @Column(nullable = false)
    private String nombreArchivo;
    
    @NotBlank(message = "El tipo de archivo es requerido")
    @Column(nullable = false)
    private String tipoArchivo; // XML, PDF, CSV, EXCEL
    
    @NotBlank(message = "La ruta del archivo es requerida")
    @Column(nullable = false)
    private String rutaArchivo;
    
    @Column(nullable = false)
    private Long tamanioBytes;
    
    @Column(nullable = false)
    private LocalDateTime fechaSubida;
    
    private String descripcion;
    
    private String subidoPor;
    
    @Column(columnDefinition = "LONGTEXT")
    private String notas;
}
