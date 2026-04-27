package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "maquinas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Maquina {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre de la máquina es requerido")
    @Column(nullable = false)
    private String nombre;
    
    @NotBlank(message = "El tipo de máquina es requerido")
    @Column(nullable = false)
    private String tipo;
    
    private String descripcion;
    
    @NotNull(message = "El límite inferior es requerido")
    @Column(nullable = false)
    private Double limiteInferior;
    
    @NotNull(message = "El límite superior es requerido")
    @Column(nullable = false)
    private Double limiteSuperior;
    
    private String unidadMedida;
    
    @Column(nullable = false)
    private Boolean activa = true;
    
    private String ubicacion;
    
    private String modelo;
    
    private String numeroSerie;
    
    // Factor Histórico (FH)
    @Column(nullable = false)
    private Boolean calcularFH = false;
    
    @Column(nullable = true)
    private Double parametroZ = 14.0; // Valor por defecto
}
