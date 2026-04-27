package com.sivco.gestion_archivos.exceptiones;

public class EnsayoNoEncontradoException extends RuntimeException {
    public EnsayoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
    
    public EnsayoNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
