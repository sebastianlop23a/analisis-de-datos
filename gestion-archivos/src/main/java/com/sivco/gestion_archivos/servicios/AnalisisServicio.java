package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnalisisServicio {
    
    public double calcularMedia(List<DatoEnsayoTemporal> datos) {
        if (datos.isEmpty()) return 0;
        return datos.stream()
            .mapToDouble(DatoEnsayoTemporal::getValor)
            .average()
            .orElse(0);
    }
    
    public double calcularDesviacionEstandar(List<DatoEnsayoTemporal> datos) {
        if (datos.isEmpty()) return 0;
        
        double media = calcularMedia(datos);
        double sumaCuadrados = datos.stream()
            .mapToDouble(d -> Math.pow(d.getValor() - media, 2))
            .sum();
        
        return Math.sqrt(sumaCuadrados / datos.size());
    }
    
    public double calcularMaximo(List<DatoEnsayoTemporal> datos) {
        return datos.stream()
            .mapToDouble(DatoEnsayoTemporal::getValor)
            .max()
            .orElse(0);
    }
    
    public double calcularMinimo(List<DatoEnsayoTemporal> datos) {
        return datos.stream()
            .mapToDouble(DatoEnsayoTemporal::getValor)
            .min()
            .orElse(0);
    }
    
    public int contarAnormales(List<DatoEnsayoTemporal> datos) {
        return (int) datos.stream()
            .filter(DatoEnsayoTemporal::getAnormal)
            .count();
    }
    
    public double calcularPorcentajeAnormales(List<DatoEnsayoTemporal> datos) {
        if (datos.isEmpty()) return 0;
        
        long anormales = datos.stream()
            .filter(DatoEnsayoTemporal::getAnormal)
            .count();
        
        return (anormales * 100.0) / datos.size();
    }
    
    public List<DatoEnsayoTemporal> obtenerAnormales(List<DatoEnsayoTemporal> datos) {
        return datos.stream()
            .filter(DatoEnsayoTemporal::getAnormal)
            .toList();
    }
    
    public boolean esDatosValido(List<DatoEnsayoTemporal> datos) {
        return !datos.isEmpty();
    }
    
    public double calcularRango(List<DatoEnsayoTemporal> datos) {
        if (datos.isEmpty()) return 0;
        return calcularMaximo(datos) - calcularMinimo(datos);
    }
    
    public double calcularCoeficienteVariacion(List<DatoEnsayoTemporal> datos) {
        double media = calcularMedia(datos);
        if (media == 0) return 0;
        
        double desviacion = calcularDesviacionEstandar(datos);
        return (desviacion / media) * 100;
    }
    
    /**
     * Calcula el Factor Histórico (FH) usando la fórmula:
     * FH = Σ(10^((Ti - 250)/z) * Δt)
     * 
     * @param datos Lista de datos temporales ordenados por timestamp
     * @param z Parámetro de temperatura (típicamente 14)
     * @return Factor Histórico calculado
     */
    public double calcularFactorHistorico(List<DatoEnsayoTemporal> datos, double z) {
        if (datos.isEmpty() || datos.size() < 2) {
            return 0.0;
        }
        
        // Ordenar datos por timestamp
        List<DatoEnsayoTemporal> datosOrdenados = datos.stream()
            .sorted((d1, d2) -> d1.getTimestamp().compareTo(d2.getTimestamp()))
            .toList();
        
        double fh = 0.0;
        
        // Calcular para cada intervalo
        for (int i = 1; i < datosOrdenados.size(); i++) {
            DatoEnsayoTemporal datoActual = datosOrdenados.get(i);
            DatoEnsayoTemporal datoAnterior = datosOrdenados.get(i - 1);
            
            // Ti = temperatura actual
            double ti = datoActual.getValor();
            
            // Δt = diferencia de tiempo en minutos
            long deltaTSegundos = java.time.Duration.between(
                datoAnterior.getTimestamp(),
                datoActual.getTimestamp()
            ).getSeconds();
            double deltaTMinutos = deltaTSegundos / 60.0;
            
            // FH += 10^((Ti - 250)/z) * Δt
            double exponente = (ti - 250.0) / z;
            double termino = Math.pow(10, exponente) * deltaTMinutos;
            fh += termino;
        }
        
        return fh;
    }
    
    /**
     * Calcula el cuartil especificado (Q1, Q2/mediana, Q3)
     * @param datos Lista de datos
     * @param percentil Percentil a calcular (25 para Q1, 50 para mediana, 75 para Q3)
     * @return Valor del cuartil
     */
    public double calcularCuartil(List<DatoEnsayoTemporal> datos, double percentil) {
        if (datos.isEmpty()) return 0;
        
        List<Double> valores = datos.stream()
            .map(DatoEnsayoTemporal::getValor)
            .sorted()
            .toList();
        
        int n = valores.size();
        double posicion = (percentil / 100.0) * (n - 1);
        int indiceInferior = (int) Math.floor(posicion);
        int indiceSuperior = (int) Math.ceil(posicion);
        
        if (indiceInferior == indiceSuperior) {
            return valores.get(indiceInferior);
        }
        
        double valorInferior = valores.get(indiceInferior);
        double valorSuperior = valores.get(indiceSuperior);
        double fraccion = posicion - indiceInferior;
        
        return valorInferior + (valorSuperior - valorInferior) * fraccion;
    }
    
    /**
     * Calcula Q1 (Percentil 25)
     */
    public double calcularQ1(List<DatoEnsayoTemporal> datos) {
        return calcularCuartil(datos, 25.0);
    }
    
    /**
     * Calcula la mediana (Percentil 50 / Q2)
     */
    public double calcularMediana(List<DatoEnsayoTemporal> datos) {
        return calcularCuartil(datos, 50.0);
    }
    
    /**
     * Calcula Q3 (Percentil 75)
     */
    public double calcularQ3(List<DatoEnsayoTemporal> datos) {
        return calcularCuartil(datos, 75.0);
    }
    
    /**
     * Calcula el error estándar de la media
     * Error = Desviación estándar / sqrt(n)
     */
    public double calcularErrorEstandar(List<DatoEnsayoTemporal> datos) {
        if (datos.isEmpty()) return 0;
        double desviacion = calcularDesviacionEstandar(datos);
        return desviacion / Math.sqrt(datos.size());
    }
    
    /**
     * Calcula el límite de confianza inferior
     * LCI = Media - (t * Error estándar)
     * @param valorT Valor de t (típicamente 2 para 95% de confianza)
     */
    public double calcularLimiteConfianzaInferior(List<DatoEnsayoTemporal> datos, double valorT) {
        double media = calcularMedia(datos);
        double error = calcularErrorEstandar(datos);
        return media - (valorT * error);
    }
    
    /**
     * Calcula el límite de confianza superior
     * LCS = Media + (t * Error estándar)
     * @param valorT Valor de t (típicamente 2 para 95% de confianza)
     */
    public double calcularLimiteConfianzaSuperior(List<DatoEnsayoTemporal> datos, double valorT) {
        double media = calcularMedia(datos);
        double error = calcularErrorEstandar(datos);
        return media + (valorT * error);
    }
    
    /**
     * Detecta cortes de energía basándose en caídas bruscas de temperatura
     * Un corte se identifica cuando:
     * 1. Hay una caída >= umbralCaida grados en un intervalo corto
     * 2. La temperatura se mantiene baja por al menos duracionMinima minutos
     * 3. Hay una recuperación gradual
     * 
     * @param datos Lista de datos ordenados por timestamp
     * @param umbralCaida Caída mínima para detectar corte (ej: 5°C)
     * @param duracionMinima Duración mínima del corte en minutos (ej: 5 min)
     * @return Lista de eventos de corte detectados
     */
    public java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> detectarCortesEnergia(
            List<DatoEnsayoTemporal> datos, double umbralCaida, long duracionMinima) {
        
        java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> eventos = new java.util.ArrayList<>();
        
        if (datos.size() < 3) return eventos;
        
        List<DatoEnsayoTemporal> datosOrdenados = datos.stream()
            .sorted((d1, d2) -> d1.getTimestamp().compareTo(d2.getTimestamp()))
            .toList();
        
        for (int i = 1; i < datosOrdenados.size() - 1; i++) {
            DatoEnsayoTemporal anterior = datosOrdenados.get(i - 1);
            DatoEnsayoTemporal actual = datosOrdenados.get(i);
            
            // Detectar caída brusca
            double caida = anterior.getValor() - actual.getValor();
            
            if (caida >= umbralCaida) {
                // Buscar el punto de recuperación
                double tempMinima = actual.getValor();
                int indiceMin = i;
                int indiceFin = i;
                boolean recuperacionCompleta = false;
                
                // Buscar el mínimo y cuando se recupera
                for (int j = i + 1; j < datosOrdenados.size(); j++) {
                    double tempActual = datosOrdenados.get(j).getValor();
                    
                    if (tempActual < tempMinima) {
                        tempMinima = tempActual;
                        indiceMin = j;
                    }
                    
                    // Recuperación completa: temperatura vuelve al 90% del valor original o más
                    if (tempActual >= (anterior.getValor() * 0.9)) {
                        indiceFin = j;
                        recuperacionCompleta = true;
                        break;
                    }
                    
                    // Recuperación parcial: si sube más del 50% de la caída (backup)
                    if (tempActual >= (anterior.getValor() - caida * 0.5)) {
                        indiceFin = j;
                    }
                    
                    // Límite de búsqueda: 2 horas
                    long minutosBusqueda = java.time.Duration.between(
                        actual.getTimestamp(), 
                        datosOrdenados.get(j).getTimestamp()
                    ).toMinutes();
                    
                    if (minutosBusqueda > 120) {
                        indiceFin = j;
                        break;
                    }
                }
                
                // Calcular duración del evento
                long duracion = java.time.Duration.between(
                    actual.getTimestamp(),
                    datosOrdenados.get(indiceFin).getTimestamp()
                ).toMinutes();
                
                // Solo registrar si cumple duración mínima
                if (duracion >= duracionMinima) {
                    com.sivco.gestion_archivos.modelos.EventoCorteEnergia evento = 
                        new com.sivco.gestion_archivos.modelos.EventoCorteEnergia();
                    evento.setInicio(actual.getTimestamp());
                    evento.setFin(datosOrdenados.get(indiceFin).getTimestamp());
                    evento.setTemperaturaAntes(anterior.getValor());
                    evento.setTemperaturaMinima(tempMinima);
                    evento.setTemperaturaDespues(datosOrdenados.get(indiceFin).getValor());
                    evento.setDuracionMinutos(duracion);
                    
                    // Clasificar tipo de evento según recuperación
                    if (recuperacionCompleta) {
                        evento.setTipo("CORTE_ENERGIA_CON_RECUPERACION");
                    } else {
                        evento.setTipo("CORTE_ENERGIA");
                    }
                    
                    eventos.add(evento);
                }
                
                // Saltar al final del evento para no detectar duplicados
                i = indiceFin;
            }
        }
        
        return eventos;
    }
    
    /**
     * Detecta aperturas de puerta basándose en picos breves de temperatura
     * Una apertura se identifica cuando:
     * 1. Hay un aumento >= umbralSubida grados
     * 2. La temperatura vuelve a la normalidad en menos de duracionMaxima minutos
     * 
     * @param datos Lista de datos ordenados por timestamp
     * @param umbralSubida Subida mínima para detectar apertura (ej: 3°C)
     * @param duracionMaxima Duración máxima del pico en minutos (ej: 10 min)
     * @return Lista de eventos de apertura detectados
     */
    public java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> detectarAperturasPuerta(
            List<DatoEnsayoTemporal> datos, double umbralSubida, long duracionMaxima) {
        
        java.util.List<com.sivco.gestion_archivos.modelos.EventoCorteEnergia> eventos = new java.util.ArrayList<>();
        
        if (datos.size() < 3) return eventos;
        
        List<DatoEnsayoTemporal> datosOrdenados = datos.stream()
            .sorted((d1, d2) -> d1.getTimestamp().compareTo(d2.getTimestamp()))
            .toList();
        
        for (int i = 1; i < datosOrdenados.size() - 1; i++) {
            DatoEnsayoTemporal anterior = datosOrdenados.get(i - 1);
            DatoEnsayoTemporal actual = datosOrdenados.get(i);
            
            // Detectar subida brusca
            double subida = actual.getValor() - anterior.getValor();
            
            if (subida >= umbralSubida) {
                // Buscar el punto máximo y recuperación
                double tempMaxima = actual.getValor();
                int indiceMax = i;
                int indiceFin = i;
                
                for (int j = i + 1; j < datosOrdenados.size(); j++) {
                    double tempActual = datosOrdenados.get(j).getValor();
                    
                    if (tempActual > tempMaxima) {
                        tempMaxima = tempActual;
                        indiceMax = j;
                    }
                    
                    // Si baja cerca del valor inicial, considerar que se cerró la puerta
                    if (tempActual <= (anterior.getValor() + subida * 0.3)) {
                        indiceFin = j;
                        break;
                    }
                    
                    // Límite de búsqueda
                    long minutosBusqueda = java.time.Duration.between(
                        actual.getTimestamp(), 
                        datosOrdenados.get(j).getTimestamp()
                    ).toMinutes();
                    
                    if (minutosBusqueda > duracionMaxima) {
                        indiceFin = j;
                        break;
                    }
                }
                
                long duracion = java.time.Duration.between(
                    actual.getTimestamp(),
                    datosOrdenados.get(indiceFin).getTimestamp()
                ).toMinutes();
                
                // Solo registrar si es un pico breve (apertura de puerta)
                if (duracion <= duracionMaxima) {
                    com.sivco.gestion_archivos.modelos.EventoCorteEnergia evento = 
                        new com.sivco.gestion_archivos.modelos.EventoCorteEnergia();
                    evento.setInicio(actual.getTimestamp());
                    evento.setFin(datosOrdenados.get(indiceFin).getTimestamp());
                    evento.setTemperaturaAntes(anterior.getValor());
                    evento.setTemperaturaMinima(tempMaxima); // Guardamos el máximo aquí
                    evento.setTemperaturaDespues(datosOrdenados.get(indiceFin).getValor());
                    evento.setDuracionMinutos(duracion);
                    evento.setTipo("APERTURA_PUERTA");
                    
                    eventos.add(evento);
                }
                
                i = indiceFin;
            }
        }
        
        return eventos;
    }
}
