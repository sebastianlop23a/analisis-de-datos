@echo off
setlocal enabledelayedexpansion

REM ===================================
REM GESTION ARCHIVOS - RUN SCRIPT
REM ===================================
REM Ejecuta la aplicacion SIN testeos
REM Compatible con multiples PCs

cd /d "%~dp0"

echo.
echo ===================================
echo GESTION ARCHIVOS - Iniciando...
echo ===================================
echo.

REM Crear directorio de correcciones si no existe
if not exist "correcciones" (
    echo [INFO] Creando directorio de correcciones...
    mkdir correcciones
)

REM Verificar si Maven esta disponible
where mvn.cmd >nul 2>&1
if errorlevel 1 (
    echo [ERROR] mvn.cmd no encontrado en PATH
    echo [INFO] Asegúrate de tener Maven instalado y en el PATH del sistema
    echo.
    pause
    exit /b 1
)

REM Compilar y ejecutar la aplicacion
echo [INFO] Compilando y ejecutando la aplicacion...
echo [INFO] URL Base: http://localhost:8080
echo.

start "Gestion Archivos" cmd /k ^
mvn.cmd clean package -DskipTests=true -q -f "%~dp0" spring-boot:run ^
-Dspring-boot.run.arguments="--server.port=8080"

REM Esperar a que la aplicacion inicie (maxino 40 intentos x 2 segundos = 80 segundos)
echo [INFO] Esperando que la aplicacion inicie...
set RETRY_COUNT=0
set MAX_RETRIES=40

:RETRY
if %RETRY_COUNT% geq %MAX_RETRIES% (
    echo [WARN] Tiempo de espera agotado, pero puede que la aplicacion este iniciandose...
    echo [INFO] Verifica http://localhost:8080 en tu navegador
    goto :FIN
)

timeout /t 2 /nobreak >nul

REM Intenta acceder a la aplicacion
curl -s http://localhost:8080 >nul 2>&1
if not errorlevel 1 (
    echo [SUCCESS] Aplicacion iniciada correctamente!
    echo [INFO] Abriendo http://localhost:8080 en el navegador...
    start "" http://localhost:8080
    goto :FIN
)

set /a RETRY_COUNT+=1
goto RETRY

:FIN
echo.
echo ===================================
echo La aplicacion se esta ejecutando
echo URL: http://localhost:8080
echo Para detener: Cierra la ventana de la aplicacion
echo ===================================
echo.
pause
