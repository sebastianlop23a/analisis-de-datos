# Guide: Carga de Datos SIVCO-LOGGER desde PDF

## ¿Qué se cambió?

Se ha implementado un nuevo servicio especializado para extraer datos directamente desde archivos PDF del SIVCO-LOGGER, sin necesidad de convertirlos a CSV primero.

## Nuevos Componentes

### 1. **SivcoLoggerPdfService.java**
Servicio especializado que:
- Extrae el texto del PDF usando Apache PDFBox
- Parsea los metadatos (Logger SN, Begin Time, End Time, Number of Points)
- Extrae estadísticas (Min, Max, Mean) por sensor
- Extrae la tabla de datos de temperatura (SN, Time, T1-oC, T2-oC, ..., T8-oC)
- Convierte los datos en objetos `DatoEnsayoTemporal`

### 2. **CargaDatosServicio mejorado**
Se agregaron dos métodos nuevos:
- `cargarDatos()` - Método principal que detecta el tipo de archivo (CSV o PDF)
- `cargarDatosPdfSivcoLogger()` - Especializado para procesar PDFs SIVCO-LOGGER

### 3. **CargaDatosControlador actualizado**
El endpoint `POST /api/carga/pdf/{ensayoId}` ahora:
- Recibe archivos PDF SIVCO-LOGGER
- Extrae automáticamente todos los datos de temperatura
- Valida los valores contra los límites de la máquina
- Guarda los datos en la base de datos

## Cómo Usar

### Desde la API REST

```bash
# Cargar PDF SIVCO-LOGGER
curl -X POST \
  http://localhost:8080/api/carga/pdf/123 \
  -F "archivo=@logger_data.pdf"
```

Donde:
- `123` es el ID del ensayo
- `logger_data.pdf` es el archivo PDF SIVCO-LOGGER

### Respuesta exitosa

```json
{
  "mensaje": "Datos PDF SIVCO-LOGGER cargados correctamente",
  "totalRegistros": 1431,
  "tipoArchivo": "PDF_SIVCO_LOGGER"
}
```

## Formato esperado del PDF SIVCO-LOGGER

El PDF debe contener:

1. **Sección de metadatos:**
   ```
   LOGGER_SN: HS220HL047
   Begin Time: 2025-11-06 10:32:12
   End Time: 2025-11-07 12:09:12
   Number of Points: 1431
   ```

2. **Sección de estadísticas (opcional pero recomendada):**
   ```
   Min: 0.0(oC)/-21.8(oC)/-22.5(oC)/...
   Max: 0.0(oC)/13.9(oC)/-5.8(oC)/...
   Mean: 0.0(oC)/-17.2(oC)/-16.8(oC)/...
   ```

3. **Tabla de datos:**
   ```
   SN  Time                T1-oC  T2-oC  T3-oC  T4-oC  T5-oC  T6-oC  T7-oC  T8-oC
   1   2025-11-06 10:32:12 0.0   -13.6  -12.5  -13.5  0.0   -12.4  0.0    0.0
   2   2025-11-06 10:33:12 0.0   -13.9  -13.4  -13.9  0.0   -13.1  0.0    0.0
   ...
   ```

## Características principales

✅ **Extracción automática:** Lee directamente PDFs sin conversión previa  
✅ **Validación de datos:** Verifica valores contra límites de la máquina  
✅ **Manejo de errores:** Reporta problemas específicos en la extracción  
✅ **Logging detallado:** Registra todo el proceso para debugging  
✅ **Integración con correcciones:** Aplica correcciones calibradas automáticamente  

## Campos extraídos por sensor

Para cada línea de datos, se crea un `DatoEnsayoTemporal` con:

| Campo | Valor | Ejemplo |
|-------|-------|---------|
| `ensayoId` | ID del ensayo | 123 |
| `timestamp` | Fecha y hora del registro | 2025-11-06 10:32:12 |
| `valor` | Temperatura medida | -13.6 |
| `sensor` | Nombre del sensor | sensor_1 |
| `fuente` | Origen del dato | PDF_SIVCO_LOGGER |
| `anormal` | Si sobrepasa límites | true/false |

## Solución de problemas

### Error: "El archivo no tiene el formato esperado de SIVCO-LOGGER"
**Causa:** El PDF no contiene la estructura esperada (falta tabla de datos o headers)  
**Solución:** Verificar que el PDF sea un reporte válido del SIVCO-LOGGER

### Error: "No se pudo parsear línea de datos"
**Evento:** Algunas líneas pueden no parsearse correctamente  
**Si ocurre:** Se registra como DEBUG, otros datos se siguen procesando

### Pocos registros extraídos
**Verificar:**
1. El PDF tiene datos en la tabla (no solo metadatos)
2. El formato de las columnas es: SN, Time, T1-oC, T2-oC, etc.
3. Los valores de temperatura son números válidos (ej: -13.6, 0.0)

## Próximas mejoras posibles

- 🔄 Soporte para múltiples formatos de PDF
- 📊 Estadísticas pre-calculadas desde el PDF
- 🔗 Vinculación automática con datos de calibración
- 💾 Almacenamiento de metadatos del PDF en base de datos
