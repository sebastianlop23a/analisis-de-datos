//Controlador encargado de recibir los archivos Excel y PDF que el usuario sube al sistema.
//Se limita a recibir el archivo y enviar la solicitud al servicio correspondiente para ser procesado.




package com.sivco.gestion_archivos.controladores;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoControlador {

    @PostMapping("/cargar-excel")
    public String cargarExcel(@RequestParam("archivo") MultipartFile archivo) {
        return "Procesando Excel...";
    }

    @PostMapping("/cargar-pdf")
    public String cargarPdf(@RequestParam("archivo") MultipartFile archivo) {
        return "Procesando PDF...";
    }
}
