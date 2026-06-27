@echo off
setlocal

cd /d "%~dp0"

echo Iniciando la aplicacion...
start "Gestion Archivos" cmd /k "cd /d ""%~dp0"" && mvn.cmd -q -f gestion-archivos spring-boot:run -Dmaven.test.skip=true"

for /l %%i in (1,1,40) do (
    timeout /t 2 /nobreak >nul
    curl -s http://localhost:8080 >nul 2>&1
    if not errorlevel 1 (
        start "" http://localhost:8080
        goto :fin
    )
)

:fin
echo.
echo La aplicacion se esta iniciando en una nueva ventana.
pause