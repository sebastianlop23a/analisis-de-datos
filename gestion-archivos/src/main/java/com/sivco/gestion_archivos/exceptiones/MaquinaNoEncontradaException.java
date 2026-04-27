package com.sivco.gestion_archivos.exceptiones;

public class MaquinaNoEncontradaException extends RuntimeException {
    public MaquinaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
    
    public MaquinaNoEncontradaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
