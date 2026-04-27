# 🔧 Solución: Las Correcciones No Se Aplican

## ❗ Problema Identificado

Has subido archivos de corrección pero **los cálculos no se están aplicando correctamente**.

## 🔍 Causas Posibles

### 1. **Aplicación No Reiniciada**
- **Problema**: El código se compiló pero la aplicación sigue corriendo con código viejo
- **Solución**: Reiniciar la aplicación

### 2. **Nombres de Sensores No Coinciden**
- **Problema**: El CSV dice "Sensor 2" pero los datos tienen "sensor_2"
- **Verificado**: El código YA normaliza nombres automáticamente ✅
- **Debería funcionar**: El código convierte "Sensor 2" → "sensor_2"

### 3. **Archivo No Se Encuentra**
- **Problema**: La ruta del archivo de corrección no es correcta
- **Verificar**: Los archivos están en `correcciones/ensayo_X/`

## ✅ Pasos para Solucionar

### Paso 1: Detener la Aplicación Actual

```powershell
# Ver procesos Java ejecutándose
Get-Process java -ErrorAction SilentlyContinue

# Si hay alguno, detenerlo
Stop-Process -Name java -Force
```

### Paso 2: Compilar y Reiniciar

```powershell
cd "c:\Users\sebastisan lopez\Desktop\gestion-archivos\gestion-archivos"

# Compilar
mvn clean package -DskipTests

# Iniciar aplicación
mvn spring-boot:run
```

### Paso 3: Subir Archivos de Corrección

1. Ve a la interfaz web: `http://localhost:8080`
2. Entra al ensayo que quieres corregir
3. Sube el archivo `*_CORRECCIONES.csv`
4. Espera confirmación de subida exitosa

### Paso 4: Cargar Datos del Ciclo

1. En el mismo ensayo, sube el archivo de datos del ciclo
2. El sistema automáticamente aplicará las correcciones

### Paso 5: Verificar en Logs

```powershell
# Ver logs en tiempo real
Get-Content app.err -Wait -Tail 20

# Buscar logs de correcciones
Get-Content app.err -Tail 100 | Select-String "Correc|aplica"
```

**Deberías ver mensajes como:**
```
[INFO] Aplicando 1 correcciones al ensayo X
[INFO] Mapa de correcciones creado con 3 entradas
[INFO] Sensores con corrección disponible: sensor_1, sensor_2, sensor_3
[INFO] Correcciones aplicadas: 847 de 1500 datos modificados
```

### Paso 6: Verificar en la Interfaz

1. Ve a la sección **"Análisis"**
2. Revisa la tabla de datos
3. Busca la columna **"Corregido"**:
   - ✅ **Sí** = Corrección aplicada
   - ❌ **No** = Sin corrección
4. Las filas con corrección tienen **fondo azul claro**

## 🧪 Prueba Rápida de Corrección

Abre la **consola del navegador** (F12) y ejecuta:

```javascript
debugCorrecciones(1)  // Reemplaza 1 con tu ID de ensayo
```

Esto te mostrará:
- ✅ Archivos de corrección subidos
- ✅ Cuántos datos tienen corrección aplicada  
- ✅ Sensores detectados
- ✅ Ejemplos de datos corregidos

## 📊 Formato Correcto del CSV de Correcciones

Tu archivo CSV debe tener este formato:

```csv
"SENSOR","COEFICIENTE A","COEFICIENTE B","COEFICIENTE C","COEFICIENTE D"
"Sensor 2","1.7166e-5","-0.00048","0","-4.5"
"Sensor 3","2.5e-5","-0.0006","0.0001","-5.0"
```

**Notas importantes:**
- ✅ El nombre del sensor puede ser "Sensor 2" o "sensor_2" (se normaliza automáticamente)
- ✅ Los coeficientes pueden estar en notación científica (1.7e-5)
- ✅ Columnas adicionales son opcionales

## 🔎 Diagnóstico de Problemas

### Si NO ves correcciones aplicadas:

#### 1. Revisa los logs:
```powershell
Get-Content app.err -Tail 100 | Select-String "Sensores"
```

Busca líneas como:
```
[WARN] No se aplicó ninguna corrección.
[WARN] Sensores en datos: sensor_1, sensor_2, sensor_3
[WARN] Sensores en correcciones: Sensor 1, Sensor 2, Sensor 3
```

Si ves esto, significa que los nombres no coinciden (aunque **NO DEBERÍA pasar** porque el código normaliza).

#### 2. Verifica el archivo subido:

```powershell
# Ver primer archivo de corrección
Get-ChildItem "correcciones" -Recurse -Filter "*_CORRECCIONES.csv" | 
    Select-Object -First 1 | 
    ForEach-Object { Get-Content $_.FullName -Head 5 }
```

#### 3. Verifica que la corrección esté en la base de datos:

Ve a: `http://localhost:8080/api/correcciones/{ensayo_id}`

Deberías ver un JSON con tu archivo de corrección.

## 🎯 Ejemplo Completo

### 1. Tu archivo `ensayo1_CORRECCIONES.csv`:
```csv
"SENSOR","COEFICIENTE A","COEFICIENTE B","COEFICIENTE C","COEFICIENTE D"
"Sensor 2","0.5","1.02","0.0001","-0.00001"
```

### 2. Carga el archivo en el ensayo

### 3. Luego carga el archivo de datos

### 4. Verifica en logs:
```
[INFO] Aplicando 1 correcciones al ensayo 1
[INFO] Coeficientes cargados para Sensor 2 (normalizado: sensor_2): A=0.5, B=1.02, C=0.0001, D=-0.00001
[INFO] Aplicando corrección a sensor_2: 120.00 -> 122.52
[INFO] Correcciones aplicadas: 500 de 1500 datos modificados
```

### 5. En la interfaz verás:
- Columna "Corregido" con ✅ Sí
- Fondo azul en las filas corregidas
- Valores diferentes a los originales

## 🆘 Si Sigue Sin Funcionar

1. **Detén TODOS los procesos Java**:
   ```powershell
   Stop-Process -Name java -Force
   ```

2. **Limpia y recompila**:
   ```powershell
   mvn clean package -DskipTests
   ```

3. **Inicia de nuevo**:
   ```powershell
   mvn spring-boot:run
   ```

4. **Espera a que inicie completamente** (ver mensaje "Started GestionArchivosApplication")

5. **Sube primero el archivo de CORRECCIONES**

6. **Luego sube el archivo de DATOS**

7. **Verifica en logs y en la interfaz**

## 📝 Notas Importantes

- ✅ **Las correcciones se aplican automáticamente** al cargar los datos
- ✅ **No necesitas hacer nada extra** después de subir los archivos
- ✅ **El sistema normaliza nombres** de sensores automáticamente
- ✅ **Puedes subir correcciones antes o después** de los datos
- ⚠️ **Si subes datos primero**, debes **volver a cargarlos** después de subir correcciones

## 🎉 ¿Cómo Saber que Funcionó?

1. **En los logs** verás: `Correcciones aplicadas: X de Y datos modificados`
2. **En la interfaz** verás: Columna "Corregido" con ✅ Sí
3. **En la consola** (debugCorrecciones): Verás datos con corrección aplicada
4. **En el reporte PDF**: Los valores serán diferentes a los originales

---

**¿Aún no funciona?** Ejecuta el script de diagnóstico:

```powershell
.\test_completo.ps1
```

Esto verificará todos los componentes del sistema.
