package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reporte {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "ensayo_id", nullable = false)
    private Ensayo ensayo;
    
    @Column(nullable = false)
    private LocalDateTime fechaGeneracion;
    
    private String ruta;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoReporte tipo;
    
    @Column(columnDefinition = "LONGTEXT")
    private String contenido;
    
    private String generadoPor;
    
    private Integer numPaginas;
    
    private Double media;
    
    private Double desviacionEstandar;
    
    private Integer datosAnormales;
}
