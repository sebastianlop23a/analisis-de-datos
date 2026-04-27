package com.sivco.gestion_archivos.modelos;

public enum TipoReporte {
    PDF("PDF"),
    EXCEL("Excel"),
    HTML("HTML"),
    CSV("CSV");
    
    private final String descripcion;
    
    TipoReporte(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}
