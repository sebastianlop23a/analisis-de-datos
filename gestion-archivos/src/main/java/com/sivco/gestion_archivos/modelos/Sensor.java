package com.sivco.gestion_archivos.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "sensores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String codigo; // Identificador único del sensor
    
    @Column(nullable = false)
    private String ubicacion; // Ej: "Cámara 1 - Esquina superior izquierda"
    
    @Column(nullable = false)
    private String tipoSonda; // Ej: "PT100", "Termopar K", "RTD"
    
    @Column(nullable = false)
    private String modelo; // Modelo del sensor
    
    private String fabricante;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    // Información de calibración
    private LocalDate ultimaCalibracion;
    
    private LocalDate proximaCalibracion;
    
    @Column(nullable = false)
    private Integer frecuenciaCalibracionDias = 365; // Por defecto anual
    
    // Características técnicas
    private Double rangoMinimo; // °C
    
    private Double rangoMaximo; // °C
    
    @Column(name = "`precision`")
    private Double precision; // ±°C
    
    // Observaciones
    @Column(length = 500)
    private String observaciones;
    
    /**
     * Verifica si el sensor necesita calibración próximamente
     * @param diasAnticipacion Días de anticipación para la alerta
     * @return true si necesita calibración en los próximos N días
     */
    @Transient
    public boolean necesitaCalibracion(int diasAnticipacion) {
        if (proximaCalibracion == null) return false;
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAnticipacion);
        return proximaCalibracion.isBefore(fechaLimite) || proximaCalibracion.isEqual(fechaLimite);
    }
    
    /**
     * Calcula días restantes hasta la próxima calibración
     */
    @Transient
    public long diasHastaProximaCalibracion() {
        if (proximaCalibracion == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), proximaCalibracion);
    }
}
