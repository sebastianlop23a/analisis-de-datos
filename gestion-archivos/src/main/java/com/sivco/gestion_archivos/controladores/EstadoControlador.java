//Controlador simple para verificar el estado del sistema.
//Sirve para comprobar que el backend está funcionando correctamente y respondiendo.


package com.sivco.gestion_archivos.controladores;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estado")
public class EstadoControlador {

    @GetMapping("/ping")
    public String ping() {
        return "Servicio activo";
    }
}
