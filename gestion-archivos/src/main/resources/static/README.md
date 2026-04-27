# 🌐 Interfaz Web - Sistema de Gestión de Ensayos

## 📋 Descripción

Interfaz web moderna y responsiva para el Sistema de Gestión de Ensayos de Máquinas. Proporciona una experiencia de usuario intuitiva para gestionar máquinas, ensayos, análisis de datos y generación de reportes.

## 🎨 Estructura

```
src/main/resources/static/
├── index.html           # Página HTML principal
├── css/
│   └── styles.css       # Estilos CSS (completo + responsivo)
└── js/
    ├── config.js        # Configuración de la API
    ├── api.js           # Funciones de comunicación con API
    └── app.js           # Lógica principal de la aplicación
```

## 📦 Dependencias

- **Chart.js 3.9.1**: Para gráficos interactivos (CDN)
- **Navegador moderno**: Chrome, Firefox, Safari, Edge

## 🚀 Características

### 1. Dashboard
- 📊 Contadores de máquinas, ensayos y reportes
- 📈 Gráficos de estados de ensayos
- 📉 Gráficos de distribución de ensayos por máquina
- ℹ️ Información del sistema

### 2. Gestión de Máquinas
- ➕ Crear nuevas máquinas
- 📝 Editar máquinas existentes
- 🗑️ Eliminar máquinas
- ⚙️ Configurar límites de operación
- 🔍 Listar todas las máquinas

### 3. Gestión de Ensayos
- ➕ Crear nuevos ensayos
- 📊 Registrar datos individuales
- 📁 Cargar datos desde archivos (CSV, TXT)
- ⏸️ Pausar ensayos
- ⏹️ Finalizar ensayos
- ❌ Cancelar ensayos

### 4. Análisis de Datos
- 📈 Estadísticas: media, desviación estándar, máx, mín, rango
- 🔴 Detección automática de datos anormales
- 📊 Gráficos de distribución
- 📋 Tabla de datos detallada
- 📐 Coeficiente de variación
- 📊 Porcentaje de anormalidad

### 5. Reportes
- 📄 Generar reportes automáticos
- 📥 Descargar en Excel (.xlsx)
- 📥 Descargar en CSV (.csv)
- 📥 Descargar datos completos
- 📊 Exportación con estadísticas

## 🎯 Secciones de la Interfaz

### Dashboard
```
🏢 Sistema de Gestión de Ensayos
├─ Contadores (Máquinas, Ensayos, Reportes)
├─ Gráfico: Estados de Ensayos
├─ Gráfico: Ensayos por Máquina
└─ Info del Sistema
```

### Máquinas
```
Crear Máquina
├─ Nombre *
├─ Tipo *
├─ Límite Inferior *
├─ Límite Superior *
├─ Unidad *
├─ Descripción
└─ Ubicación

Listado de Máquinas
├─ Nombre
├─ Tipo
├─ Rango
├─ Estado (Activa/Inactiva)
├─ Editar
└─ Eliminar
```

### Ensayos
```
Crear Ensayo
├─ Nombre *
├─ Máquina *
├─ Responsable
└─ Descripción

Registrar Datos
├─ Ensayo en Progreso *
├─ Valor *
└─ Fuente

Cargar desde Archivo
├─ Ensayo *
├─ Archivo (CSV/TXT) *
└─ Cargar

Listado de Ensayos
├─ Nombre
├─ Máquina
├─ Estado
├─ Responsable
├─ Fecha Inicio
├─ Acciones (Pausar, Finalizar, Cancelar)
```

### Análisis
```
Seleccionar Ensayo para Análisis

Tarjetas de Estadísticas
├─ Total de Datos
├─ Media
├─ Desviación Estándar
├─ Máximo
├─ Mínimo
└─ Datos Anormales

Gráficos
├─ Distribución de Datos (Histograma)
└─ Normales vs Anormales (Doughnut)

Tabla de Datos
├─ # (Secuencia)
├─ Timestamp
├─ Valor
├─ Anormal (Sí/No)
└─ Fuente
```

### Reportes
```
Generar Reporte
├─ Ensayo *
├─ Tipo *
└─ Generado por

Descargar Archivos
├─ Ensayo *
├─ Formato *
└─ Descargar

Listado de Reportes
├─ Tipo
├─ Fecha Generación
└─ Generado Por
```

## 🎨 Diseño

### Colores
```
Primario:     #3498db (Azul)
Secundario:   #2ecc71 (Verde)
Peligro:      #e74c3c (Rojo)
Advertencia:  #f39c12 (Naranja)
Info:         #9b59b6 (Púrpura)
Oscuro:       #2c3e50 (Gris Oscuro)
Claro:        #ecf0f1 (Gris Claro)
```

### Elementos
- **Navbar**: Barra de navegación adhesiva
- **Tarjetas**: Contenedores con sombra y hover
- **Botones**: Colores contextuales (Primario, Secundario, Éxito, Peligro, Info)
- **Formularios**: Inputs con validación visual
- **Tablas**: Estilos alternados y hover
- **Gráficos**: Chart.js con temas coherentes
- **Notificaciones**: Toast en esquina inferior derecha
- **Modal**: Popup para confirmaciones

## 🔧 Configuración

### config.js
```javascript
API_CONFIG = {
    BASE_URL: 'http://localhost:8080/api',
    ENDPOINTS: { ... },
    TIMEOUT: 10000,
    RETRY_ATTEMPTS: 3
}
```

### Cambiar URL de API
Edita `src/main/resources/static/js/config.js`:
```javascript
BASE_URL: 'http://tu-dominio.com/api'
```

## 📱 Responsividad

- ✅ Desktop (1200px+)
- ✅ Tablet (768px - 1199px)
- ✅ Mobile (480px - 767px)
- ✅ Small Mobile (< 480px)

Los estilos se adaptan automáticamente a cada tamaño de pantalla.

## 🔄 Funciones de Auto-actualización

- **Dashboard**: Se actualiza cada 5 segundos
- **Ensayos**: Se actualiza cada 5 segundos
- **Reportes**: Se actualiza cada 5 segundos
- **API Status**: Se verifica cada 5 segundos

## 🔐 Funcionalidades de Seguridad

- ✅ Validación de campos en cliente
- ✅ Confirmaciones antes de acciones destructivas
- ✅ Manejo de errores y reintentos automáticos
- ✅ Timeouts para peticiones (10 segundos)
- ✅ Estados de carga visuales

## 📊 Gráficos

### Chart.js Integration
```javascript
// Pie Chart - Estados
chartEstados = new Chart(ctx, { type: 'pie', ... })

// Bar Chart - Máquinas
chartMaquinas = new Chart(ctx, { type: 'bar', ... })

// Bar Chart - Distribución de Datos
chartDatos = new Chart(ctx, { type: 'bar', ... })

// Doughnut Chart - Anormales
chartAnormales = new Chart(ctx, { type: 'doughnut', ... })
```

## 🖼️ Temas Visuales

### Estados de Ensayo
- 🟡 **EN_PROGRESO**: Amarillo (En ejecución)
- 🟢 **COMPLETADO**: Verde (Finalizado exitosamente)
- 🔵 **PAUSADO**: Azul (Pausado temporalmente)
- 🔴 **CANCELADO**: Rojo (Cancelado)

### Badges
- ✓ Status badges con colores contextuales
- Emojis para identificación visual rápida

## 🚀 Uso Inicial

1. **Crear Máquinas**: Ir a "Máquinas" y crear al menos una
2. **Crear Ensayo**: Ir a "Ensayos" y crear nuevo
3. **Registrar Datos**: Usar formulario o cargar archivo
4. **Ver Análisis**: Ir a "Análisis" y seleccionar ensayo
5. **Generar Reporte**: Ir a "Reportes" y generar

## 🐛 Troubleshooting

| Problema | Solución |
|----------|----------|
| API no responde | Verificar que el servidor está corriendo en localhost:8080 |
| Gráficos no cargan | Revisar consola (F12) para errores de Chart.js |
| Formularios no envían | Verificar que todos los campos requeridos (*) están completos |
| Archivos no cargan | Asegurarse que sea CSV o TXT con formato correcto |
| Toast no se muestra | Verificar que el div #toast existe en HTML |

## 📈 Performance

- Carga inicial: < 2 segundos
- Gráficos se dibujan en < 500ms
- Auto-actualización sin bloqueos
- Gestión de memoria de gráficos (destroy/recreate)

## 🔄 Actualizaciones Futuras

- [ ] Autenticación de usuarios
- [ ] Búsqueda y filtrado avanzado
- [ ] Exportación a PDF
- [ ] Gráficos en tiempo real (WebSocket)
- [ ] Múltiples idiomas (i18n)
- [ ] Temas oscuro/claro
- [ ] Historial de cambios
- [ ] Notificaciones push

## 📝 Notas

- La interfaz está diseñada para trabajar con Spring Boot en `localhost:8080`
- Todos los estilos están en un solo archivo CSS para facilitar el deployment
- JavaScript puro (sin frameworks adicionales) para máxima compatibilidad
- Soporta navegadores ES6+

---

**Versión**: 1.0.0  
**Última actualización**: Noviembre 2024  
**Autor**: Sistema SIVCO
