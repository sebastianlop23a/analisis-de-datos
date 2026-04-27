package com.sivco.gestion_archivos.modelos;

import lombok.Data;

@Data
public class CoeficientesCorreccion {
    private String sensor;
    private double coeficienteA;
    private double coeficienteB;
    private double coeficienteC;
    private double coeficienteD;
    private int indice;
    private double humedad;
    private double lineal;
    private double cubica;
    private double coLinHum;
    private java.util.List<Double> valores;

    // Constructor vacío para Lombok
    public CoeficientesCorreccion() {}

    // Constructor con parámetros principales
    public CoeficientesCorreccion(String sensor, double coeficienteA, double coeficienteB,
                                double coeficienteC, double coeficienteD) {
        this.sensor = sensor;
        this.coeficienteA = coeficienteA;
        this.coeficienteB = coeficienteB;
        this.coeficienteC = coeficienteC;
        this.coeficienteD = coeficienteD;
        this.valores = new java.util.ArrayList<>();
    }

    /**
     * Calcula la corrección como offset usando los coeficientes
     * Fórmula: correccion = A + B*x + C*x^2 + D*x^3
     * donde x es el valor original.
     * El valor final debe calcularse como valor_original + correccion.
     */
    public double aplicarCorreccion(double valorOriginal) {
        double x = valorOriginal;
        return coeficienteA + coeficienteB * x + coeficienteC * Math.pow(x, 2) + coeficienteD * Math.pow(x, 3);
    }

    // Añade un valor corregido a la colección interna (usado al parsear CSVs de corrección)
    public void addValor(double v) {
        if (this.valores == null) this.valores = new java.util.ArrayList<>();
        this.valores.add(v);
    }

    public java.util.List<Double> getValores() {
        if (this.valores == null) this.valores = new java.util.ArrayList<>();
        return this.valores;
    }
}