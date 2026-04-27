package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del Servicio de Análisis")
class AnalisisServicioTest {
    
    private AnalisisServicio analisisServicio;
    
    @BeforeEach
    void setUp() {
        analisisServicio = new AnalisisServicio();
    }
    
    @Test
    @DisplayName("Cálculo de Media - Caso Normal")
    void testCalcularMedia() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 120.0, 130.0, 110.0});
        
        double media = analisisServicio.calcularMedia(datos);
        
        assertEquals(115.0, media, 0.01, "La media debería ser 115.0");
    }
    
    @Test
    @DisplayName("Cálculo de Desviación Estándar")
    void testCalcularDesviacionEstandar() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 120.0, 130.0, 110.0});
        
        double desviacion = analisisServicio.calcularDesviacionEstandar(datos);
        System.out.println("Desviación estándar calculada: " + desviacion);
        
        // Desviación estándar de [100, 120, 130, 110]
        assertTrue(desviacion > 10.0 && desviacion < 15.0, 
                   "La desviación estándar debería estar entre 10 y 15, pero fue: " + desviacion);
    }
    
    @Test
    @DisplayName("Cálculo de Máximo")
    void testCalcularMaximo() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 150.0, 130.0, 110.0});
        
        double maximo = analisisServicio.calcularMaximo(datos);
        
        assertEquals(150.0, maximo, 0.01, "El máximo debería ser 150.0");
    }
    
    @Test
    @DisplayName("Cálculo de Mínimo")
    void testCalcularMinimo() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 150.0, 130.0, 90.0});
        
        double minimo = analisisServicio.calcularMinimo(datos);
        
        assertEquals(90.0, minimo, 0.01, "El mínimo debería ser 90.0");
    }
    
    @Test
    @DisplayName("Cálculo de Rango")
    void testCalcularRango() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 150.0, 130.0, 90.0});
        
        double rango = analisisServicio.calcularRango(datos);
        
        assertEquals(60.0, rango, 0.01, "El rango debería ser 60.0 (150 - 90)");
    }
    
    @Test
    @DisplayName("Cálculo de Coeficiente de Variación")
    void testCalcularCoeficienteVariacion() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 120.0, 130.0, 110.0});
        
        double cv = analisisServicio.calcularCoeficienteVariacion(datos);
        
        assertTrue(cv > 0, "El coeficiente de variación debería ser positivo");
        assertTrue(cv < 100, "El coeficiente de variación debería ser menor a 100%");
    }
    
    @Test
    @DisplayName("Cálculo del Factor Histórico - Caso Normal")
    void testCalcularFactorHistorico() {
        // Crear datos con temperaturas y timestamps conocidos
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        LocalDateTime tiempo = LocalDateTime.now();
        
        // Temperaturas típicas de esterilización
        double[] temperaturas = {121.0, 122.0, 123.0, 124.0, 125.0};
        
        for (int i = 0; i < temperaturas.length; i++) {
            DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
            dato.setId((long) i);
            dato.setValor(temperaturas[i]);
            dato.setTimestamp(tiempo.plusMinutes(i));
            dato.setSensor("sensor_1");
            datos.add(dato);
        }
        
        double z = 14.0;
        double fh = analisisServicio.calcularFactorHistorico(datos, z);
        
        assertTrue(fh > 0, "El Factor Histórico debería ser positivo");
        assertTrue(fh < 100, "El Factor Histórico debería ser razonable (< 100)");
        
        System.out.println("Factor Histórico calculado: " + fh);
    }
    
    @Test
    @DisplayName("Factor Histórico - Caso con Lista Vacía")
    void testCalcularFactorHistoricoListaVacia() {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        
        double fh = analisisServicio.calcularFactorHistorico(datos, 14.0);
        
        assertEquals(0.0, fh, 0.001, "FH debería ser 0 con lista vacía");
    }
    
    @Test
    @DisplayName("Factor Histórico - Caso con un Solo Dato")
    void testCalcularFactorHistoricoUnDato() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{121.0});
        
        double fh = analisisServicio.calcularFactorHistorico(datos, 14.0);
        
        assertEquals(0.0, fh, 0.001, "FH debería ser 0 con un solo dato");
    }
    
    @Test
    @DisplayName("Factor Histórico - Diferentes Valores de Z")
    void testCalcularFactorHistoricoConDiferentesZ() {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        LocalDateTime tiempo = LocalDateTime.now();
        
        for (int i = 0; i < 5; i++) {
            DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
            dato.setId((long) i);
            dato.setValor(121.0 + i);
            dato.setTimestamp(tiempo.plusMinutes(i));
            dato.setSensor("sensor_1");
            datos.add(dato);
        }
        
        double fh_z10 = analisisServicio.calcularFactorHistorico(datos, 10.0);
        double fh_z14 = analisisServicio.calcularFactorHistorico(datos, 14.0);
        
        assertNotEquals(fh_z10, fh_z14, "FH debería ser diferente con distintos valores de Z");
        System.out.println("FH con Z=10: " + fh_z10);
        System.out.println("FH con Z=14: " + fh_z14);
    }
    
    @Test
    @DisplayName("Cálculo de Error Estándar")
    void testCalcularErrorEstandar() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 120.0, 130.0, 110.0});
        
        double errorEstandar = analisisServicio.calcularErrorEstandar(datos);
        
        assertTrue(errorEstandar > 0, "El error estándar debería ser positivo");
        System.out.println("Error estándar: " + errorEstandar);
    }
    
    @Test
    @DisplayName("Cálculo de Límites de Confianza")
    void testCalcularLimitesConfianza() {
        List<DatoEnsayoTemporal> datos = crearDatosPrueba(new double[]{100.0, 120.0, 130.0, 110.0});
        double valorT = 2.776; // t-student para 95% confianza con n=4
        
        double limiteInferior = analisisServicio.calcularLimiteConfianzaInferior(datos, valorT);
        double limiteSuperior = analisisServicio.calcularLimiteConfianzaSuperior(datos, valorT);
        double media = analisisServicio.calcularMedia(datos);
        
        assertTrue(limiteInferior < media, "El límite inferior debería ser menor que la media");
        assertTrue(limiteSuperior > media, "El límite superior debería ser mayor que la media");
        
        System.out.println("Media: " + media);
        System.out.println("Límite Inferior: " + limiteInferior);
        System.out.println("Límite Superior: " + limiteSuperior);
    }
    
    @Test
    @DisplayName("Porcentaje de Anormales")
    void testCalcularPorcentajeAnormales() {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        
        // Crear 8 datos normales
        for (int i = 0; i < 8; i++) {
            DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
            dato.setId((long) i);
            dato.setValor(120.0);
            dato.setAnormal(false);
            datos.add(dato);
        }
        
        // Crear 2 datos anormales
        for (int i = 8; i < 10; i++) {
            DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
            dato.setId((long) i);
            dato.setValor(200.0);
            dato.setAnormal(true);
            datos.add(dato);
        }
        
        double porcentaje = analisisServicio.calcularPorcentajeAnormales(datos);
        
        assertEquals(20.0, porcentaje, 0.01, "El porcentaje de anormales debería ser 20%");
    }
    
    // Método auxiliar para crear datos de prueba
    private List<DatoEnsayoTemporal> crearDatosPrueba(double[] valores) {
        List<DatoEnsayoTemporal> datos = new ArrayList<>();
        LocalDateTime tiempo = LocalDateTime.now();
        
        for (int i = 0; i < valores.length; i++) {
            DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
            dato.setId((long) i);
            dato.setValor(valores[i]);
            dato.setTimestamp(tiempo.plusSeconds(i));
            dato.setSensor("sensor_" + (i % 3 + 1));
            dato.setAnormal(false);
            datos.add(dato);
        }
        
        return datos;
    }
}
