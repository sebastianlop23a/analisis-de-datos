# 🚀 GUÍA DE INSTALACIÓN PASO A PASO

## Opción 1: Instalación Rápida con Docker (Recomendado)

### Requisitos Previos
- Docker Desktop instalado y ejecutándose
- Git (opcional, para clonar)

### Pasos

```bash
# 1. Navegar a la carpeta del proyecto
cd ruta/a/gestion-archivos

# 2. Iniciar los contenedores
docker-compose up -d

# 3. Esperar a que MySQL se inicie (2-3 segundos)
sleep 5

# 4. Verificar que todo esté ejecutándose
docker-compose ps

# 5. Acceder a la aplicación
# Abre en tu navegador: http://localhost:8080/api/utilidades/salud
```

**¡Listo!** La aplicación está ejecutándose.

### Para detener:
```bash
docker-compose down
```

---

## Opción 2: Instalación Manual

### Requisitos Previos

#### Windows
1. **Java 17+**
   - Descargar: https://www.oracle.com/java/technologies/downloads/#java17
   - Instalar y establecer JAVA_HOME

2. **Maven 3.6+**
   - Descargar: https://maven.apache.org/download.cgi
   - Descomprimir y agregar a PATH
   - Verificar: `mvn --version`

3. **MySQL 8.0+**
   - Descargar: https://dev.mysql.com/downloads/mysql/
   - Instalar MySQL Server
   - Instalar MySQL Workbench (opcional, para GUI)
   - Verificar: `mysql --version`

#### macOS
```bash
# Con Homebrew
brew install openjdk@17
brew install maven
brew install mysql

# Establecer JAVA_HOME
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-17-jdk
sudo apt install maven
sudo apt install mysql-server

# Verificar instalaciones
java -version
mvn -version
mysql --version
```

### Instalación del Proyecto

#### 1. Crear Base de Datos

Opción A - Con MySQL Workbench:
```sql
-- Ejecutar el archivo init.sql
-- O copiar y pegar en Workbench
```

Opción B - Con terminal:
```bash
# Windows (PowerShell)
mysql -u root -p < init.sql

# macOS/Linux
mysql -u root -p < init.sql
```

Opción C - Manualmente:
```sql
CREATE DATABASE gestion_archivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gestion_archivos;

-- Ejecutar el resto del script SQL...
```

#### 2. Configurar la Aplicación

Editar `src/main/resources/application.properties`:

```properties
# Cambiar según tu instalación MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/gestion_archivos
spring.datasource.username=root
spring.datasource.password=tu_contraseña_aqui
```

#### 3. Compilar el Proyecto

```bash
# Navegar a la carpeta del proyecto
cd gestion-archivos

# Compilar
mvn clean compile

# Instalar dependencias
mvn install
```

#### 4. Ejecutar la Aplicación

**Opción A - Con Maven:**
```bash
mvn spring-boot:run
```

**Opción B - Empaquetar y ejecutar:**
```bash
# Compilar JAR
mvn clean package

# Ejecutar JAR
java -jar target/gestion-archivos-*.jar
```

#### 5. Verificar que está funcionando

```bash
# Abrir en navegador
http://localhost:8080/api/utilidades/salud

# O con curl
curl http://localhost:8080/api/utilidades/info
```

---

## Solución de Problemas

### Error: "Puerto 8080 ya está en uso"

```properties
# En application.properties, cambiar puerto:
server.port=8081
```

### Error: "No puede conectarse a MySQL"

```bash
# Windows - Verificar que MySQL está corriendo
Get-Service MySQL80  # o la versión que tengas

# macOS
brew services list | grep mysql

# Linux
sudo systemctl status mysql
```

```sql
-- Verificar que existe la base de datos
SHOW DATABASES;

-- Si no existe, crearla
CREATE DATABASE gestion_archivos;
```

### Error: "java: command not found"

```bash
# Verificar que Java está instalado
java -version

# Si no funciona, establecer JAVA_HOME
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17

# macOS/Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Error: "mvn: command not found"

```bash
# Agregar Maven a PATH
# Windows: Editar variables de entorno del sistema
# macOS/Linux:
export PATH=$PATH:/ruta/a/maven/bin
```

### Error al importar datos desde archivo

```bash
# Asegurarse que:
# 1. El formato del archivo sea correcto (CSV o TXT)
# 2. Las rutas sean absolutas
# 3. El formato de fechas sea ISO-8601 (YYYY-MM-DDTHH:MM:SS)
```

---

## Testing Inicial

### 1. Verificar Salud del Sistema

```bash
curl http://localhost:8080/api/utilidades/salud
```

Respuesta esperada:
```json
{
  "estado": "OK",
  "mensaje": "Sistema de gestión de ensayos operativo",
  "version": "1.0.0"
}
```

### 2. Crear una Máquina

```bash
curl -X POST http://localhost:8080/api/maquinas \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test Horno",
    "tipo": "Horno",
    "limiteInferior": 20,
    "limiteSuperior": 150,
    "unidadMedida": "°C"
  }'
```

### 3. Crear un Ensayo

```bash
curl -X POST http://localhost:8080/api/ensayos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test Ensayo",
    "maquina": {"id": 1},
    "responsable": "Test User"
  }'
```

### 4. Registrar un Dato

```bash
curl -X POST http://localhost:8080/api/ensayos/1/datos \
  -H "Content-Type: application/json" \
  -d '{"valor": 45.5, "fuente": "TEST"}'
```

---

## Configuración de IDE (VS Code / IntelliJ)

### VS Code

1. Instalar extensiones:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - REST Client

2. Abrir proyecto:
   - File → Open Folder → Seleccionar carpeta

3. Ejecutar:
   - Terminal → Run Task → maven: clean compile
   - O usar Spring Boot Dashboard

### IntelliJ IDEA

1. Open → Seleccionar carpeta del proyecto

2. Esperar a que indexe (puede tomar 1-2 minutos)

3. Run → Run 'GestionArchivosApplication'

---

## Variables de Entorno (Recomendado)

```bash
# Para desarrollo
export SPRING_PROFILES_ACTIVE=dev

# Para producción
export SPRING_PROFILES_ACTIVE=prod

# Específico para BD
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/gestion_archivos
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=tu_contraseña
```

---

## Logs y Debugging

### Habilitar logs detallados

En `application.properties`:
```properties
logging.level.root=DEBUG
logging.level.com.sivco.gestion_archivos=TRACE
```

### Ver logs en tiempo real

```bash
# En Windows (PowerShell)
Get-Content -Path "application.log" -Wait

# En macOS/Linux
tail -f application.log
```

---

## Scripts Útiles

### Iniciar todo desde cero

```bash
#!/bin/bash
echo "Limpiando..."
mvn clean

echo "Compilando..."
mvn compile

echo "Instalando dependencias..."
mvn install

echo "Empaquetando..."
mvn package

echo "Ejecutando..."
java -jar target/gestion-archivos-*.jar
```

### Cargar datos de prueba

```bash
# Esperar a que la app esté lista
sleep 5

# Crear máquina
curl -X POST http://localhost:8080/api/maquinas \
  -H "Content-Type: application/json" \
  -d @- << 'EOF'
{
  "nombre": "Horno Test",
  "tipo": "Horno",
  "limiteInferior": 20,
  "limiteSuperior": 150,
  "unidadMedida": "°C"
}
EOF

echo "Máquina creada!"
```

---

## Configuración de Postman

1. Descargar Postman: https://www.postman.com/downloads/

2. Importar colección:
   - File → Import
   - Seleccionar `Postman_Collection.json`

3. Configurar variables:
   - Environment: New
   - Add variables:
     - baseUrl: http://localhost:8080
     - maquinaId: 1
     - ensayoId: 1

4. ¡Listo para probar!

---

## Próximos Pasos

Después de instalar:

1. Leer [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
2. Revisar [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)
3. Ejecutar `demo.sh` para una demostración completa
4. Explorar los endpoints con Postman o curl

---

## Obtener Ayuda

### Errores Comunes

- **No puede conectar a BD**: Verificar que MySQL está corriendo
- **Puerto en uso**: Cambiar puerto en `application.properties`
- **Dependencias no encontradas**: Ejecutar `mvn clean install`
- **JAVA_HOME no configurado**: Establecer variable de entorno

### Documentación

- [README_ES.md](./README_ES.md) - Visión general
- [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) - Endpoints detallados
- [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - Resumen técnico

---

**¡Listo para comenzar!** 🚀
