package com.sivco.gestion_archivos.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calibration_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, SENT, ACK

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private String message;
}
