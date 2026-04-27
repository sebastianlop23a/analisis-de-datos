# 📑 ÍNDICE COMPLETO DEL PROYECTO

## 🎯 Documentación Principal

Comienza por leer estos archivos en orden:

1. **[README_ES.md](./README_ES.md)** ⭐ START HERE
   - Visión general del proyecto
   - Características principales
   - Guía de inicio rápido
   - Estructura básica

2. **[INSTALLATION_GUIDE.md](./INSTALLATION_GUIDE.md)** 🚀
   - Instalación paso a paso
   - Docker Compose (recomendado)
   - Instalación manual
   - Solución de problemas

3. **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** 📚
   - Documentación completa de endpoints
   - Formatos de request/response
   - Ejemplos de uso
   - Códigos de respuesta HTTP

4. **[IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)** 📋
   - Resumen técnico
   - Componentes implementados
   - Estadísticas del proyecto
   - Próximas mejoras opcionales

5. **[PROJECT_CHECKLIST.md](./PROJECT_CHECKLIST.md)** ✅
   - Checklist de implementación
   - Elementos completados
   - Estado del proyecto

---

## 🧪 NUEVA CARACTERÍSTICA: Sistema de Calibración con Regresión

**Agregado en esta sesión**: Sistema automático de calibración basado en regresión

### Documentación del Sistema de Calibración

6. **[QUICK_START.md](./QUICK_START.md)** ⭐ LEER PRIMERO (5 minutos)
   - Descripción de cambios
   - Guía rápida de 5 pasos
   - Solución de problemas
   - Tabla de referencia rápida

7. **[CALIBRATION_SYSTEM.md](./CALIBRATION_SYSTEM.md)** 📖 Referencia Completa
   - Descripción general del sistema
   - Arquitectura y componentes
   - Diagramas de flujo
   - Matemáticas de regresión
   - Guía de selección de modelos
   - Referencia de API REST
   - Opciones de configuración
   - Schema de base de datos

8. **[CALIBRATION_EXAMPLES.md](./CALIBRATION_EXAMPLES.md)** 💡 12 Ejemplos Prácticos
   - Ejemplos de API REST con curl
   - Integración de código Java
   - Formatos de archivos CSV
   - Escenarios de error
   - Procesamiento en lote
   - Consultas SQL

9. **[REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md)** 🔧 Detalles de Implementación
   - Componentes implementados
   - Modelos, servicios, repositorios
   - Componentes refactorizados
   - Estructura de archivos
   - Características clave
   - Cambios disruptivos (ninguno)

10. **[IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md)** ✓ Lista de Verificación
    - Todas las características implementadas
    - Estado de pruebas unitarias
    - Checklist pre-despliegue
    - Verificación de criterios de éxito
    - Próximos pasos para características opcionales

---

### Cambios Implementados

#### ✨ Nuevas Características
- ✅ Cálculo automático de regresión (Linear, Quadratic, Cubic)
- ✅ Almacenamiento de coeficientes en base de datos
- ✅ Selección dinámica del modelo activo por dispositivo
- ✅ 8 nuevos endpoints REST API para gestión de calibración
- ✅ Validación matemática (R², error estándar)
- ✅ Aplicación automática de correcciones a mediciones

#### 🔄 Compatibilidad Hacia Atrás
- ✅ Sistema antiguo (CalibrationCorrection) sigue funcionando
- ✅ Migración automática sin acción requerida
- ✅ Fallback automático si no hay calibración nueva
- ✅ Ambos sistemas pueden ejecutarse concurrentemente

#### 📁 Estructuras de Código Nuevas
```
src/main/java/com/sivco/gestion_archivos/

NUEVOS PAQUETES:
├── modelos/calibration/
│   ├── CalibrationPoint.java
│   ├── CalibrationSession.java
│   ├── RegressionModel.java
│   └── RegressionModelType.java

├── servicios/calibration/
│   ├── RegressionCalculationService.java
│   └── CalibrationManagementService.java

├── repositorios/calibration/
│   ├── CalibrationSessionRepositorio.java
│   ├── CalibrationPointRepositorio.java
│   └── RegressionModelRepositorio.java

REFACTORIZADOS:
├── controladores/CalibrationController.java (8 endpoints nuevos)
├── modelos/CalibrationCorrection.java (actualizado)
├── servicios/CalibrationCorrectionServicio.java (refactorizado)
└── servicios/CargaDatosServicio.java (refactorizado)
```

#### 🗄️ Cambios de Base de Datos
- Nueva migración: `V7__create_regression_calibration_system.sql`
- 3 nuevas tablas: calibration_sessions, calibration_points, regression_models
- Índices optimizados para rendimiento
- Sin cambios disruptivos a esquema existente

#### 🧪 Pruebas
- 11 nuevos tests unitarios: `RegressionCalculationServiceTest.java`
- Cobertura: Linear/Quadratic/Cubic, aplicación de modelos, manejo de errores
- Ejecutar: `mvn test -Dtest=RegressionCalculationServiceTest`

---

### Endpoints REST Nuevos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/calibrations/upload/{deviceId}` | Subir archivo de calibración |
| GET | `/api/calibrations/active/{deviceId}` | Obtener calibración activa |
| GET | `/api/calibrations/history/{deviceId}` | Historial de calibraciones |
| PUT | `/api/calibrations/{id}/set-model` | Cambiar modelo activo |
| GET | `/api/calibrations/{id}/model/{type}/coefficients` | Obtener coeficientes |
| POST | `/api/calibrations/{deviceId}/apply-correction` | Aplicar corrección |
| GET | `/api/calibrations/{deviceId}/has-active` | Verificar si está calibrado |

---

### Flujo de Calibración

```
1. Subir CSV con puntos de calibración
   (patron_reading, instrument_reading)
           ↓
2. Calcular correcciones automáticas
   correction = patron - instrument
           ↓
3. Calcular 3 modelos de regresión
   ├── Linear: y = m*x + b
   ├── Quadratic: y = a*x² + b*x + c
   └── Cubic: y = a*x³ + b*x² + c*x + d
           ↓
4. Guardar coeficientes en BD
           ↓
5. Activar modelo (default: LINEAR)
           ↓
6. Aplicar a mediciones futuras
   corrected = model.apply(raw_reading)
```

---

### Matemáticas de Regresión

**Métodos implementados:**
- Mínimos cuadrados: minimiza Σ(residuales²)
- Eliminación Gaussiana: resuelve Ax=b para coeficientes
- Validación de ajuste: métrica R² (0-1, mayor es mejor)
- Error estándar: √(MSE) para estimación de error

**Criterios de calidad:**
- R² > 0.95: Excelente
- R² > 0.80: Bueno
- R² > 0.50: Aceptable
- R² < 0.50: Deficiente

---

### Cómo Comenzar

**Opción 1: Lectura Rápida (5 minutos)**
1. Lee QUICK_START.md
2. Revisa 3-4 ejemplos de CALIBRATION_EXAMPLES.md

**Opción 2: Aprendizaje Técnico (2 horas)**
1. Lee CALIBRATION_SYSTEM.md
2. Estudia ejemplos de CALIBRATION_EXAMPLES.md
3. Revisa código en `src/main/java/.../calibration/`

**Opción 3: Implementación Completa (4 horas)**
1. Ejecuta migración V7: `V7__create_regression_calibration_system.sql`
2. Ejecuta tests: `mvn test -Dtest=RegressionCalculationServiceTest`
3. Prueba endpoints REST
4. Integra en tu código

---

### Formato de Archivo CSV

```csv
patron_reading,instrument_reading
100,99.5
101,100.4
102,101.3
103,102.2
```

Requisitos:
- Mínimo 2 puntos para Linear
- Mínimo 3 puntos para Quadratic
- Mínimo 4 puntos para Cubic
- Formato: valores separados por comas

---

### Integración en Java

```java
@Autowired
private CalibrationManagementService calibrationService;

// Aplicar corrección
Double corrected = calibrationService.applyCorrection(deviceId, rawReading);

// Verificar si está calibrado
boolean hasCalibration = calibrationService.hasActiveCalibration(deviceId);

// Obtener detalles de calibración
CalibrationSession session = calibrationService.getActiveCalibration(deviceId);
```

---

### Compatibilidad y Migración

✅ **Cambios disruptivos**: Ninguno
✅ **Compatibilidad hacia atrás**: 100%
✅ **Migración requerida**: Solo BD (V7)
✅ **Acción del usuario**: Ninguna requerida

El sistema antiguo sigue funcionando. El sistema nuevo es usado automáticamente cuando está disponible.

---

## 🗂️ Estructura de Carpetas

```
gestion-archivos/
│
├── 📚 DOCUMENTACIÓN
│   ├── README_ES.md                    # Guía principal
│   ├── API_DOCUMENTATION.md            # Documentación de endpoints
│   ├── INSTALLATION_GUIDE.md           # Guía de instalación
│   ├── IMPLEMENTATION_SUMMARY.md       # Resumen técnico
│   ├── PROJECT_CHECKLIST.md            # Checklist
│   ├── INDEX.md                        # Este archivo
│   └── HELP.md                         # Ayuda adicional
│
├── 🚀 CONFIGURACIÓN & DESPLIEGUE
│   ├── docker-compose.yml              # Compose para Docker
│   ├── Dockerfile                      # Imagen Docker
│   ├── init.sql                        # Script de BD
│   ├── pom.xml                         # Dependencias Maven
│   ├── mvnw / mvnw.cmd                 # Maven Wrapper
│   └── .gitignore                      # Ignorar archivos
│
├── 📋 TESTING & EJEMPLOS
│   ├── Postman_Collection.json         # Collection Postman
│   ├── demo.sh                         # Script de demostración
│   ├── ejemplo_datos.csv               # Datos CSV de ejemplo
│   └── ejemplo_datos.txt               # Datos TXT de ejemplo
│
├── 📁 CÓDIGO FUENTE (src/main/java)
│   └── com/sivco/gestion_archivos/
│       │
│       ├── 🔌 CONTROLADORES (7 archivos)
│       │   ├── MaquinaControlador.java
│       │   ├── EnsayoControlador.java
│       │   ├── AnalisisControlador.java
│       │   ├── ReporteControlador.java
│       │   ├── CargaDatosControlador.java
│       │   ├── ExportacionControlador.java
│       │   └── UtilidadesControlador.java
│       │
│       ├── 💼 SERVICIOS (7 archivos)
│       │   ├── MaquinaServicio.java
│       │   ├── EnsayoServicio.java
│       │   ├── ReporteServicio.java
│       │   ├── AnalisisServicio.java
│       │   ├── CargaDatosServicio.java
│       │   ├── ExcelServicio.java
│       │   └── (GeneradorReporteFinal en utilidades)
│       │
│       ├── 🏛️ MODELOS (8 archivos)
│       │   ├── Maquina.java
│       │   ├── Ensayo.java
│       │   ├── Reporte.java
│       │   ├── DatoEnsayoTemporal.java
│       │   ├── EstadoEnsayo.java (enum)
│       │   ├── TipoReporte.java (enum)
│       │   ├── EstadisticasEnsayo.java (DTO)
│       │   └── ReporteFinal.java (DTO)
│       │
│       ├── 🗄️ REPOSITORIOS (3 archivos)
│       │   ├── MaquinaRepositorio.java
│       │   ├── EnsayoRepositorio.java
│       │   └── ReporteRepositorio.java
│       │
│       ├── 🛡️ EXCEPCIONES (3 archivos)
│       │   ├── EnsayoNoEncontradoException.java
│       │   ├── MaquinaNoEncontradaException.java
│       │   └── ManejadorExcepciones.java
│       │
│       ├── ⚙️ CONFIGURACIÓN (2 archivos)
│       │   ├── CorsConfiguration.java
│       │   └── AppConfiguration.java
│       │
│       ├── 🛠️ UTILIDADES (1 archivo)
│       │   └── GeneradorReporteFinal.java
│       │
│       └── 🚀 PRINCIPAL
│           └── GestionArchivosApplication.java
│
├── 📱 RECURSOS (src/main/resources)
│   ├── application.properties           # Config base
│   ├── application-dev.properties       # Config desarrollo
│   ├── application-prod.properties      # Config producción
│   ├── static/                          # Archivos estáticos
│   └── templates/                       # Templates (si hay)
│
└── 🧪 TESTS (src/test)
    └── java/com/sivco/gestion_archivos/
        └── GestionArchivosApplicationTests.java
```

---

## 📊 Estadísticas del Proyecto

| Componente | Cantidad | Líneas |
|-----------|----------|--------|
| **Controladores** | 7 | ~600 |
| **Servicios** | 7 | ~800 |
| **Modelos** | 8 | ~350 |
| **Repositorios** | 3 | ~150 |
| **Excepciones** | 3 | ~150 |
| **Configuración** | 2 | ~100 |
| **Utilidades** | 1 | ~50 |
| **Principal** | 1 | ~10 |
| **TOTAL JAVA** | **32 archivos** | **~2,100 líneas** |
| **Endpoints REST** | - | **30 endpoints** |
| **Documentación** | 5 archivos | ~2,500 líneas |
| **Configuración/Deploy** | 5 archivos | - |
| **Ejemplos** | 4 archivos | - |

---

## 🚀 Guía de Inicio Rápido

### 1️⃣ Instalación (5 minutos con Docker)

```bash
cd gestion-archivos
docker-compose up -d
sleep 5
curl http://localhost:8080/api/utilidades/salud
```

### 2️⃣ Crear Primera Máquina

```bash
curl -X POST http://localhost:8080/api/maquinas \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Mi Horno",
    "tipo": "Horno",
    "limiteInferior": 20,
    "limiteSuperior": 150,
    "unidadMedida": "°C"
  }'
```

### 3️⃣ Crear Primer Ensayo

```bash
curl -X POST http://localhost:8080/api/ensayos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Mi Primer Ensayo",
    "maquina": {"id": 1},
    "responsable": "Mi Nombre"
  }'
```

### 4️⃣ Registrar Datos

```bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/ensayos/1/datos \
    -H "Content-Type: application/json" \
    -d "{\"valor\": $((40 + RANDOM % 20)), \"fuente\": \"SENSOR_$i\"}"
done
```

### 5️⃣ Ver Estadísticas

```bash
curl http://localhost:8080/api/analisis/ensayo/1
```

### 6️⃣ Finalizar y Exportar

```bash
# Finalizar
curl -X POST http://localhost:8080/api/ensayos/1/finalizar

# Generar reporte
curl -X POST http://localhost:8080/api/reportes/generar/1 \
  -H "Content-Type: application/json" \
  -d '{"tipo": "EXCEL", "generadoPor": "Admin"}'

# Descargar Excel
curl http://localhost:8080/api/exportar/excel/datos/1 -o datos.xlsx
```

---

## 🎓 Rutas de Aprendizaje

### Para Principiantes
1. Leer [README_ES.md](./README_ES.md)
2. Ver [INSTALLATION_GUIDE.md](./INSTALLATION_GUIDE.md)
3. Ejecutar demo.sh
4. Explorar endpoints con Postman

### Para Desarrolladores
1. Revisar estructura en [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)
2. Explorar código en src/main/java
3. Leer comentarios y Javadoc
4. Revisar tests

### Para DevOps
1. Revisar [Dockerfile](./Dockerfile)
2. Revisar [docker-compose.yml](./docker-compose.yml)
3. Revisar [init.sql](./init.sql)
4. Configurar ambientes

### Para QA/Testing
1. Importar [Postman_Collection.json](./Postman_Collection.json)
2. Leer [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
3. Ejecutar [demo.sh](./demo.sh)
4. Usar ejemplo_datos.csv y ejemplo_datos.txt

---

## 🔗 Enlaces Útiles

### Documentación Interna
- [Guía Completa](./README_ES.md)
- [Instalación](./INSTALLATION_GUIDE.md)
- [API Endpoints](./API_DOCUMENTATION.md)
- [Resumen Técnico](./IMPLEMENTATION_SUMMARY.md)

### Herramientas Recomendadas
- [Postman](https://www.postman.com/downloads/) - Testing de API
- [VS Code](https://code.visualstudio.com/) - Editor
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) - IDE
- [MySQL Workbench](https://www.mysql.com/products/workbench/) - BD

### Documentación Externa
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [MySQL Docs](https://dev.mysql.com/doc/)
- [Docker Docs](https://docs.docker.com/)

---

## 💡 Tips & Trucos

### Debugging
```bash
# Ver logs en tiempo real
tail -f logs/application.log

# Activar DEBUG
export SPRING_PROFILES_ACTIVE=debug
mvn spring-boot:run
```

### Database
```bash
# Conectar a MySQL
mysql -u root -p gestion_archivos

# Ver tablas
SHOW TABLES;

# Ver estructura
DESC maquinas;
```

### Maven
```bash
# Limpiar y compilar
mvn clean compile

# Ejecutar sin empaquetar
mvn spring-boot:run

# Generar JAR
mvn clean package
```

---

## ❓ Preguntas Frecuentes

### ¿Cómo cambio el puerto?
En `application.properties`:
```properties
server.port=8081
```

### ¿Cómo conecto a una BD existente?
En `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://tu-servidor:3306/tu_base
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
```

### ¿Cómo activo logs más detallados?
En `application.properties`:
```properties
logging.level.com.sivco.gestion_archivos=DEBUG
```

### ¿Puedo usar en producción?
Sí, usar `application-prod.properties` y Docker Compose.

---

## 📞 Soporte

Si tienes dudas:

1. **Consulta la documentación**: Leer archivos .md correspondientes
2. **Revisa ejemplos**: Ver `demo.sh` y `ejemplo_datos.csv`
3. **Prueba endpoints**: Usar Postman Collection
4. **Verifica logs**: Ver salida de la aplicación

---

## 🎉 ¡Listo para Comenzar!

**Siguiente paso recomendado:**

👉 **[Leer README_ES.md](./README_ES.md)** para una visión general

O si ya quieres empezar:

👉 **[Seguir INSTALLATION_GUIDE.md](./INSTALLATION_GUIDE.md)** para instalar

---

*Última actualización: 24 de Noviembre de 2024*  
*Versión: 1.0.0*  
*Estado: ✅ PRODUCCIÓN*
