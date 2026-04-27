package com.sivco.gestion_archivos.modelos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearEnsayoDTO {
    
    @NotBlank(message = "El nombre del ensayo es requerido")
    private String nombre;
    
    @NotNull(message = "La máquina es requerida")
    private Long maquinaId;
    
    private String descripcion;
    
    private String responsable;
    
    private String observaciones;
}
