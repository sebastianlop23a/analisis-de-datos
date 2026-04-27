package com.sivco.gestion_archivos.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "datos_ensayo_temporal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatoEnsayoTemporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ensayo_id", nullable = false)
    private Long ensayoId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double valor;

    @Column(nullable = false)
    private Boolean anormal = false;

    @Column(length = 100)
    private String fuente;

    @Column(name = "numero_secuencia")
    private Integer numeroSecuencia;

    @Column(length = 50)
    private String sensor;

    @Column(name = "applied_calibration_id")
    private Long appliedCalibrationId;
}
