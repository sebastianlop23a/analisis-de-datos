package com.sivco.gestion_archivos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatoTemporalDTO {
    private LocalDateTime timestamp;
    private Double valor;
    private Boolean anormal;
    private String sensor;
    private Integer numeroSecuencia;
}
