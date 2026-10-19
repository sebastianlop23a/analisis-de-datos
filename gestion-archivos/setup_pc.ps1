# ===================================
# SETUP DE CONFIGURACION PARA OTRA PC
# ===================================
# Este script configura la aplicacion para ejecutarse en otra PC
# Ejecutar con: powershell -ExecutionPolicy Bypass -File setup_pc.ps1

param(
    [string]$DBHost = "localhost",
    [int]$DBPort = 3306,
    [string]$DBName = "gestion_archivos",
    [string]$DBUser = "root",
    [string]$DBPassword,
    [int]$AppPort = 8080
)

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "SETUP - Configuracion para otra PC" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# Si no se proporcionan parametros, pedir al usuario
if ($DBHost -eq "localhost" -or [string]::IsNullOrEmpty($DBPassword)) {
    Write-Host "Configura la conexion a MySQL:" -ForegroundColor Yellow
    Write-Host ""

    $DBHost = Read-Host "Host MySQL (default: localhost)"
    if ([string]::IsNullOrEmpty($DBHost)) { $DBHost = "localhost" }

    $DBPort = Read-Host "Puerto MySQL (default: 3306)"
    if ([string]::IsNullOrEmpty($DBPort)) { $DBPort = 3306 } else { $DBPort = [int]$DBPort }

    $DBName = Read-Host "Nombre BD (default: gestion_archivos)"
    if ([string]::IsNullOrEmpty($DBName)) { $DBName = "gestion_archivos" }

    $DBUser = Read-Host "Usuario MySQL (default: root)"
    if ([string]::IsNullOrEmpty($DBUser)) { $DBUser = "root" }

    Write-Host "Contraseña MySQL: " -ForegroundColor Yellow -NoNewline
    $secure = Read-Host -AsSecureString
    $DBPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($secure))

    Write-Host ""
    $AppPort = Read-Host "Puerto Aplicacion (default: 8080)"
    if ([string]::IsNullOrEmpty($AppPort)) { $AppPort = 8080 } else { $AppPort = [int]$AppPort }
}

# Guardar en variables de entorno del sistema
Write-Host ""
Write-Host "Configurando variables de entorno..." -ForegroundColor Yellow

$DBUrl = "jdbc:mysql://${DBHost}:${DBPort}/${DBName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000&socketTimeout=10000"

# Variables de entorno para esta sesion
$env:SPRING_DATASOURCE_URL = $DBUrl
$env:SPRING_DATASOURCE_USERNAME = $DBUser
$env:SPRING_DATASOURCE_PASSWORD = $DBPassword
$env:SERVER_PORT = $AppPort

Write-Host "Variables configuradas:" -ForegroundColor Green
Write-Host "  - SPRING_DATASOURCE_URL: $DBUrl"
Write-Host "  - SPRING_DATASOURCE_USERNAME: $DBUser"
Write-Host "  - SERVER_PORT: $AppPort"
Write-Host ""

# Opcion de guardar en el registro de Windows (permanente)
$SavePermanent = Read-Host "Guardar estas variables de entorno de forma permanente en el sistema? (s/n)"
if ($SavePermanent -eq "s" -or $SavePermanent -eq "S") {
    try {
        [System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL", $DBUrl, "User")
        [System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_USERNAME", $DBUser, "User")
        [System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_PASSWORD", $DBPassword, "User")
        [System.Environment]::SetEnvironmentVariable("SERVER_PORT", $AppPort, "User")
        Write-Host "Variables guardadas permanentemente en el sistema" -ForegroundColor Green
    } catch {
        Write-Host "Error al guardar variables: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Setup completado!" -ForegroundColor Green
Write-Host ""
Write-Host "Proximos pasos:" -ForegroundColor Yellow
Write-Host "1. Asegurate de que MySQL este corriendo en $DBHost`:$DBPort"
Write-Host "2. Crea la BD: CREATE DATABASE $DBName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Write-Host "3. Ejecuta: run.bat"
Write-Host ""
