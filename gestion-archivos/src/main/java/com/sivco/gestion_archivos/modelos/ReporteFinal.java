package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteFinal {
    
    private Long ensayoId;
    private String nombreEnsayo;
    private String nombreMaquina;
    private String tipoMaquina;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String responsable;
    private String estado;
    
    // Estadísticas
    private Integer totalDatos;
    private Integer datosAnormales;
    private Double media;
    private Double desviacionEstandar;
    private Double maximo;
    private Double minimo;
    private Double rango;
    private Double coeficienteVariacion;
    private Double porcentajeAnormales;
    
    // Límites de la máquina
    private Double limiteInferior;
    private Double limiteSuperior;
    
    // Factor Histórico (FH)
    private Double factorHistorico;
    private Boolean calculaFH;
    private Double parametroZ;
    
    // Estadísticos avanzados
    private Double errorEstandar;
    private Double valorT = 2.0; // Default para 95% confianza
    private Double limiteConfianzaInferior;
    private Double limiteConfianzaSuperior;
    
    // Eventos detectados
    private java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> cortesEnergia;
    private java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> aperturasPuerta;
    
    // Observaciones
    private String observaciones;
    private String contenidoReporte;
}
