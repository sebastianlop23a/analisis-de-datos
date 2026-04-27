package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasEnsayo {
    
    private Double media;
    private Double desviacionEstandar;
    private Double maximo;
    private Double minimo;
    private Double rango;
    private Double coeficienteVariacion;
    private Integer totalDatos;
    private Integer datosAnormales;
    private Double porcentajeAnormales;
    
    // Factor Histórico (FH)
    private Double factorHistorico;
    
    // Datos para Boxplot
    private Double q1;  // Cuartil 1 (percentil 25)
    private Double q3;  // Cuartil 3 (percentil 75)
    private Double mediana;  // Mediana (percentil 50)
    
    // Distancias para análisis del boxplot
    private Double mediaMinusQ1;  // Media - Q1
    private Double q3MinusMedia;  // Q3 - Media
    private Double maximoMinusQ3;  // Máximo - Q3
    private Double q1MinusMinimo;  // Q1 - Mínimo
}
