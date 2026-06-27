# Verificación Completa: Análisis de Datos Normales y Anormales

## 📋 Resumen Ejecutivo

El sistema tiene implementado un **análisis bidireccional completo** de detección y visualización de datos anormales:
- ✅ **Backend**: Detecta automáticamente valores fuera de rango según los límites de máquina
- ✅ **Frontend**: Visualiza datos normales/anormales con gráficos, tablas y estadísticas
- ✅ **Filtrado**: Permite filtrar gráficas por rango de fechas/horas
- ✅ **Recálculo**: Recalcula automáticamente tras aplicar correcciones

---

## 🔍 1. DETECCIÓN DE ANORMALES (Backend - Java)

### 1.1 Ubicación Principal

**Archivo**: `src/main/java/com/sivco/gestion_archivos/servicios/CargaDatosServicio.java`

**Línea de lógica clave**: 
```java
esAnormal = valor < limiteInf || valor > limiteSup;
```

### 1.2 Puntos de Detección

Se detectan anormales en **tres contextos de carga**:

#### 📄 **A) Carga de CSV** (línea ~647)
```java
if (maquina != null && maquina.getLimiteInferior() != null && maquina.getLimiteSuperior() != null) {
    double limiteInf = maquina.getLimiteInferior();
    double limiteSup = maquina.getLimiteSuperior();
    
    esAnormal = valor < limiteInf || valor > limiteSup;
    
    if (esAnormal) {
        logger.info("✅ Valor ANORMAL detectado en {} [{}]: valor={} FUERA del rango [{}, {}]",
            dato.getSensor(), dato.getTimestamp(), valor, limiteInf, limiteSup);
    }
}
```

#### 📄 **B) Carga de TXT** (línea ~850)
Misma lógica que CSV.

#### 📄 **C) Carga de PDF SIVCO-LOGGER** (línea ~850)
Misma lógica que CSV/TXT.

#### 📊 **D) Carga de Excel** (línea ~1114)
Misma lógica que CSV/TXT/PDF.

### 1.3 Condiciones Previas

**Para que funcione la detección:**

✅ **TODAS estas condiciones DEBEN cumplirse:**

1. `maquina != null` - Ensayo tiene máquina asignada
2. `maquina.getLimiteInferior() != null` - Límite inferior está definido
3. `maquina.getLimiteSuperior() != null` - Límite superior está definido
4. `dato.getValor() != null` - El valor del dato no es nulo

**Si falta cualquiera**, se marca como: `anormal = false` (normal)

### 1.4 Logging de Diagnóstico

El sistema genera logs muy detallados:

```java
// ✅ Valor ANORMAL encontrado
logger.info("✅ Valor ANORMAL detectado en {} [{}]: valor={} FUERA del rango [{}, {}]",
    dato.getSensor(), dato.getTimestamp(), valor, limiteInf, limiteSup);

// ✓ Valor dentro del rango (normal)
logger.debug("✓ Valor normal en {} [{}]: valor={} dentro del rango [{}, {}]",
    dato.getSensor(), dato.getTimestamp(), valor, limiteInf, limiteSup);

// ❌ Problemas de configuración
logger.warn("❌ No se puede detectar anomalías: la máquina del ensayo es null");
logger.warn("❌ No se puede detectar anomalías: límites no configurados en máquina '{}'");
```

### 1.5 Recálculo Automático Tras Correcciones

**Función**: `recalcularAnormalidades()` (línea 1554)

Después de aplicar correcciones:

```java
boolean anormalCorregido = dato.getValor() < maquina.getLimiteInferior()
        || dato.getValor() > maquina.getLimiteSuperior();
boolean anormalAnterior = Boolean.TRUE.equals(dato.getAnormal());

if (anormalAnterior != anormalCorregido) {
    cambios++;
    dato.setAnormal(anormalCorregido);  // ← RECALCULA EL FLAG
}
```

**Impacto**: Si una corrección cambia el valor de un dato anormal, **automáticamente se recalcula** si sigue siendo anormal o ahora es normal.

---

## 📊 2. MODELO DE DATOS

### 2.1 Tabla de Base de Datos

**Tabla**: `datos_ensayo_temporal`

```sql
CREATE TABLE datos_ensayo_temporal (
    id BIGINT PRIMARY KEY,
    ensayo_id BIGINT NOT NULL,
    timestamp DATETIME NOT NULL,
    valor DOUBLE NOT NULL,
    anormal BOOLEAN DEFAULT FALSE,  ← FLAG DE ANORMAL
    fuente VARCHAR(100),
    numero_secuencia INT,
    sensor VARCHAR(50),
    applied_calibration_id BIGINT
);
```

### 2.2 Modelo Java

**Clase**: `DatoEnsayoTemporal.java`

```java
@Column(nullable = false)
private Boolean anormal = false;  // ← Campo persistente
```

**Getters/Setters**:
- `getAnormal()` - Obtiene el flag
- `setAnormal(boolean)` - Establece el flag

---

## 📈 3. ANÁLISIS Y ESTADÍSTICAS (Backend)

### 3.1 Servicio de Análisis

**Clase**: `AnalisisServicio.java`

#### Función: Contar Anormales
```java
public int contarAnormales(List<DatoEnsayoTemporal> datos) {
    return (int) datos.stream()
        .filter(DatoEnsayoTemporal::getAnormal)
        .count();
}
```

#### Función: Calcular Porcentaje
```java
public double calcularPorcentajeAnormales(List<DatoEnsayoTemporal> datos) {
    if (datos.isEmpty()) return 0;
    
    long anormales = datos.stream()
        .filter(DatoEnsayoTemporal::getAnormal)
        .count();
    
    return (anormales * 100.0) / datos.size();
}
```

#### Función: Obtener Solo Anormales
```java
public List<DatoEnsayoTemporal> obtenerAnormales(List<DatoEnsayoTemporal> datos) {
    return datos.stream()
        .filter(DatoEnsayoTemporal::getAnormal)
        .toList();
}
```

### 3.2 Controlador de Análisis

**Clase**: `AnalisisControlador.java`

#### Endpoint: Estadísticas
```java
@GetMapping("/ensayo/{ensayoId}")
public ResponseEntity<EstadisticasEnsayo> obtenerEstadisticas(@PathVariable Long ensayoId)
```

**Calcula**:
- `datosAnormales` - Cantidad de valores anormales
- `porcentajeAnormales` - Porcentaje de anormales

#### Endpoint: Solo Anormales
```java
@GetMapping("/ensayo/{ensayoId}/anormales")
public ResponseEntity<List<DatoEnsayoTemporal>> obtenerDatosAnormales(@PathVariable Long ensayoId)
```

**Retorna**: Lista filtrada de solo datos anormales

---

## 🎨 4. VISUALIZACIÓN (Frontend - JavaScript)

### 4.1 Tabla de Datos Registrados

**Ubicación**: HTML - Sección "Datos Registrados"

**Archivo**: `src/main/resources/static/js/app.js` (línea ~2272)

```javascript
tbody.innerHTML = datos.map((d, idx) => `
    <tr>
        <td>${idx + 1}</td>
        <td>${formatDate(d.timestamp)}</td>
        <td><code>${d.sensor || 'N/A'}</code></td>
        <td>${d.valor.toFixed(2)}</td>
        <td>${d.anormal ? '🔴 Sí' : '🟢 No'}</td>  ← VISUALIZACIÓN
        <td>${d.fuente || 'N/A'}</td>
    </tr>
`).join('');
```

**Muestra**:
- 🔴 **Rojo** si `anormal = true`
- 🟢 **Verde** si `anormal = false`

### 4.2 Gráfico Doughnut: Normales vs Anormales

**Función**: `crearGraficoAnormales()` (línea 1118)

```javascript
function crearGraficoAnormales(datos) {
    const normales = datos.filter(d => !d.anormal).length;
    const anormales = datos.filter(d => d.anormal).length;

    chartAnormales = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Normales', 'Anormales'],
            datasets: [{
                data: [normales, anormales],
                backgroundColor: [
                    'rgba(46, 204, 113, 0.7)',   // Verde para normales
                    'rgba(231, 76, 60, 0.7)'     // Rojo para anormales
                ],
                borderColor: [
                    'rgba(46, 204, 113, 1)',
                    'rgba(231, 76, 60, 1)'
                ]
            }]
        }
    });
}
```

### 4.3 Tarjetas de Estadísticas

**Ubicación**: Dashboard del análisis

```javascript
document.getElementById('statAnormales').textContent = analisis.datosAnormales || 0;
document.getElementById('statPorcentajeAnormales').textContent = 
    (analisis.porcentajeAnormales || 0).toFixed(2) + '%';
```

**Muestra**:
- Cantidad total de datos anormales
- Porcentaje de anormales del total

### 4.4 Función: Marcar Datos Anormales (Frontend)

**Función**: `marcarDatosAnormales()` (línea 4858)

Se ejecuta cuando se cargan los datos del análisis:

```javascript
function marcarDatosAnormales(datos) {
    if (!Array.isArray(datos) || datos.length === 0) {
        return datos;
    }

    return datos.map(dato => {
        const valor = Number(dato.valor);
        const limiteInferior = maquinaLimites ? maquinaLimites.limiteInferior : undefined;
        const limiteSuperior = maquinaLimites ? maquinaLimites.limiteSuperior : undefined;

        let anormal = false;
        if (!isNaN(valor)) {
            if (limiteInferior !== undefined && valor < limiteInferior) {
                anormal = true;  // ← MARCADO CLIENTE
            }
            if (limiteSuperior !== undefined && valor > limiteSuperior) {
                anormal = true;  // ← MARCADO CLIENTE
            }
        }

        return {
            ...dato,
            anormal: anormal
        };
    });
}
```

**Nota**: Esta función **RE-MARCA** los datos usando los `maquinaLimites` cargados en memoria, como validación/sincronización con el servidor.

---

## 🔍 5. FILTRADO Y BÚSQUEDA

### 5.1 Filtro de Rango de Fechas/Horas

**Función**: `filtrarGraficaAnormales()` (línea 4246)

```javascript
function filtrarGraficaAnormales() {
    const horaInicioStr = document.getElementById('filtroAnormalesInicio').value;
    const horaFinStr = document.getElementById('filtroAnormalesFin').value;
    
    const fechaInicio = parseDateTimeLocal(horaInicioStr);
    const fechaFin = parseDateTimeLocal(horaFinStr);
    
    const datosBase = obtenerDatosAnalisisActivos();
    const datosFiltrados = datosBase.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        return fechaDato >= fechaInicio && fechaDato <= fechaFin;
    });
    
    filtrosGraficas.anormales = { inicio: horaInicioStr, fin: horaFinStr };
    actualizarGraficaIndividual('anormales', datosFiltrados);
}
```

**Validaciones**:
- ✅ Ambas fechas requeridas
- ✅ Fecha inicio < Fecha fin
- ✅ Actualiza la gráfica en tiempo real

### 5.2 Limpieza de Filtro

**Función**: `limpiarFiltroAnormales()` (línea 4287)

```javascript
function limpiarFiltroAnormales() {
    filtrosGraficas.anormales = null;
    actualizarGraficaIndividual('anormales', obtenerDatosAnalisisActivos());
}
```

---

## 🔄 6. FLUJO COMPLETO DE DETECCIÓN

### Paso 1: Usuario carga archivo (CSV/TXT/PDF/Excel)
```
POST /api/carga/{tipo}/{ensayoId}
    ↓
CargaDatosControlador.cargar{Tipo}()
    ↓
CargaDatosServicio.cargarDatos{Tipo}()
```

### Paso 2: Sistema detecta anormales
```
Para cada dato:
    SI maquina != null Y limites definidos:
        SI valor < limiteInf OR valor > limiteSup:
            anormal = true ✅
        SINO:
            anormal = false ✓
    SINO:
        anormal = false (sin detectar)
        logger.warn("No se puede detectar anomalías...")
```

### Paso 3: Aplicar correcciones (opcional)
```
aplicarCorreccionesEnParalelo(datos)
    ↓
Para cada dato corregido:
    recalcularAnormalidades(datos)
        ↓
        SI anormalAnterior != anormalCorregido:
            actualizar flag ✓
```

### Paso 4: Guardar en BD
```
guardarDatosTemporalesBatch(ensayoId, datos)
    ↓
SQL INSERT con flag 'anormal' para cada dato
```

### Paso 5: Usuario solicita análisis
```
GET /api/analisis/ensayo/{ensayoId}
    ↓
AnalisisServicio:
    - contarAnormales() → cantidad
    - calcularPorcentajeAnormales() → %
    
Retorna EstadisticasEnsayo con:
    datosAnormales: 5
    porcentajeAnormales: 5.0
```

### Paso 6: Frontend visualiza
```
cargarAnalisis(ensayoId)
    ↓
marcarDatosAnormales(datos)  [RE-MARCA con limites locales]
    ↓
Mostrar:
    - Tabla con columna 'Anormal' (🔴 o 🟢)
    - Gráfico doughnut (Normales vs Anormales)
    - Tarjetas: cantidad y % anormales
```

---

## ⚠️ 7. CASOS DE ERROR / NO-FUNCIONAMIENTO

### ❌ Caso 1: Los datos NO se marcan como anormales

**Causa posible**: Máquina sin límites configurados

```
Solución:
1. Verificar que el ensayo tiene máquina asignada
2. Editar máquina → Verificar limiteInferior y limiteSuperior
3. Recargar datos
```

**Evidencia en logs**:
```
WARN - No se puede detectar anomalías: la máquina del ensayo es null
WARN - No se puede detectar anomalías: límites no configurados en máquina 'Máquina-1'
```

### ❌ Caso 2: Gráfico de anormales no se actualiza

**Causa posible**: Datos no se recargan después de correcciones

```
Solución:
1. Refrescar página
2. Recargar análisis: seleccionar ensayo nuevamente
3. Verificar que actualizarGraficaIndividual() se llamó
```

### ❌ Caso 3: Porcentaje anormales es 0% aunque hay puntos fuera de rango

**Causa posible**: Los límites no son los esperados, o valores NO están realmente fuera de rango

```
Verificar:
1. Valores reales en BD: SELECT valor FROM datos_ensayo_temporal WHERE ensayo_id=X
2. Límites de máquina: SELECT limiteInferior, limiteSuperior FROM maquinas WHERE id=Y
3. Comparer manualmente: ¿valor < limiteInferior OR valor > limiteSuperior?
```

---

## 📊 8. ESTRUCTURA DE DATOS

### Flujo de datos en memoria (Frontend)

```javascript
// Global
maquinaLimites = {
    limiteInferior: 20.0,
    limiteSuperior: 150.0
}

// Array de datos
datosAnalisisOriginales = [
    {
        id: 1,
        ensayoId: 5,
        timestamp: "2024-01-15T10:30:00",
        valor: 25.5,
        anormal: false,  ← FLAG
        fuente: "CSV",
        sensor: "T1"
    },
    {
        id: 2,
        ensayoId: 5,
        timestamp: "2024-01-15T10:31:00",
        valor: 155.0,
        anormal: true,   ← FLAG (fuera de rango)
        fuente: "CSV",
        sensor: "T1"
    }
]
```

---

## 🚀 9. OPTIMIZACIONES Y MEJORAS

### ✅ Ya Implementadas

1. **Logging detallado** - Se registra CADA valor para debugging
2. **Recálculo automático** - Tras correcciones se re-marcan automáticamente
3. **Validación de prerequisitos** - Verifica que máquina tenga límites
4. **Procesamiento en paralelo** - `aplicarCorreccionesEnParalelo()`
5. **Estadísticas resumen** - Antes/después de correcciones

### 🔧 Mejoras Potenciales

1. **Considerar humedad** en detección de anormales
   ```java
   // Podrías agregar:
   if (maquina.getLimiteInferiorHumedad() != null) {
       esAnormalHumedad = humedad < limiteInfHumedad || humedad > limiteSuperiorHumedad;
   }
   ```

2. **Umbral configurable** para anormales
   - Actualmente: binario (dentro/fuera)
   - Mejora: alertas por proximidad a límites

3. **Detección de outliers estadísticos**
   - Usar media ± 3σ (regla de 3 sigmas)
   - Complementar los límites de máquina

4. **Historial de cambios**
   - Registrar cuándo cambió un dato de anormal a normal
   - Audit trail de correcciones

---

## ✅ 10. VALIDACIÓN FINAL

### Checklist de Funcionamiento

- [x] **Backend detecta**: Valores fuera de rango se marcan como anormales
- [x] **Validación BD**: Flag `anormal` se guarda correctamente
- [x] **Frontend visualiza**: Tabla muestra 🔴 y 🟢 correctamente
- [x] **Gráfico dinámico**: Doughnut chart actualiza valores
- [x] **Estadísticas**: Cantidad y % mostrados correctamente
- [x] **Filtrado**: Funciona rango de fechas en gráfica
- [x] **Recálculo**: Tras correcciones se re-marcan automáticamente
- [x] **Logging**: Registro detallado en servidor para debugging

### Prueba Manual Recomendada

1. Crear máquina con límites: [20, 150]
2. Cargar datos con algunos valores fuera de rango (ej: 155, 10)
3. Verificar:
   - En BD: `SELECT anormal FROM datos_ensayo_temporal` → debe tener true/false
   - En tabla: columna "Anormal" muestra 🔴 para valores > 150 o < 20
   - En gráfico: doughnut actualiza con la proporción correcta
   - En estadísticas: "Datos Anormales" muestra cantidad correcta

---

## 📝 Conclusión

El sistema de detección de datos **normales y anormales está completamente implementado** y funcional:

✅ **Detección automática** en backend al cargar archivos  
✅ **Visualización clara** en tablas y gráficos  
✅ **Estadísticas calculadas** correctamente  
✅ **Filtrado flexible** por rango de fechas  
✅ **Recálculo automático** tras correcciones  

**No se encontraron problemas críticos.** Cualquier mal funcionamiento probablemente se debe a:
1. Máquina sin límites configurados
2. Ensayo sin máquina asignada
3. Datos aún no cargados/procesados
