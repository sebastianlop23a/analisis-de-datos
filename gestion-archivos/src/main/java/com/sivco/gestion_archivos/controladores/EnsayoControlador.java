package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.CrearEnsayoDTO;
import com.sivco.gestion_archivos.modelos.EstadoEnsayo;
import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import com.sivco.gestion_archivos.dto.DatoTemporalDTO;
import com.sivco.gestion_archivos.servicios.EnsayoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ensayos")
@CrossOrigin(origins = "*")
public class EnsayoControlador {
    
    @Autowired
    private EnsayoServicio ensayoServicio;
    
    @PostMapping
    public ResponseEntity<Ensayo> crearEnsayo(@Valid @RequestBody CrearEnsayoDTO dto) {
        return ResponseEntity.ok(ensayoServicio.crearEnsayo(dto));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Ensayo> obtenerEnsayo(@PathVariable Long id) {
        return ensayoServicio.obtenerEnsayo(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Ensayo>> obtenerTodosLosEnsayos() {
        return ResponseEntity.ok(ensayoServicio.obtenerTodosLosEnsayos());
    }
    
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Ensayo>> obtenerEnsayosPorEstado(@PathVariable EstadoEnsayo estado) {
        return ResponseEntity.ok(ensayoServicio.obtenerEnsayosPorEstado(estado));
    }
    
    @GetMapping("/maquina/{maquinaId}")
    public ResponseEntity<List<Ensayo>> obtenerEnsayosPorMaquina(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(ensayoServicio.obtenerEnsayosPorMaquina(maquinaId));
    }
    
    @PostMapping("/{ensayoId}/datos")
    public ResponseEntity<Map<String, String>> registrarDato(
            @PathVariable Long ensayoId,
            @RequestBody Map<String, Object> datos) {
        try {
            Double valor = Double.parseDouble(datos.get("valor").toString());
            String fuente = datos.getOrDefault("fuente", "SISTEMA").toString();
            
            ensayoServicio.registrarDato(ensayoId, valor, fuente);
            
            return ResponseEntity.ok(Map.of("mensaje", "Dato registrado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{ensayoId}/datos-temporales")
    public ResponseEntity<List<DatoTemporalDTO>> obtenerDatosTemporales(@PathVariable Long ensayoId) {
        List<DatoEnsayoTemporal> datos = ensayoServicio.obtenerDatosTemporales(ensayoId);
        
        // Convertir a DTO para evitar problemas de serialización
        List<DatoTemporalDTO> datosDTO = datos.stream()
            .map(d -> new DatoTemporalDTO(
                d.getTimestamp(),
                d.getValor(),
                d.getAnormal(),
                d.getSensor(),
                d.getNumeroSecuencia()
            ))
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(datosDTO);
    }
    
    @PostMapping("/{ensayoId}/finalizar")
    public ResponseEntity<Ensayo> finalizarEnsayo(@PathVariable Long ensayoId) {
        try {
            return ResponseEntity.ok(ensayoServicio.finalizarEnsayo(ensayoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{ensayoId}/pausar")
    public ResponseEntity<Map<String, String>> pausarEnsayo(@PathVariable Long ensayoId) {
        try {
            ensayoServicio.pausarEnsayo(ensayoId);
            return ResponseEntity.ok(Map.of("mensaje", "Ensayo pausado"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{ensayoId}/reanudar")
    public ResponseEntity<Map<String, String>> reanudarEnsayo(@PathVariable Long ensayoId) {
        try {
            ensayoServicio.reanudarEnsayo(ensayoId);
            return ResponseEntity.ok(Map.of("mensaje", "Ensayo reanudado"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{ensayoId}/cancelar")
    public ResponseEntity<Map<String, String>> cancelarEnsayo(@PathVariable Long ensayoId) {
        try {
            ensayoServicio.cancelarEnsayo(ensayoId);
            return ResponseEntity.ok(Map.of("mensaje", "Ensayo cancelado"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{ensayoId}/mover-serie")
    public ResponseEntity<Map<String, Object>> moverSerieTemporal(
            @PathVariable Long ensayoId,
            @RequestBody Map<String, Object> body) {
        try {
            long offsetSeconds = 0L;
            if (body.containsKey("offsetSeconds")) {
                Object o = body.get("offsetSeconds");
                offsetSeconds = Long.parseLong(o.toString());
            } else if (body.containsKey("offsetIso")) {
                String iso = body.get("offsetIso").toString();
                java.time.Duration d = java.time.Duration.parse(iso);
                offsetSeconds = d.getSeconds();
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Se requiere 'offsetSeconds' o 'offsetIso' en el body"));
            }

            int afectados = ensayoServicio.desplazarSerieTemporal(ensayoId, offsetSeconds);

            return ResponseEntity.ok(Map.of("afectados", afectados, "offsetSeconds", offsetSeconds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
