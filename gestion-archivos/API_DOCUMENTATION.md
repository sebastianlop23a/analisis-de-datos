# Sistema de Gestión de Ensayos de Máquinas

## Descripción

Sistema Spring Boot para gestionar, programar, registrar, analizar y reportar ensayos realizados a diferentes máquinas (hornos, cámaras térmicas, incubadoras, etc.). Los datos se procesan localmente durante el ensayo y solo se guarda el reporte final en la base de datos.

## Características

✅ Gestión de máquinas con límites configurables  
✅ Registro de ensayos en tiempo real  
✅ Almacenamiento temporal de datos en memoria  
✅ Detección automática de valores anormales  
✅ Análisis estadístico completo  
✅ Carga de datos desde archivos (CSV, TXT)  
✅ Exportación a Excel y CSV  
✅ Generación de reportes finales  
✅ Manejo centralizado de excepciones  
✅ CORS habilitado  

## Requisitos

- Java 17+
- MySQL 8.0+
- Spring Boot 3.0+
- Maven 3.6+

## Configuración

### Base de Datos

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gestion_archivos
spring.datasource.username=root
spring.datasource.password=tu_contraseña
```

### Crear la base de datos

```sql
CREATE DATABASE gestion_archivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/sivco/gestion_archivos/
│   │   ├── controladores/
│   │   │   ├── MaquinaControlador.java
│   │   │   ├── EnsayoControlador.java
│   │   │   ├── ReporteControlador.java
│   │   │   ├── AnalisisControlador.java
│   │   │   ├── CargaDatosControlador.java
│   │   │   ├── ExportacionControlador.java
│   │   │   └── UtilidadesControlador.java
│   │   ├── servicios/
│   │   │   ├── MaquinaServicio.java
│   │   │   ├── EnsayoServicio.java
│   │   │   ├── ReporteServicio.java
│   │   │   ├── AnalisisServicio.java
│   │   │   ├── CargaDatosServicio.java
│   │   │   └── ExcelServicio.java
│   │   ├── modelos/
│   │   │   ├── Maquina.java
│   │   │   ├── Ensayo.java
│   │   │   ├── Reporte.java
│   │   │   ├── DatoEnsayoTemporal.java
│   │   │   ├── EstadisticasEnsayo.java
│   │   │   ├── ReporteFinal.java
│   │   │   ├── EstadoEnsayo.java
│   │   │   └── TipoReporte.java
│   │   ├── repositorios/
│   │   │   ├── MaquinaRepositorio.java
│   │   │   ├── EnsayoRepositorio.java
│   │   │   └── ReporteRepositorio.java
│   │   ├── exceptiones/
│   │   │   ├── EnsayoNoEncontradoException.java
│   │   │   ├── MaquinaNoEncontradaException.java
│   │   │   └── ManejadorExcepciones.java
│   │   ├── configuracion/
│   │   │   └── CorsConfiguration.java
│   │   ├── utilidades/
│   │   │   └── GeneradorReporteFinal.java
│   │   └── GestionArchivosApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/...
```

## API Endpoints

### 🏭 Máquinas

#### Crear máquina
```
POST /api/maquinas
Content-Type: application/json

{
  "nombre": "Horno Térmico A1",
  "tipo": "Horno",
  "descripcion": "Horno industrial de precisión",
  "limiteInferior": 20.0,
  "limiteSuperior": 150.0,
  "unidadMedida": "°C",
  "ubicacion": "Laboratorio 1",
  "modelo": "HT-2000",
  "numeroSerie": "SN-001"
}
```

#### Obtener todas las máquinas
```
GET /api/maquinas
```

#### Obtener máquina por ID
```
GET /api/maquinas/{id}
```

#### Obtener máquinas activas
```
GET /api/maquinas/activas
```

#### Obtener máquinas por tipo
```
GET /api/maquinas/tipo/{tipo}
```

#### Actualizar máquina
```
PUT /api/maquinas/{id}
```

#### Eliminar máquina
```
DELETE /api/maquinas/{id}
```

#### Validar valor dentro de rango
```
GET /api/maquinas/{id}/validar/{valor}
```

### 📊 Ensayos

#### Crear ensayo
```
POST /api/ensayos
Content-Type: application/json

{
  "nombre": "Ensayo Temperatura Inicial",
  "maquina": {
    "id": 1
  },
  "descripcion": "Prueba de calibración inicial",
  "responsable": "Juan Pérez"
}
```

#### Obtener todos los ensayos
```
GET /api/ensayos
```

#### Obtener ensayo por ID
```
GET /api/ensayos/{id}
```

#### Obtener ensayos por estado
```
GET /api/ensayos/estado/{estado}
```

Valores válidos: EN_PROGRESO, COMPLETADO, PAUSADO, CANCELADO, REPORTE_GENERADO

#### Obtener ensayos por máquina
```
GET /api/ensayos/maquina/{maquinaId}
```

#### Registrar dato
```
POST /api/ensayos/{ensayoId}/datos
Content-Type: application/json

{
  "valor": 45.5,
  "fuente": "SENSOR_A"
}
```

#### Obtener datos temporales
```
GET /api/ensayos/{ensayoId}/datos-temporales
```

#### Pausar ensayo
```
POST /api/ensayos/{ensayoId}/pausar
```

#### Reanudar ensayo
```
POST /api/ensayos/{ensayoId}/reanudar
```

#### Cancelar ensayo
```
POST /api/ensayos/{ensayoId}/cancelar
```

#### Finalizar ensayo
```
POST /api/ensayos/{ensayoId}/finalizar
```

### 📈 Análisis

#### Obtener estadísticas del ensayo
```
GET /api/analisis/ensayo/{ensayoId}
```

Respuesta:
```json
{
  "media": 45.5,
  "desviacionEstandar": 2.3,
  "maximo": 52.1,
  "minimo": 38.9,
  "rango": 13.2,
  "coeficienteVariacion": 5.05,
  "totalDatos": 100,
  "datosAnormales": 3,
  "porcentajeAnormales": 3.0
}
```

#### Obtener datos anormales
```
GET /api/analisis/ensayo/{ensayoId}/anormales
```

#### Obtener resumen
```
GET /api/analisis/ensayo/{ensayoId}/resumen
```

### 📤 Carga de Datos

#### Cargar datos desde CSV
```
POST /api/carga/csv/{ensayoId}
Content-Type: multipart/form-data

archivo: [archivo.csv]
```

Formato esperado del CSV:
```
Timestamp,Valor,Fuente
2024-11-24T10:30:00,45.5,SENSOR_A
2024-11-24T10:31:00,46.2,SENSOR_A
```

#### Cargar datos desde TXT
```
POST /api/carga/txt/{ensayoId}
Content-Type: multipart/form-data

archivo: [archivo.txt]
```

Formato esperado del TXT:
```
2024-11-24 10:30:00 45.5 SENSOR_A
2024-11-24 10:31:00 46.2 SENSOR_A
```

### 💾 Exportación

#### Exportar datos a Excel
```
GET /api/exportar/excel/datos/{ensayoId}
```

#### Exportar reporte a Excel
```
GET /api/exportar/excel/reporte/{ensayoId}
```

#### Exportar datos a CSV
```
GET /api/exportar/csv/{ensayoId}
```

### 📋 Reportes

#### Generar reporte final
```
POST /api/reportes/generar/{ensayoId}
Content-Type: application/json

{
  "tipo": "EXCEL",
  "generadoPor": "Usuario Admin"
}
```

Tipos válidos: PDF, EXCEL, HTML, CSV

#### Obtener reporte
```
GET /api/reportes/ensayo/{ensayoId}
```

### 🛠️ Utilidades

#### Obtener reporte completo
```
GET /api/utilidades/reporte-completo/{ensayoId}
```

#### Verificar salud del sistema
```
GET /api/utilidades/salud
```

#### Obtener información del sistema
```
GET /api/utilidades/info
```

## Flujo de Uso

1. **Crear máquina** con límites de operación
2. **Crear ensayo** vinculado a una máquina
3. **Registrar datos** durante el ensayo (en memoria)
4. **Monitorear** con análisis en tiempo real
5. **Pausar/Reanudar** según sea necesario
6. **Finalizar** el ensayo (calcula estadísticas)
7. **Generar reporte** final (se guarda en BD)
8. **Exportar** datos en Excel o CSV

## Ejemplo de Flujo Completo

```bash
# 1. Crear máquina
curl -X POST http://localhost:8080/api/maquinas \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Horno A1",
    "tipo": "Horno",
    "limiteInferior": 20,
    "limiteSuperior": 150,
    "unidadMedida": "°C"
  }'

# 2. Crear ensayo
curl -X POST http://localhost:8080/api/ensayos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ensayo Inicial",
    "maquina": {"id": 1},
    "responsable": "Juan"
  }'

# 3. Registrar datos
curl -X POST http://localhost:8080/api/ensayos/1/datos \
  -H "Content-Type: application/json" \
  -d '{"valor": 45.5, "fuente": "SENSOR_A"}'

# 4. Obtener estadísticas
curl http://localhost:8080/api/analisis/ensayo/1

# 5. Finalizar ensayo
curl -X POST http://localhost:8080/api/ensayos/1/finalizar

# 6. Generar reporte
curl -X POST http://localhost:8080/api/reportes/generar/1 \
  -H "Content-Type: application/json" \
  -d '{"tipo": "EXCEL", "generadoPor": "Admin"}'

# 7. Descargar Excel
curl http://localhost:8080/api/exportar/excel/datos/1 \
  -o datos_ensayo.xlsx
```

## Códigos de Respuesta HTTP

- **200 OK** - Solicitud exitosa
- **201 Created** - Recurso creado
- **204 No Content** - Sin contenido
- **400 Bad Request** - Solicitud inválida
- **404 Not Found** - Recurso no encontrado
- **500 Internal Server Error** - Error del servidor

## Manejo de Errores

El sistema retorna errores en formato JSON:

```json
{
  "error": "Descripción del error",
  "tipo": "TipoDeError"
}
```

## Versiones de Dependencias

- Spring Boot: 3.0+
- Java: 17+
- MySQL Connector: 8.0+
- Apache POI: 5.2.5+
- Lombok: 1.18+

## Desarrollo

### Compilar
```bash
mvn clean compile
```

### Ejecutar tests
```bash
mvn test
```

### Empaquetar
```bash
mvn clean package
```

### Ejecutar
```bash
mvn spring-boot:run
```

O después de empaquetar:
```bash
java -jar target/gestion-archivos-0.0.1-SNAPSHOT.jar
```

## Notas Importantes

- Los datos temporales se almacenan en memoria durante el ensayo
- Solo se guardan en BD el ensayo y el reporte final
- Los datos temporales se eliminan automáticamente al finalizar
- La detección de anomalías se basa en los límites de la máquina
- Los reportes pueden ser exportados antes de ser eliminados

## Soporte

Para reportar bugs o solicitar features, crear un issue en el repositorio.

## Licencia

Este proyecto es propiedad de SIVCO.
