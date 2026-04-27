package com.sivco.gestion_archivos.modelos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Coeficientes de Corrección")
class CoeficientesCorreccionTest {
    
    @Test
    @DisplayName("Aplicar Corrección - Fórmula Completa")
    void testAplicarCorreccionFormula() {
        // Configurar coeficientes de ejemplo
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "sensor_1", 
            0.5,      // A
            1.02,     // B
            0.0001,   // C
            -0.00001  // D
        );
        
        double valorOriginal = 100.0;
        double valorCorregido = coeficientes.aplicarCorreccion(valorOriginal);
        
        // Cálculo manual: 0.5 + (1.02*100) + (0.0001*100²) + (-0.00001*100³)
        // = 0.5 + 102 + 1 + (-10) = 93.5
        double esperado = 0.5 + (1.02 * 100) + (0.0001 * 10000) + (-0.00001 * 1000000);
        
        assertEquals(esperado, valorCorregido, 0.001, 
                    "El valor corregido debería coincidir con el cálculo manual");
        
        System.out.println("Valor Original: " + valorOriginal);
        System.out.println("Valor Corregido: " + valorCorregido);
        System.out.println("Diferencia: " + (valorCorregido - valorOriginal));
    }
    
    @Test
    @DisplayName("Aplicar Corrección - Sin cambio (A=0, B=0, C=0, D=0)")
    void testAplicarCorreccionLineal() {
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "sensor_1", 
            0.0,   // A
            0.0,   // B
            0.0,   // C
            0.0    // D
        );
        
        double valorOriginal = 120.0;
        double valorCorregido = coeficientes.aplicarCorreccion(valorOriginal);
        
        assertEquals(0.0, valorCorregido, 0.001, 
                    "Sin corrección, el offset debe ser 0");
    }
    
    @Test
    @DisplayName("Aplicar Corrección - Con Offset (Solo A)")
    void testAplicarCorreccionConOffset() {
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "sensor_1", 
            5.0,   // A (offset)
            0.0,   // B
            0.0,   // C
            0.0    // D
        );
        
        double valorOriginal = 120.0;
        double valorCorregido = coeficientes.aplicarCorreccion(valorOriginal);
        
        assertEquals(5.0, valorCorregido, 0.001, 
                    "Con offset de 5, la corrección debe ser 5");
    }
    
    @Test
    @DisplayName("Aplicar Corrección - Escala Diferente (B=0.05)")
    void testAplicarCorreccionConEscala() {
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "sensor_1", 
            0.0,   // A
            0.05,  // B (pendiente de corrección)
            0.0,   // C
            0.0    // D
        );
        
        double valorOriginal = 100.0;
        double valorCorregido = coeficientes.aplicarCorreccion(valorOriginal);
        
        assertEquals(5.0, valorCorregido, 0.001, 
                    "Con B=0.05, la corrección debe ser 5 para un valor de 100");
    }
    
    @Test
    @DisplayName("Aplicar Corrección - Temperatura Típica de Esterilización")
    void testAplicarCorreccionTemperaturaEsterilizacion() {
        // Coeficientes típicos de calibración
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "sensor_1", 
            1.2,      // A
            0.998,    // B (ligera corrección de escala)
            0.00001,  // C (pequeña no linealidad)
            0.0       // D
        );
        
        double valorOriginal = 121.1; // Temperatura típica de autoclave
        double valorCorregido = coeficientes.aplicarCorreccion(valorOriginal);
        
        assertTrue(valorCorregido > 120.0 && valorCorregido < 125.0, 
                  "La temperatura corregida debería estar en rango razonable");
        
        System.out.println("Temperatura Original: " + valorOriginal + "°C");
        System.out.println("Temperatura Corregida: " + valorCorregido + "°C");
        System.out.println("Diferencia: " + (valorCorregido - valorOriginal) + "°C");
    }
    
    @Test
    @DisplayName("Aplicar Corrección - Valores Extremos")
    void testAplicarCorreccionValoresExtremos() {
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "sensor_1", 
            0.5, 1.02, 0.0001, -0.00001
        );
        
        // Probar con valor muy pequeño
        double valorPequeño = 10.0;
        double corregidoPequeño = coeficientes.aplicarCorreccion(valorPequeño);
        assertFalse(Double.isNaN(corregidoPequeño), "No debería producir NaN");
        assertFalse(Double.isInfinite(corregidoPequeño), "No debería producir Infinito");
        
        // Probar con valor grande
        double valorGrande = 200.0;
        double corregidoGrande = coeficientes.aplicarCorreccion(valorGrande);
        assertFalse(Double.isNaN(corregidoGrande), "No debería producir NaN");
        assertFalse(Double.isInfinite(corregidoGrande), "No debería producir Infinito");
        
        System.out.println("Valor Pequeño: " + valorPequeño + " -> " + corregidoPequeño);
        System.out.println("Valor Grande: " + valorGrande + " -> " + corregidoGrande);
    }
    
    @Test
    @DisplayName("Aplicar Corrección - Caso Real con Todos los Coeficientes")
    void testAplicarCorreccionCasoReal() {
        // Ejemplo con todos los coeficientes activos
        CoeficientesCorreccion coeficientes = new CoeficientesCorreccion(
            "PT100_01", 
            -2.5,        // A (offset negativo)
            1.015,       // B (ligero ajuste de ganancia)
            -0.000015,   // C (corrección cuadrática)
            0.0000001    // D (corrección cúbica)
        );
        
        double[] valoresPrueba = {100.0, 110.0, 120.0, 130.0, 140.0};
        
        System.out.println("\n=== Prueba de Corrección con Coeficientes Reales ===");
        System.out.println("Coeficientes: A=" + coeficientes.getCoeficienteA() + 
                          ", B=" + coeficientes.getCoeficienteB() +
                          ", C=" + coeficientes.getCoeficienteC() +
                          ", D=" + coeficientes.getCoeficienteD());
        System.out.println("\nValor Original -> Valor Corregido (Diferencia)");
        
        for (double valor : valoresPrueba) {
            double corregido = coeficientes.aplicarCorreccion(valor);
            double diferencia = corregido - valor;
            
            System.out.printf("%.2f°C -> %.4f°C (%.4f°C)%n", 
                             valor, corregido, diferencia);
            
            // Verificar que el resultado es válido
            assertFalse(Double.isNaN(corregido), "El resultado no debería ser NaN");
            assertFalse(Double.isInfinite(corregido), "El resultado no debería ser Infinito");
        }
    }
    
    @Test
    @DisplayName("Comparar Corrección vs Sin Corrección")
    void testCompararConYSinCorreccion() {
        CoeficientesCorreccion sinCorreccion = new CoeficientesCorreccion(
            "sensor_1", 0.0, 0.0, 0.0, 0.0
        );
        
        CoeficientesCorreccion conCorreccion = new CoeficientesCorreccion(
            "sensor_1", 2.0, 0.1, 0.0, 0.0
        );
        
        double valorOriginal = 121.0;
        
        double sinCorrec = sinCorreccion.aplicarCorreccion(valorOriginal);
        double conCorrec = conCorreccion.aplicarCorreccion(valorOriginal);
        
        assertEquals(0.0, sinCorrec, 0.001);
        assertNotEquals(0.0, conCorrec, "La corrección debería cambiar el valor");
        
        System.out.println("\n=== Comparación Con y Sin Corrección ===");
        System.out.println("Valor Original: " + valorOriginal);
        System.out.println("Sin Corrección: " + sinCorrec);
        System.out.println("Con Corrección: " + conCorrec);
        System.out.println("Diferencia: " + (conCorrec - sinCorrec));
    }
}
