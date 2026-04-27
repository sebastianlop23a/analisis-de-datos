package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.modelos.Maquina;
import com.sivco.gestion_archivos.servicios.MaquinaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
@CrossOrigin(origins = "*")
public class MaquinaControlador {
    
    @Autowired
    private MaquinaServicio maquinaServicio;
    
    @PostMapping
    public ResponseEntity<?> crearMaquina(@Valid @RequestBody Maquina maquina) {
        try {
            return ResponseEntity.ok(maquinaServicio.crearMaquina(maquina));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Error al crear máquina: " + e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Maquina>> obtenerTodasLasMaquinas() {
        return ResponseEntity.ok(maquinaServicio.obtenerTodasLasMaquinas());
    }
    
    @GetMapping("/activas")
    public ResponseEntity<List<Maquina>> obtenerMaquinasActivas() {
        return ResponseEntity.ok(maquinaServicio.obtenerMaquinasActivas());
    }
    
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Maquina>> obtenerMaquinasPorTipo(@PathVariable String tipo) {
        return ResponseEntity.ok(maquinaServicio.obtenerMaquinasPorTipo(tipo));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Maquina> obtenerMaquina(@PathVariable Long id) {
        return maquinaServicio.obtenerMaquina(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/validar/{valor}")
    public ResponseEntity<Boolean> validarValor(
            @PathVariable Long id,
            @PathVariable Double valor) {
        return ResponseEntity.ok(maquinaServicio.estaValorDentroDelRango(id, valor));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Maquina> actualizarMaquina(
            @PathVariable Long id,
            @Valid @RequestBody Maquina maquina) {
        try {
            return ResponseEntity.ok(maquinaServicio.actualizarMaquina(id, maquina));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMaquina(@PathVariable Long id) {
        try {
            maquinaServicio.eliminarMaquina(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // Si hay ensayos asociados o cualquier otra razón, devolver 400 con mensaje
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
