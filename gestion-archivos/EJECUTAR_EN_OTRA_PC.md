# 🚀 GESTION ARCHIVOS - Guía de Ejecución en Otra PC

## ⚠️ Solución de Errores de "Get y Set"

Los errores de **GET y SET** (credenciales y configuración) ocurren cuando la aplicación se ejecuta en una PC diferente porque:

- **Host MySQL**: Puede estar en `localhost`, `127.0.0.1`, o una IP diferente
- **Credenciales**: Usuario/contraseña MySQL pueden ser diferentes
- **Puerto**: MySQL puede estar en otro puerto (3306 por defecto)
- **Base de datos**: Nombre y carácter encoding pueden variar

---

## ✅ Pasos para Ejecutar en Otra PC

### 1️⃣ Requisitos Previos
- **Java 17+** instalado
- **Maven 3.6+** en el PATH
- **MySQL 8.0+** corriendo y accesible
- **Base de datos creada**:
  ```sql
  CREATE DATABASE gestion_archivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### 2️⃣ Configurar Variables de Entorno

#### Opción A: Automática (Recomendado)
```powershell
powershell -ExecutionPolicy Bypass -File setup_pc.ps1
```

Esto abrirá un asistente interactivo que:
- Te pedirá datos de conexión MySQL
- Configurará variables de entorno locales
- Opcionalmente guardará los datos de forma permanente

#### Opción B: Manual

Edita `src/main/resources/application.properties`:

```properties
# Para localhost:
spring.datasource.url=jdbc:mysql://localhost:3306/gestion_archivos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=tu_usuario_mysql
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
server.port=8080
```

### 3️⃣ Ejecutar la Aplicación

**Sin Testeos:**
```batch
run.bat
```

Esto:
✓ Crea directorio `correcciones/` si no existe
✓ Compila sin ejecutar tests (`-DskipTests=true`)
✓ Inicia en puerto 8080
✓ Abre automáticamente http://localhost:8080

### 4️⃣ Verificar Conexión

Una vez iniciada, verifica en:
- Navegador: http://localhost:8080
- Logs de error: Revisa la ventana de PowerShell

---

## 🔧 Variables de Entorno (Alternativa Portable)

Si prefieres no modificar archivos, establece variables de entorno:

### Windows - CMD:
```batch
setx SPRING_DATASOURCE_URL "jdbc:mysql://localhost:3306/gestion_archivos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
setx SPRING_DATASOURCE_USERNAME "root"
setx SPRING_DATASOURCE_PASSWORD "tu_contraseña"
setx SERVER_PORT "8080"
```

### Windows - PowerShell:
```powershell
[System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL", "jdbc:mysql://localhost:3306/gestion_archivos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "User")
[System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_USERNAME", "root", "User")
[System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_PASSWORD", "tu_contraseña", "User")
[System.Environment]::SetEnvironmentVariable("SERVER_PORT", "8080", "User")
```

---

## ❌ Solución de Problemas

### Error: "Connection refused" (Puerto 3306)
```
→ MySQL no está corriendo o está en otro puerto
→ Verifica: netstat -ano | findstr :3306
→ Inicia MySQL: mysql -u root -p
```

### Error: "Access denied" (Usuario/Contraseña)
```
→ Credenciales MySQL incorrectas
→ Verifica tu usuario: mysql -u root -p
→ Ejecuta setup_pc.ps1 para reconfigurabilidad
```

### Error: "Unknown database"
```
→ La BD 'gestion_archivos' no existe
→ Crea con: CREATE DATABASE gestion_archivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Error: "Maven not found"
```
→ Maven no está en el PATH
→ Verifica: mvn -version
→ Reinstala Maven o añade su ruta al PATH
```

---

## 📋 Archivo de Configuración

El archivo `application.properties` ahora soporta **variables de entorno con fallback**:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/gestion_archivos...}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

**Prioridad:**
1. Variable de entorno del sistema
2. Valor por defecto en application.properties

---

## 🎯 Resumen Rápido

| Paso | Comando | Notas |
|------|---------|-------|
| 1. Configurar | `powershell -ExecutionPolicy Bypass -File setup_pc.ps1` | Interactivo |
| 2. Crear BD | `CREATE DATABASE gestion_archivos...` | En MySQL CLI |
| 3. Ejecutar | `run.bat` | Sin testeos |
| 4. Abrir | http://localhost:8080 | Navegador |

---

## ✨ Características del Script

✓ **Sin Testeos**: `-DskipTests=true` automático
✓ **Portable**: Valida Maven, crea directorios
✓ **Auto-abre navegador**: En http://localhost:8080
✓ **Manejo de errores**: Detecta problemas de inicio
✓ **Flexible**: Acepta variables de entorno o properties

---

**¿Problemas?** Revisa los logs en la ventana PowerShell o contacta al equipo.
