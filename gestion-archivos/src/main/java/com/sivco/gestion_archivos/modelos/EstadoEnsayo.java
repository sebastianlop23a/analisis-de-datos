package com.sivco.gestion_archivos.modelos;

public enum EstadoEnsayo {
    EN_PROGRESO("En Progreso"),
    COMPLETADO("Completado"),
    PAUSADO("Pausado"),
    CANCELADO("Cancelado"),
    REPORTE_GENERADO("Reporte Generado");
    
    private final String descripcion;
    
    EstadoEnsayo(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}
