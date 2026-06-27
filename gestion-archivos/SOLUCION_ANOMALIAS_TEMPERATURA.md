# Solución: Detección de Anomalías en Temperatura - Logtags SIVCO-LOGGER

## Problema Original
"En la sección de temperatura de los logtahs también debe verse los que están fuera del límite como anormales, actualmente no es así"

## Causa del Problema

La detección de anomalías en datos de temperatura **SOLO funciona si se cumplen TODAS estas condiciones**:

1. ✅ El ensayo tiene una **MÁQUINA asignada**
2. ✅ Esa máquina tiene **`limiteInferior` configurado** (no null)
3. ✅ Esa máquina tiene **`limiteSuperior` configurado** (no null)

**Si falta alguna de estas condiciones**, los datos se marcarán como `anormal = false` (normales).

## Cambios Implementados

### 1. **Mejorado el Logging** (CargaDatosServicio.java)

Se agregó logging detallado para ayudar a identificar por qué NO se detectan anomalías:

```java
// Para datos SIVCO-LOGGER (línea 638-660)
// Para datos CSV (línea 813-828) 
// Para datos Excel (línea 1070-1086)

if (maquina != null && maquina.getLimiteInferior() != null && maquina.getLimiteSuperior() != null) {
    // ✅ Detectar anomalías
    esAnormal = valor < maquina.getLimiteInferior() || valor > maquina.getLimiteSuperior();
    if (esAnormal) {
        logger.debug("Valor ANORMAL detectado...");
    }
} else {
    // ❌ No se puede detectar
    if (maquina == null) {
        logger.warn("No se puede detectar anomalías: máquina del ensayo es null");
    } else {
        logger.warn("No se puede detectar anomalías: límites no configurados en máquina '{}'", 
            maquina.getNombre());
    }
}
```

### 2. **Detección Consistente en Todas las Fuentes**

Los siguientes tipos de archivos ahora tienen el mismo nivel de detección de anomalías:
- ✅ PDF SIVCO-LOGGER
- ✅ Archivos CSV
- ✅ Archivos Excel

### 3. **Mensajes de Diagnóstico**

Cuando cargues datos y no veas anomalías marcadas, **revisa los logs del servidor** para ver:

```
WARN  - No se puede detectar anomalías: la máquina del ensayo es null
WARN  - No se puede detectar anomalías: límites no configurados en máquina 'Máquina-1'
DEBUG - Valor ANORMAL detectado en sensor T1: 45.5 está fuera del rango [20, 30]
```

## Pasos para Usar

### Paso 1: Crear/Configurar la Máquina
1. Ve a **Configuración** → **Máquinas**
2. Crea o selecciona una máquina
3. **IMPORTANTE**: Establece los límites:
   - `Límite Inferior`: p.ej., `20.0`
   - `Límite Superior`: p.ej., `30.0`
4. Guarda los cambios

### Paso 2: Asignar Máquina al Ensayo
1. Crea un nuevo ensayo o edita uno existente
2. Asigna la máquina configurada
3. Guarda

### Paso 3: Cargar Datos
1. Ve a **Ensayos** → Selecciona el ensayo
2. En la sección "Subir Documento", carga el PDF SIVCO-LOGGER
3. El sistema automáticamente:
   - Extrae datos de temperatura
   - Compara cada valor contra los límites de la máquina
   - Marca como `anormal = true` si está fuera de rango

### Paso 4: Ver Anomalías
1. En la tabla **"Datos Registrados"**, verás una columna `Anormal`
2. Los valores fuera de límite mostrarán: **🔴 Sí**
3. Los valores dentro de límite mostrarán: **🟢 No**

### Paso 5: (Opcional) Revisar Logs
Si no ves anomalías marcadas, revisa los logs para diagnosticar:

```bash
# En Windows
tail -f target/logs/application.log | findstr "anormal\|límites\|máquina"

# En Linux/Mac
tail -f target/logs/application.log | grep -i "anormal\|límites\|máquina"
```

## Información Técnica

### Archivos Modificados
- `src/main/java/com/sivco/gestion_archivos/servicios/CargaDatosServicio.java`
  - Línea 638-660: SIVCO-LOGGER
  - Línea 813-828: CSV
  - Línea 1070-1086: Excel

### Flujo de Detección

```
1. Usuario carga PDF/CSV/Excel
   ↓
2. Sistema obtiene Máquina del ensayo
   ↓
3. Para cada dato de temperatura:
   a) ¿Máquina existe? ¿Tiene límites?
      → SÍ: comparar valor contra límites
      → NO: marcar como normal (false)
   b) Si valor < limiteInferior O valor > limiteSuperior
      → Marcar como anormal (true)
   c) Loguear resultado (DEBUG o WARN)
   ↓
4. Guardar dato en BD con flag 'anormal'
   ↓
5. Interfaz muestra resultado en tabla
```

### API Endpoints Relacionados

```
POST /api/carga/pdf/{ensayoId}
- Sube PDF SIVCO-LOGGER
- Retorna cantidad de registros cargados

GET /api/ensayo/{ensayoId}/datos
- Obtiene todos los datos temporales del ensayo
- Incluye campo 'anormal' para cada registro

GET /api/analisis/ensayo/{ensayoId}
- Retorna estadísticas incluyendo:
  - datosAnormales: cantidad de valores fuera de rango
  - porcentajeAnormales: % de datos anormales
```

## Solución de Problemas

| Problema | Causa | Solución |
|----------|-------|----------|
| No veo anomalías marcadas | Máquina es null | Asigna máquina al ensayo |
| No veo anomalías marcadas | Límites son null | Configura limiteInferior y limiteSuperior |
| Veo WARNING en logs | Máquina sin límites | Edita máquina y establece límites |
| Datos históricos no tienen anomalías | Fueron cargados antes | Recarga los datos después de configurar máquina |

## Notas Importantes

- ⚠️ **La detección de anomalías depende de límites configurados**. Sin límites, no hay detección.
- ✅ **Se conservan datos previos**: Si reconfiguras límites, solo nuevos datos usarán los nuevos límites.
- 🔄 **Todos los tipos de archivos**: PDF SIVCO-LOGGER, CSV y Excel usan la misma lógica.
- 📊 **Visualización mejorada**: Anomalías se colorean en rojo (🔴) en gráficos y tablas.

## Contacto / Soporte

Si los logs muestran `WARN` o `DEBUG` sobre anomalías, contacta al equipo de desarrollo con:
1. ID del ensayo
2. Nombre de la máquina asignada
3. Valores de limiteInferior y limiteSuperior
4. Archivo log completo (últimas 100 líneas)
