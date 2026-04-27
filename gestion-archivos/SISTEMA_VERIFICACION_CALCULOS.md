# 🧪 Sistema de Verificación de Cálculos y Correcciones

Este documento describe cómo verificar que todos los cálculos del sistema funcionan correctamente.

## 📋 Contenido

1. [Pruebas Unitarias Automatizadas](#pruebas-unitarias)
2. [Verificación Manual](#verificación-manual)
3. [Script de Verificación PowerShell](#script-powershell)
4. [Verificación en la Interfaz Web](#interfaz-web)
5. [Logs del Sistema](#logs-sistema)

---

## 🔬 Pruebas Unitarias Automatizadas

### Ejecutar Todas las Pruebas

```powershell
# Desde la carpeta gestion-archivos
mvn test
```

### Ejecutar Pruebas Específicas

```powershell
# Solo pruebas de Análisis
mvn test -Dtest=AnalisisServicioTest

# Solo pruebas de Correcciones
mvn test -Dtest=CoeficientesCorreccionTest
```

### Pruebas Incluidas

#### 1. **AnalisisServicioTest** - Pruebas Estadísticas
- ✅ Cálculo de Media
- ✅ Cálculo de Desviación Estándar
- ✅ Cálculo de Máximo y Mínimo
- ✅ Cálculo de Rango
- ✅ Coeficiente de Variación
- ✅ **Factor Histórico (FH)** con diferentes valores de Z
- ✅ Error Estándar
- ✅ Límites de Confianza
- ✅ Porcentaje de Anormales

#### 2. **CoeficientesCorreccionTest** - Pruebas de Correcciones
- ✅ Fórmula completa: `A + B×x + C×x² + D×x³`
- ✅ Corrección lineal
- ✅ Corrección con offset
- ✅ Corrección con escala
- ✅ Temperaturas típicas de esterilización
- ✅ Valores extremos (manejo de errores)
- ✅ Comparación con y sin corrección

### Interpretación de Resultados

```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

- ✅ **Success**: Todos los cálculos funcionan correctamente
- ❌ **Failures**: Algún cálculo produce resultados incorrectos
- ⚠️ **Errors**: Problemas de ejecución (revisar logs)

---

## 🔍 Verificación Manual

### 1. Verificar Fórmula de Corrección

**Fórmula**: `valor_corregido = A + B×x + C×x² + D×x³`

**Ejemplo Manual**:
```
Valor original (x) = 120°C
Coeficientes:
  A = 0.5
  B = 1.02
  C = 0.0001
  D = -0.00001

Cálculo:
  = 0.5 + (1.02 × 120) + (0.0001 × 120²) + (-0.00001 × 120³)
  = 0.5 + 122.4 + 1.44 + (-17.28)
  = 107.06°C
```

### 2. Verificar Fórmula de Factor Histórico

**Fórmula**: `FH = Σ(10^((Ti - 250)/z) × Δt)`

**Ejemplo Manual**:
```
Temperaturas: [121, 122, 123, 124, 125]°C
Z = 14
Δt = 1 minuto entre cada medición

Para T1=121°C:
  exponente = (121 - 250) / 14 = -9.2143
  término = 10^(-9.2143) × 1 = 6.1×10⁻¹⁰

Para T2=122°C:
  exponente = (122 - 250) / 14 = -9.1429
  término = 10^(-9.1429) × 1 = 7.2×10⁻¹⁰

... (continuar para todos)

FH = suma de todos los términos
```

---

## 📜 Script de Verificación PowerShell

### Ejecutar Script Completo

```powershell
cd gestion-archivos
.\verificar_correcciones.ps1
```

### Qué Verifica el Script

1. **Archivos de Corrección**
   - Lista todos los archivos `*_CORRECCIONES.csv`
   - Muestra primeras líneas de cada archivo
   
2. **Prueba de Fórmula de Corrección**
   - Calcula un ejemplo con valores conocidos
   - Verifica que el resultado sea correcto

3. **Prueba de Factor Histórico**
   - Calcula FH con temperaturas de ejemplo
   - Muestra cada término de la sumatoria

4. **Estado del API**
   - Verifica que el servidor esté ejecutándose
   - Consulta endpoint de salud

5. **Ensayos con FH**
   - Lista ensayos con Factor Histórico habilitado
   - Muestra valores de FH calculados

### Salida Esperada

```
🔍 Verificando Sistema de Correcciones y Factor Histórico

📁 1. Archivos de Correcciones encontrados:
   Total: 21 archivos
   ✓ correcciones\ensayo_1\a0d22431-7743-42e0-a623-553ab1fde2ad_CORRECCIONES.csv
   ...

🧮 2. Prueba Manual de Fórmula de Corrección:
   Fórmula: valor_corregido = A + B×x + C×x² + D×x³
   Con x=100, A=0.5, B=1.02, C=0.0001, D=-0.00001
   → Valor corregido: 93.51

🔥 3. Prueba Manual de Factor Histórico:
   Fórmula: FH = Σ(10^((Ti - 250)/z) × Δt)
   Temperaturas: 121, 122, 123, 124, 125°C
   Parámetro z: 14
   Δt: 1 minutos
   T1=121°C → exponente=-9.2143 → término=6.096E-10
   ...
   → Factor Histórico calculado: 0.000000

🌐 4. Verificando conexión con API...
   ✓ API está activa
   Estado: ok

✅ Verificación completa
```

---

## 🌐 Verificación en la Interfaz Web

### 1. Indicadores Visuales

#### En la Tabla de Datos (Sección Análisis):
- **Columna "Corregido"**: 
  - ✓ Sí = Dato con corrección aplicada (badge azul)
  - ✗ No = Dato sin corrección
  
- **Fila con fondo azul claro**: Datos corregidos (#e3f2fd)

- **Columna "Fuente"**: 
  - `ARCHIVO_CICLO_CORRECCION` = Corrección aplicada
  - `ARCHIVO_CICLO` = Sin corrección

#### En las Estadísticas:
- **Tarjeta de Factor Histórico** (si está habilitado):
  ```
  Factor Histórico (FH): 0.123456
  Parámetro Z: 14.0
  ```

### 2. Consola del Navegador (F12)

#### Logs Automáticos:
```javascript
📊 Tabla de datos actualizada:
   Total de datos: 1500
   Datos con corrección aplicada: 847
   Datos sin corrección: 653
   ✅ Las correcciones están activas en 847 datos
```

#### Función de Debug:
```javascript
// En la consola del navegador
debugCorrecciones(1)  // Reemplaza 1 con tu ID de ensayo
```

**Salida esperada**:
```
🔍 ========================================
🔍 DEBUG DE CORRECCIONES
🔍 ========================================
📋 Correcciones encontradas: 1

📄 Archivos de corrección:
  1. a0d22431-7743-42e0-a623-553ab1fde2ad_CORRECCIONES.csv
     - Tamaño: 2.45 KB
     - Fecha: 27/12/2025
     - Ruta: correcciones/ensayo_1/...

📊 Total de datos temporales: 1500
✅ Datos con corrección aplicada: 847
📌 Datos sin corrección: 653

🔌 Sensores detectados: 3
   - sensor_1: 500 datos (300 corregidos)
   - sensor_2: 500 datos (280 corregidos)
   - sensor_3: 500 datos (267 corregidos)
```

---

## 📝 Logs del Sistema

### Ver Logs en PowerShell

```powershell
# Ver últimas 50 líneas con "correc"
Get-Content app.err -Tail 50 | Select-String "correc"

# Ver logs en tiempo real
Get-Content app.err -Wait -Tail 20

# Buscar logs de Factor Histórico
Get-Content app.err -Tail 100 | Select-String "Factor|FH"
```

### Logs Importantes a Buscar

#### Correcciones:
```
[INFO] Aplicando 1 correcciones al ensayo 1
[INFO] Mapa de correcciones creado con 3 entradas
[INFO] Sensores con corrección disponible: sensor_1, sensor_2, sensor_3
[INFO] Aplicando corrección a sensor_1: 121.00 -> 121.3456 (A=0.5, B=1.02)
[INFO] Correcciones aplicadas: 847 de 1500 datos modificados
```

#### Factor Histórico:
```
[INFO] Calculando Factor Histórico con Z=14.0
[INFO] Factor Histórico calculado: 0.123456
```

#### Advertencias:
```
[WARN] No se aplicó ninguna corrección
[WARN] Sensores en datos: sensor_1, sensor_2
[WARN] Sensores en correcciones: sensor_a, sensor_b
```

---

## ✅ Checklist de Verificación Completa

### Antes de Iniciar
- [ ] API ejecutándose en `http://localhost:8080`
- [ ] Al menos un ensayo creado
- [ ] Archivos de corrección subidos

### Pruebas Unitarias
- [ ] Ejecutar `mvn test`
- [ ] Verificar que todas las pruebas pasen (0 failures)
- [ ] Revisar resultados en consola

### Script de PowerShell
- [ ] Ejecutar `.\verificar_correcciones.ps1`
- [ ] Verificar que encuentra archivos de corrección
- [ ] Verificar que calcula ejemplos correctamente
- [ ] Verificar conexión con API

### Interfaz Web
- [ ] Abrir aplicación en navegador
- [ ] Ir a sección "Análisis"
- [ ] Verificar columna "Corregido"
- [ ] Verificar filas con fondo azul
- [ ] Abrir consola (F12)
- [ ] Verificar logs de correcciones aplicadas
- [ ] Ejecutar `debugCorrecciones(ID_ENSAYO)`
- [ ] Verificar estadísticas (si aplica FH)

### Logs del Servidor
- [ ] Revisar `app.err` para mensajes de correcciones
- [ ] Buscar warnings o errors
- [ ] Verificar que se aplicaron correcciones

### Reportes PDF
- [ ] Generar reporte de ensayo con correcciones
- [ ] Verificar que valores sean diferentes a originales
- [ ] Si FH está habilitado, verificar que aparezca en reporte
- [ ] Descargar Excel y comparar valores

---

## 🐛 Solución de Problemas

### Las Correcciones No Se Aplican

**Síntoma**: Columna "Corregido" muestra "No" para todos los datos

**Verificar**:
1. Archivos CSV subidos correctamente
2. Formato de archivo correcto (ver logs)
3. Nombres de sensores coinciden entre datos y correcciones
4. Revisar logs: `Get-Content app.err | Select-String "correc"`

**Solución**:
```powershell
# Ver sensores en datos vs correcciones
Get-Content app.err -Tail 100 | Select-String "Sensores"
```

### Factor Histórico es 0

**Síntoma**: FH aparece como 0.000000

**Verificar**:
1. La máquina tiene "Calcular FH" habilitado
2. Hay al menos 2 datos con timestamps diferentes
3. El parámetro Z está configurado (default 14.0)
4. Las temperaturas están en rango razonable (> 100°C)

### Pruebas Unitarias Fallan

**Síntoma**: `mvn test` muestra failures

**Solución**:
```powershell
# Ver detalles del error
mvn test -Dtest=AnalisisServicioTest
mvn test -Dtest=CoeficientesCorreccionTest

# Ver logs detallados
mvn test -X
```

---

## 📊 Casos de Prueba Recomendados

### Caso 1: Ensayo Simple con Correcciones
1. Crear ensayo nuevo
2. Subir archivo de ciclo (datos originales)
3. Subir archivo de correcciones
4. Verificar en tabla que aparezcan datos corregidos
5. Generar reporte PDF
6. Comparar valores originales vs corregidos

### Caso 2: Factor Histórico
1. Crear máquina con "Calcular FH" habilitado
2. Crear ensayo con esa máquina
3. Subir datos de ciclo con temperaturas ~121°C
4. Verificar que se calcule FH
5. Ver FH en estadísticas
6. Generar reporte y verificar FH

### Caso 3: Múltiples Sensores
1. Subir archivo con 3+ sensores
2. Subir correcciones para cada sensor
3. Verificar que se apliquen a todos
4. Usar `debugCorrecciones()` para ver detalle por sensor

---

## 📞 Ayuda Adicional

### Recursos
- **Documentación**: Ver `COMO_VERIFICAR_CORRECCIONES.md`
- **API Docs**: Ver `API_DOCUMENTATION.md`
- **Instalación**: Ver `INSTALLATION_GUIDE.md`

### Comandos Útiles

```powershell
# Reiniciar aplicación
mvn spring-boot:run

# Limpiar y compilar
mvn clean install

# Ejecutar tests con coverage
mvn test jacoco:report

# Ver estructura de base de datos
# (usar herramienta de DB que prefieras)
```

---

## 🎯 Resumen

Este sistema de verificación incluye:

1. ✅ **Pruebas unitarias automatizadas** para todos los cálculos
2. ✅ **Script de PowerShell** para verificación rápida
3. ✅ **Herramientas en navegador** para debug en tiempo real
4. ✅ **Logs detallados** del servidor
5. ✅ **Checklist completo** de verificación

**Para verificar que todo funciona**:
```powershell
# 1. Ejecutar pruebas
mvn test

# 2. Ejecutar script
.\verificar_correcciones.ps1

# 3. Verificar en navegador (F12 -> Console)
debugCorrecciones(1)
```

✅ Si todo es verde/positivo, el sistema está funcionando correctamente!
