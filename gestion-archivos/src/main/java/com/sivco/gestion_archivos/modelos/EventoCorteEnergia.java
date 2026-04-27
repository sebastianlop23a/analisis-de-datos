package com.sivco.gestion_archivos.modelos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoCorteEnergia {
    
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private Double temperaturaAntes;
    private Double temperaturaMinima;
    private Double temperaturaDespues;
    private Long duracionMinutos;
    private String tipo; // "CORTE_ENERGIA" o "APERTURA_PUERTA"
}
