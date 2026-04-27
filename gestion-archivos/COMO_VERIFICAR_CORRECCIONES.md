# 🔍 Cómo Verificar que las Correcciones Están Funcionando

## 1️⃣ Indicadores Visuales en la Interfaz

### En la Tabla de Datos (Sección Análisis)
- **Columna "Corregido"**: Muestra ✓ Sí (badge azul) si el dato tiene corrección aplicada
- **Fila con fondo azul claro**: Los datos corregidos tienen un fondo azul distintivo (#e3f2fd)
- **Borde izquierdo azul**: Una línea azul en el lado izquierdo marca las filas corregidas
- **Columna "Sensor"**: Muestra el código del sensor con formato `<code>`
- **Columna "Fuente"**: Mostrará algo como "ARCHIVO_CICLO_CORRECCION" para datos corregidos

### En la Lista de Correcciones (Sección Ensayos)
- Puedes ver todos los archivos de corrección subidos
- Filtrar por ensayo específico
- Ver tamaño, fecha y descripción de cada archivo

## 2️⃣ Verificación en la Consola del Navegador

### Abrir la Consola
1. Presiona `F12` en tu navegador
2. Ve a la pestaña "Console"

### Logs Automáticos
Cada vez que se carga la tabla de datos, verás:
```
📊 Tabla de datos actualizada:
   Total de datos: 1500
   Datos con corrección aplicada: 847
   Datos sin corrección: 653
   ✅ Las correcciones están activas en 847 datos
```

### Función de Debug Avanzada
Usa esta función en la consola para análisis detallado:

```javascript
debugCorrecciones(1)  // Reemplaza 1 con el ID de tu ensayo
```

Esta función te mostrará:
- ✅ Cantidad de archivos de corrección subidos
- 📊 Total de datos temporales
- ✅ Cuántos datos tienen corrección aplicada
- 🔌 Lista de sensores y estadísticas por sensor
- 📝 Ejemplos de datos corregidos vs no corregidos

**Ejemplo de salida:**
```
🔍 ========================================
🔍 DEBUG DE CORRECCIONES
🔍 ========================================
📋 Correcciones encontradas: 1

📄 Archivos de corrección:
  1. a0d22431-7743-42e0-a623-553ab1fde2ad_CORRECCIONES.csv (CSV)
     - Tamaño: 2.45 KB
     - Fecha: 27/12/2025 10:30:00
     - Ruta: correcciones/ensayo_1/...

📊 Total de datos temporales: 1500
✅ Datos con corrección aplicada: 847
📌 Datos sin corrección: 653

🔌 Sensores detectados: 3
   - sensor_1: 500 datos (300 corregidos)
   - sensor_2: 500 datos (280 corregidos)
   - sensor_3: 500 datos (267 corregidos)

✅ Ejemplos de datos CORREGIDOS:
   Sensor: sensor_1, Valor: 121.3456, Fuente: ARCHIVO_CICLO_CORRECCION
   Sensor: sensor_2, Valor: 122.7890, Fuente: ARCHIVO_CICLO_CORRECCION
   Sensor: sensor_3, Valor: 120.9876, Fuente: ARCHIVO_CICLO_CORRECCION
```

## 3️⃣ Verificación en los Logs del Servidor

Revisa el archivo `app.err` o los logs de la aplicación:

```bash
# En PowerShell
Get-Content app.err -Tail 50 | Select-String "correc"
```

Busca líneas como:
```
Aplicando 1 correcciones al ensayo 1
Mapa de correcciones creado con 3 entradas
Correcciones aplicadas: 847 de 1500 datos modificados
Aplicando corrección a sensor_1: 121.00 -> 121.3456 (A=0.5, B=1.02)
```

## 4️⃣ Verificación Manual de la Fórmula

La fórmula aplicada es:
```
valor_corregido = A + B×x + C×x² + D×x³
```

**Ejemplo práctico:**
Si tienes un valor original de `120°C` y coeficientes:
- A = 0.5
- B = 1.02
- C = 0.0001
- D = -0.00001

Entonces:
```
valor_corregido = 0.5 + (1.02 × 120) + (0.0001 × 120²) + (-0.00001 × 120³)
                = 0.5 + 122.4 + 1.44 + (-17.28)
                = 107.06°C
```

## 5️⃣ Comparación Antes/Después

### Método 1: Revisar archivos CSV originales
1. Ve a la carpeta donde guardaste los archivos CSV originales
2. Compara los valores con los que se muestran en la tabla de análisis
3. Los valores corregidos deberían ser diferentes

### Método 2: Script de verificación
Ejecuta el script de PowerShell:
```powershell
.\verificar_correcciones.ps1
```

Este script:
- Lista los archivos de corrección encontrados
- Calcula ejemplos de la fórmula
- Verifica el estado del API
- Muestra ensayos con Factor Histórico

## 6️⃣ Checklist de Verificación

- [ ] Los archivos de corrección CSV están en `correcciones/ensayo_X/`
- [ ] En la tabla de análisis hay filas con fondo azul claro
- [ ] La columna "Corregido" muestra ✓ Sí para algunos datos
- [ ] La columna "Fuente" incluye "_CORRECCION" para datos corregidos
- [ ] La consola muestra logs con "Datos con corrección aplicada: X"
- [ ] `debugCorrecciones(ID)` muestra correcciones activas
- [ ] Los valores en la tabla son diferentes a los del CSV original

## 7️⃣ Solución de Problemas

### Si no ves datos corregidos:

1. **Verifica que el archivo de corrección existe:**
   ```javascript
   debugCorrecciones(1)  // Debería mostrar archivos
   ```

2. **Verifica el formato del CSV de corrección:**
   Debe tener este formato:
   ```csv
   sensor,A,B,C,D
   sensor_1,0.5,1.02,0.0001,-0.00001
   sensor_2,-0.3,1.05,0,0
   ```

3. **Recarga los datos:**
   - Vuelve a cargar el archivo CSV del ensayo
   - Las correcciones se aplican automáticamente al cargar

4. **Revisa los logs del servidor:**
   Busca errores relacionados con la carga de correcciones

## 8️⃣ Función del Factor Histórico (FH)

Si tienes "Calcular FH" habilitado, verás:
- Tarjeta con "Factor Histórico (FH): X.XXXXXX"
- Parámetro Z usado
- Este valor también aparece en los reportes PDF

## 📞 Ayuda Adicional

Si después de seguir estos pasos aún no ves las correcciones:
1. Abre la consola del navegador (F12)
2. Ejecuta: `debugCorrecciones(TU_ENSAYO_ID)`
3. Copia toda la salida
4. Revisa si hay errores en rojo

Las correcciones funcionan correctamente cuando:
✅ Tienes archivos CSV en la carpeta de correcciones
✅ Los datos tienen "_CORRECCION" en la fuente
✅ Los valores mostrados son diferentes a los originales
✅ La función debugCorrecciones muestra datos corregidos
