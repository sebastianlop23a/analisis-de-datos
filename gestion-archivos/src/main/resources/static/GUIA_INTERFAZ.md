# 🌐 ACCESO A LA INTERFAZ WEB

## ¡Tu interfaz web está lista! 🎉

Una vez que tengas el servidor corriendo, puedes acceder a la interfaz web en:

```
http://localhost:8080
```

O accede directamente a:

```
http://localhost:8080/index.html
```

---

## 📖 GUÍA RÁPIDA DE USO

### Paso 1: Crear una Máquina 🏭

1. Ve a la sección **"Máquinas"** en la barra de navegación
2. Rellena el formulario "Crear Nueva Máquina":
   - **Nombre**: Ej: "Horno A1"
   - **Tipo**: Ej: "Horno"
   - **Límite Inferior**: Ej: 20
   - **Límite Superior**: Ej: 150
   - **Unidad Medida**: Ej: "°C"
   - **Descripción**: (Opcional)
   - **Ubicación**: (Opcional)
3. Click en "Crear Máquina"
4. ¡Verás la máquina en la lista de abajo!

### Paso 2: Crear un Ensayo 📋

1. Ve a la sección **"Ensayos"**
2. Rellena "Crear Nuevo Ensayo":
   - **Nombre del Ensayo**: Ej: "Prueba 1"
   - **Máquina**: Selecciona la máquina que creaste
   - **Responsable**: (Opcional)
   - **Descripción**: (Opcional)
3. Click en "Crear Ensayo"
4. El ensayo estará en estado "EN_PROGRESO"

### Paso 3: Registrar Datos 📊

**Opción A: Registrar uno por uno**
1. En "Registrar Datos en Ensayo":
   - Selecciona tu ensayo
   - Ingresa un valor (Ej: 45.5)
   - Ingresa la fuente (Ej: SENSOR_A)
   - Click en "Registrar Dato"

**Opción B: Cargar desde archivo**
1. En "Cargar Datos desde Archivo":
   - Selecciona tu ensayo
   - Carga un archivo CSV o TXT (ejemplo_datos.csv o ejemplo_datos.txt)
   - Click en "Cargar"
2. ¡Se cargarán todos los datos automáticamente!

### Paso 4: Ver Análisis 📈

1. Ve a la sección **"Análisis"**
2. Selecciona tu ensayo en el dropdown
3. ¡Verás automáticamente:
   - ✓ Media
   - ✓ Desviación Estándar
   - ✓ Máximo y Mínimo
   - ✓ Datos Anormales
   - ✓ Gráfico de Distribución
   - ✓ Tabla de Datos

### Paso 5: Generar Reporte 📄

1. **PRIMERO**: Debes finalizar el ensayo
   - Ve a "Ensayos"
   - Click en "Finalizar" en tu ensayo

2. Ve a la sección **"Reportes"**
3. En "Generar Reporte":
   - Selecciona tu ensayo
   - Elige el tipo (Excel, CSV, PDF)
   - Tu nombre (Opcional)
   - Click en "Generar Reporte"

### Paso 6: Descargar Datos 📥

En "Descargar Archivos":
- Selecciona el ensayo
- Elige el formato:
  - **Excel (Datos)**: Todos los datos en Excel
  - **Excel (Reporte)**: Resumen con estadísticas
  - **CSV (Datos)**: Archivo CSV
- Click en "Descargar"

---

## 🎯 EJEMPLOS DE FLUJOS

### Flujo 1: Crear y Analizar un Ensayo Rápido
```
1. Dashboard (ver estado)
2. Máquinas → Crear "Horno Test"
3. Ensayos → Crear "Ensayo 1" con Horno Test
4. Ensayos → Registrar datos (5-10 valores)
5. Análisis → Ver gráficos y estadísticas
6. Ensayos → Finalizar ensayo
7. Reportes → Generar y descargar
```

### Flujo 2: Cargar Datos en Batch
```
1. Máquinas → Crear máquina
2. Ensayos → Crear ensayo
3. Ensayos → Cargar archivo CSV/TXT
4. Análisis → Ver todo analizado automáticamente
5. Reportes → Descargar
```

### Flujo 3: Múltiples Ensayos Simultáneos
```
1. Máquinas → Crear 3 máquinas diferentes
2. Ensayos → Crear 3 ensayos (uno por máquina)
3. Ensayos → Registrar datos en cada uno
4. Dashboard → Ver comparación de máquinas
5. Análisis → Analizar uno por uno
6. Reportes → Generar reportes para cada uno
```

---

## ℹ️ INFORMACIÓN DEL DASHBOARD

El dashboard te muestra en tiempo real:
- 📊 **Máquinas Activas**: Total de máquinas registradas
- 📋 **Ensayos en Progreso**: Cuántos están siendo ejecutados ahora
- ✓ **Ensayos Completados**: Cuántos ya finalizaron
- 📄 **Total Reportes**: Reportes generados

Además verás:
- 📈 Gráfico de estados de ensayos
- 📊 Gráfico de ensayos por máquina
- ℹ️ Información del sistema

---

## 🔄 ESTADOS DE UN ENSAYO

```
EN_PROGRESO ⏳
    ↓ (Se pueden registrar datos)
    ├→ PAUSADO ⏸️ (click "Pausar")
    │   └→ EN_PROGRESO ⏳ (click "Reanudar")
    └→ COMPLETADO ✓ (click "Finalizar")
        ↓
    REPORTE_GENERADO ✓

O en cualquier momento:
    → CANCELADO ✕ (click "Cancelar")
```

---

## 🎨 INTERFAZ VISUAL

### Navbar (Arriba)
- Logo del sistema
- Secciones de navegación
- Indicador de estado del API

### Secciones Principales
1. **Dashboard**: Resumen y gráficos
2. **Máquinas**: Gestión de equipos
3. **Ensayos**: Ejecución de pruebas
4. **Análisis**: Estadísticas y gráficos
5. **Reportes**: Generación de reportes

### Elementos Comunes
- ✅ Formularios con validación
- ✅ Listas de elementos
- ✅ Gráficos interactivos
- ✅ Botones de acción
- ✅ Notificaciones (Toast)
- ✅ Indicadores de estado

---

## ⌨️ ATAJOS Y TIPS

### Validaciones
- Los campos marcados con * son obligatorios
- Los límites de máquina: inferior < superior
- Los valores se validan automáticamente

### Auto-actualización
- Dashboard se actualiza cada 5 segundos
- Ensayos se actualizan automáticamente
- Reportes se actualizan al generar

### Descarga de Archivos
- Excel: Abre en Excel, Google Sheets, etc.
- CSV: Abre en cualquier editor de texto o Excel
- PDF: Se abrirá en tu navegador

---

## 🐛 SI ALGO NO FUNCIONA

### El API no responde
```
✓ Asegúrate que el servidor está corriendo
✓ Verifica: http://localhost:8080/api/utilidades/salud
✓ Abre la consola (F12) para ver errores
```

### Los gráficos no aparecen
```
✓ Recarga la página (Ctrl+F5)
✓ Abre consola (F12) y busca errores
✓ Verifica que hay datos en el ensayo
```

### Las notificaciones no se ven
```
✓ Abre consola (F12)
✓ Busca mensajes de error
✓ Verifica que los campos están completos
```

### El archivo no se carga
```
✓ Verifica que sea .csv o .txt
✓ Verifica el formato del archivo
✓ Intenta con ejemplo_datos.csv
```

---

## 📞 SOPORTE

Revisa los archivos de documentación:
- 📖 `README_ES.md` - Documentación completa
- 🔌 `API_DOCUMENTATION.md` - Endpoints detallados
- 📦 `INSTALLATION_GUIDE.md` - Instalación

---

**¡Disfruta usando el Sistema de Gestión de Ensayos!** 🎉

Version: 1.0.0  
Last Updated: Noviembre 2024
