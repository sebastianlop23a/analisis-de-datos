package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "ensayos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ensayo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre del ensayo es requerido")
    @Column(nullable = false)
    private String nombre;
    
    @NotNull(message = "La máquina es requerida")
    @ManyToOne
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;
    
    @Column(nullable = false)
    private LocalDateTime fechaInicio;
    
    private LocalDateTime fechaFin;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEnsayo estado = EstadoEnsayo.EN_PROGRESO;
    
    private String descripcion;
    
    private String responsable;
    
    @Column(columnDefinition = "LONGTEXT")
    private String observaciones;
    
    private Double temperaturaPromedio;
    
    private Double temperaturaMaxima;
    
    private Double temperaturaMinima;
    
    private Integer totalRegistros;
    
    private Integer registrosAnormales;
    
    // Factor Histórico (si la máquina lo calcula)
    private Double factorHistorico;
}
