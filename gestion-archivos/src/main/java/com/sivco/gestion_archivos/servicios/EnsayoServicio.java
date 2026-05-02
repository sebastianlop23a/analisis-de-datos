package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.*;
import com.sivco.gestion_archivos.repositorios.DatoEnsayoTemporalRepositorio;
import com.sivco.gestion_archivos.repositorios.EnsayoRepositorio;
import com.sivco.gestion_archivos.repositorios.MaquinaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EnsayoServicio {

    private static final int BATCH_SAVE_SIZE = 500;
    
    @Autowired
    private EnsayoRepositorio ensayoRepositorio;
    
    @Autowired
    private MaquinaRepositorio maquinaRepositorio;
    
    @Autowired
    private MaquinaServicio maquinaServicio;
    
    @Autowired
    private AnalisisServicio analisisServicio;
    
    @Autowired
    private DatoEnsayoTemporalRepositorio datoTemporalRepositorio;
    
    // Almacenamiento temporal en memoria para los datos durante el ensayo
    private Map<Long, List<DatoEnsayoTemporal>> datosTemporalesPorEnsayo = new ConcurrentHashMap<>();
    
    private Map<Long, Long> contadorSecuencia = new ConcurrentHashMap<>();
    
    public Ensayo crearEnsayo(CrearEnsayoDTO dto) {
        // Log para debugging
        System.out.println("=== CREAR ENSAYO ===");
        System.out.println("Nombre recibido: '" + dto.getNombre() + "'");
        System.out.println("Máquina ID: " + dto.getMaquinaId());
        
        // Validar que el nombre no esté vacío
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new RuntimeException("El nombre del ensayo no puede estar vacío");
        }
        
        // Obtener la máquina del repositorio
        Maquina maquina = maquinaRepositorio.findById(dto.getMaquinaId())
            .orElseThrow(() -> new RuntimeException("Máquina no encontrada"));
        
        // Crear el ensayo
        Ensayo ensayo = new Ensayo();
        ensayo.setNombre(dto.getNombre().trim());
        ensayo.setMaquina(maquina);
        ensayo.setDescripcion(dto.getDescripcion());
        ensayo.setResponsable(dto.getResponsable());
        ensayo.setObservaciones(dto.getObservaciones());
        ensayo.setFechaInicio(LocalDateTime.now());
        ensayo.setEstado(EstadoEnsayo.EN_PROGRESO);
        
        Ensayo ensayoGuardado = ensayoRepositorio.save(ensayo);
        
        System.out.println("Ensayo guardado con ID: " + ensayoGuardado.getId() + " y nombre: '" + ensayoGuardado.getNombre() + "'");
        
        // Inicializar lista de datos temporales
        datosTemporalesPorEnsayo.put(ensayoGuardado.getId(), new ArrayList<>());
        contadorSecuencia.put(ensayoGuardado.getId(), 0L);
        
        return ensayoGuardado;
    }
    
    public Optional<Ensayo> obtenerEnsayo(Long id) {
        return ensayoRepositorio.findById(id);
    }
    
    public List<Ensayo> obtenerTodosLosEnsayos() {
        return ensayoRepositorio.findAll();
    }
    
    public List<Ensayo> obtenerEnsayosPorEstado(EstadoEnsayo estado) {
        return ensayoRepositorio.findByEstado(estado);
    }
    
    public List<Ensayo> obtenerEnsayosPorMaquina(Long maquinaId) {
        return ensayoRepositorio.findByMaquinaId(maquinaId);
    }
    
    // Registrar dato durante el ensayo (se guarda en memoria)
    public void registrarDato(Long ensayoId, Double valor, String fuente) {
        if (!datosTemporalesPorEnsayo.containsKey(ensayoId)) {
            throw new RuntimeException("Ensayo no encontrado o ya finalizado");
        }
        
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        
        DatoEnsayoTemporal dato = new DatoEnsayoTemporal();
        dato.setEnsayoId(ensayoId);
        dato.setTimestamp(LocalDateTime.now());
        dato.setValor(valor);
        dato.setFuente(fuente);
        dato.setNumeroSecuencia(contadorSecuencia.get(ensayoId).intValue());
        
        // Validar si el valor está fuera del rango
        if (!maquinaServicio.estaValorDentroDelRango(ensayo.getMaquina().getId(), valor)) {
            dato.setAnormal(true);
        }
        
        datosTemporalesPorEnsayo.get(ensayoId).add(dato);
        contadorSecuencia.put(ensayoId, contadorSecuencia.get(ensayoId) + 1);
    }
    
    // Obtener datos temporales del ensayo en progreso
    public List<DatoEnsayoTemporal> obtenerDatosTemporales(Long ensayoId) {
        // Primero intentar obtener de memoria
        List<DatoEnsayoTemporal> datosMemoria = datosTemporalesPorEnsayo.get(ensayoId);
        if (datosMemoria != null && !datosMemoria.isEmpty()) {
            return datosMemoria;
        }
        
        // Si no hay en memoria, cargar desde la base de datos
        List<DatoEnsayoTemporal> datosBD = datoTemporalRepositorio.findByEnsayoId(ensayoId);
        if (!datosBD.isEmpty()) {
            // Cargar en memoria para futuras consultas
            datosTemporalesPorEnsayo.put(ensayoId, new ArrayList<>(datosBD));
            return datosBD;
        }
        
        return new ArrayList<>();
    }
    
    // Agregar datos temporales desde CSV (crea el mapa si no existe)
    public void agregarDatoTemporalCSV(Long ensayoId, DatoEnsayoTemporal dato) {
        // Crear lista si no existe
        if (!datosTemporalesPorEnsayo.containsKey(ensayoId)) {
            datosTemporalesPorEnsayo.put(ensayoId, new ArrayList<>());
            contadorSecuencia.put(ensayoId, 0L);
        }
        
        // Agregar el dato
        if (dato.getNumeroSecuencia() == null) {
            dato.setNumeroSecuencia((int) contadorSecuencia.get(ensayoId).longValue());
            contadorSecuencia.put(ensayoId, contadorSecuencia.get(ensayoId) + 1);
        }
        
        datosTemporalesPorEnsayo.get(ensayoId).add(dato);
        
        // Guardar en la base de datos para persistencia
        try {
            datoTemporalRepositorio.save(dato);
        } catch (Exception e) {
            System.err.println("Error guardando dato temporal en BD: " + e.getMessage());
        }
    }

    /**
     * Desplaza todos los timestamps de la serie temporal del ensayo por un offset en segundos.
     * Actualiza tanto la lista en memoria como persiste los cambios en la base de datos.
     * @param ensayoId id del ensayo
     * @param offsetSeconds segundos a desplazar (puede ser negativo)
     * @return cantidad de puntos afectados
     */
    public int desplazarSerieTemporal(Long ensayoId, long offsetSeconds) {
        List<DatoEnsayoTemporal> datos = obtenerDatosTemporales(ensayoId);
        if (datos == null || datos.isEmpty()) return 0;

        synchronized (datos) {
            for (DatoEnsayoTemporal d : datos) {
                try {
                    if (d.getTimestamp() != null) {
                        d.setTimestamp(d.getTimestamp().plusSeconds(offsetSeconds));
                    }
                } catch (Exception ex) {
                    System.err.println("Error desplazando timestamp para dato id=" + d.getId() + ": " + ex.getMessage());
                }
            }
        }

        try {
            // Persistir cambios en la BD; saveAllAndFlush asegura escritura inmediata
            datoTemporalRepositorio.saveAllAndFlush(datos);
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudieron persistir todos los cambios de timestamps: " + e.getMessage());
        }

        return datos.size();
    }

    public void guardarDatosTemporalesBatch(Long ensayoId, List<DatoEnsayoTemporal> datos) {
        if (datos == null || datos.isEmpty()) {
            return;
        }

        if (!datosTemporalesPorEnsayo.containsKey(ensayoId)) {
            datosTemporalesPorEnsayo.put(ensayoId, new ArrayList<>());
            contadorSecuencia.put(ensayoId, 0L);
        }

        long secuencia = contadorSecuencia.get(ensayoId);
        for (DatoEnsayoTemporal dato : datos) {
            if (dato.getNumeroSecuencia() == null) {
                dato.setNumeroSecuencia((int) secuencia);
                secuencia++;
            }
        }
        contadorSecuencia.put(ensayoId, secuencia);

        datosTemporalesPorEnsayo.get(ensayoId).addAll(datos);
        try {
            guardarDatosTemporalesBatchPorChunks(datos);
        } catch (Exception e) {
            System.err.println("Error guardando datos temporales por lote en BD: " + e.getMessage());
        }
    }

    private void guardarDatosTemporalesBatchPorChunks(List<DatoEnsayoTemporal> datos) {
        int total = datos.size();
        for (int start = 0; start < total; start += BATCH_SAVE_SIZE) {
            int end = Math.min(start + BATCH_SAVE_SIZE, total);
            List<DatoEnsayoTemporal> sublist = new ArrayList<>(datos.subList(start, end));
            datoTemporalRepositorio.saveAllAndFlush(sublist);
        }
    }
    
    // Finalizar ensayo, generar reporte y limpiar datos temporales
    public Ensayo finalizarEnsayo(Long ensayoId) {
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        
        List<DatoEnsayoTemporal> datos = datosTemporalesPorEnsayo.get(ensayoId);
        
        if (datos != null && !datos.isEmpty()) {
            // Calcular estadísticas
            double[] estadisticas = calcularEstadisticas(datos);
            
            ensayo.setTemperaturaPromedio(estadisticas[0]);
            ensayo.setTemperaturaMaxima(estadisticas[1]);
            ensayo.setTemperaturaMinima(estadisticas[2]);
            ensayo.setTotalRegistros(datos.size());
            ensayo.setRegistrosAnormales((int) datos.stream().filter(d -> d.getAnormal()).count());
            
            // Calcular Factor Histórico si la máquina lo requiere
            if (ensayo.getMaquina().getCalcularFH() != null && ensayo.getMaquina().getCalcularFH()) {
                double z = ensayo.getMaquina().getParametroZ() != null ? ensayo.getMaquina().getParametroZ() : 14.0;
                double fh = analisisServicio.calcularFactorHistorico(datos, z);
                ensayo.setFactorHistorico(fh);
            }
        }
        
        ensayo.setFechaFin(LocalDateTime.now());
        ensayo.setEstado(EstadoEnsayo.COMPLETADO);
        
        Ensayo ensayoActualizado = ensayoRepositorio.save(ensayo);
        // NOTE: Do not remove temporal data immediately. A scheduled cleaner will purge old data
        // after a retention period to avoid race conditions when generating reports.
        return ensayoActualizado;
    }
    
    private double[] calcularEstadisticas(List<DatoEnsayoTemporal> datos) {
        double[] resultado = new double[3]; // [promedio, máximo, mínimo]
        
        double suma = 0;
        double maximo = Double.NEGATIVE_INFINITY;
        double minimo = Double.POSITIVE_INFINITY;
        
        for (DatoEnsayoTemporal dato : datos) {
            suma += dato.getValor();
            maximo = Math.max(maximo, dato.getValor());
            minimo = Math.min(minimo, dato.getValor());
        }
        
        resultado[0] = suma / datos.size();
        resultado[1] = maximo;
        resultado[2] = minimo;
        
        return resultado;
    }
    
    public void pausarEnsayo(Long ensayoId) {
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        ensayo.setEstado(EstadoEnsayo.PAUSADO);
        ensayoRepositorio.save(ensayo);
    }
    
    public void reanudarEnsayo(Long ensayoId) {
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        if (ensayo.getEstado() == EstadoEnsayo.PAUSADO) {
            ensayo.setEstado(EstadoEnsayo.EN_PROGRESO);
            ensayoRepositorio.save(ensayo);
        }
    }
    
    public void cancelarEnsayo(Long ensayoId) {
        Ensayo ensayo = ensayoRepositorio.findById(ensayoId)
            .orElseThrow(() -> new RuntimeException("Ensayo no encontrado"));
        ensayo.setEstado(EstadoEnsayo.CANCELADO);
        ensayoRepositorio.save(ensayo);
        // NOTE: Do not remove temporal data immediately on cancel; allow scheduled cleanup.
    }

    // Retention (minutes) before purging temporal data after ensayo finalizado
    @org.springframework.beans.factory.annotation.Value("${app.temporal-data-retention-minutes:60}")
    private long retentionMinutes;

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${app.cleanup.interval.ms:600000}")
    public void purgeOldTemporalData() {
        try {
            java.time.Instant now = java.time.Instant.now();
            java.util.List<Long> toRemove = new java.util.ArrayList<>();
            for (Long ensayoId : datosTemporalesPorEnsayo.keySet()) {
                try {
                    java.util.Optional<Ensayo> maybe = ensayoRepositorio.findById(ensayoId);
                    if (maybe.isPresent()) {
                        Ensayo e = maybe.get();
                        if (e.getFechaFin() != null) {
                            java.time.Instant fin = e.getFechaFin().atZone(java.time.ZoneId.systemDefault()).toInstant();
                            long minutesSince = java.time.Duration.between(fin, now).toMinutes();
                            if (minutesSince >= retentionMinutes) {
                                toRemove.add(ensayoId);
                            }
                        }
                    } else {
                        // If ensayo record missing, remove temporal data older than retention too
                        // We don't have fechaFin; assume stale and remove after retentionMinutes since key existence is unknown,
                        // but to avoid accidental immediate deletion, only remove if retention has passed since JVM start
                        // (best-effort): skip immediate removal here.
                    }
                } catch (Exception ex) {
                    // log and continue
                    System.err.println("Error checking ensayo " + ensayoId + " for purge: " + ex.getMessage());
                }
            }

            for (Long id : toRemove) {
                datosTemporalesPorEnsayo.remove(id);
                contadorSecuencia.remove(id);
                System.out.println("Purgados datos temporales para ensayoId=" + id);
            }
        } catch (Exception e) {
            System.err.println("Error en purgeOldTemporalData: " + e.getMessage());
        }
    }
}
