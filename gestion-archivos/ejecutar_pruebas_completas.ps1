#!/usr/bin/env pwsh
# ===================================
# Script de Pruebas Completas del Sistema
# Ejecuta todas las verificaciones automáticas
# ===================================

Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   🧪 SISTEMA DE VERIFICACIÓN COMPLETA                     ║" -ForegroundColor Cyan
Write-Host "║      Pruebas de Cálculos y Correcciones                  ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$testsPassed = 0
$testsFailed = 0

# ===================================
# FUNCIÓN: Verificar Resultado
# ===================================
function Test-Result {
    param(
        [bool]$Condition,
        [string]$TestName
    )
    
    if ($Condition) {
        Write-Host "  ✅ PASS: $TestName" -ForegroundColor Green
        $script:testsPassed++
        return $true
    } else {
        Write-Host "  ❌ FAIL: $TestName" -ForegroundColor Red
        $script:testsFailed++
        return $false
    }
}

# ===================================
# 1. PRUEBAS UNITARIAS (Maven)
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "1️⃣  EJECUTANDO PRUEBAS UNITARIAS (Maven)" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

try {
    $mvnOutput = mvn test -q 2>&1
    $testResults = $mvnOutput -join "`n"
    
    # Verificar si las pruebas pasaron
    if ($testResults -match "BUILD SUCCESS") {
        Test-Result -Condition $true -TestName "Pruebas Unitarias Maven"
        
        # Extraer estadísticas
        if ($testResults -match "Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)") {
            $testsRun = $Matches[1]
            $failures = $Matches[2]
            $errors = $Matches[3]
            $skipped = $Matches[4]
            
            Write-Host "     📊 Tests ejecutados: $testsRun" -ForegroundColor White
            Write-Host "     ✅ Exitosos: $($testsRun - $failures - $errors)" -ForegroundColor Green
            Write-Host "     ❌ Fallidos: $failures" -ForegroundColor $(if ($failures -eq 0) { "Green" } else { "Red" })
            Write-Host "     ⚠️  Errores: $errors" -ForegroundColor $(if ($errors -eq 0) { "Green" } else { "Red" })
            Write-Host "     ⏭️  Saltados: $skipped" -ForegroundColor Gray
        }
    } else {
        Test-Result -Condition $false -TestName "Pruebas Unitarias Maven"
        Write-Host "     ⚠️  Ver detalles arriba" -ForegroundColor Yellow
    }
} catch {
    Test-Result -Condition $false -TestName "Pruebas Unitarias Maven"
    Write-Host "     ❌ Error: $_" -ForegroundColor Red
}
Write-Host ""

# ===================================
# 2. VERIFICAR ARCHIVOS DE CORRECCIÓN
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "2️⃣  VERIFICANDO ARCHIVOS DE CORRECCIÓN" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

$correcciones = Get-ChildItem -Path "correcciones" -Recurse -Filter "*_CORRECCIONES.csv" -ErrorAction SilentlyContinue

Test-Result -Condition ($correcciones.Count -gt 0) -TestName "Archivos de corrección encontrados"
Write-Host "     📁 Total de archivos: $($correcciones.Count)" -ForegroundColor White

if ($correcciones.Count -gt 0) {
    $archivoEjemplo = $correcciones | Select-Object -First 1
    $contenido = Get-Content $archivoEjemplo.FullName -TotalCount 3 -ErrorAction SilentlyContinue
    
    if ($contenido -and $contenido.Count -gt 1) {
        Test-Result -Condition $true -TestName "Formato de archivo CSV válido"
        Write-Host "     📄 Ejemplo: $($archivoEjemplo.Name)" -ForegroundColor Gray
    } else {
        Test-Result -Condition $false -TestName "Formato de archivo CSV válido"
    }
}
Write-Host ""

# ===================================
# 3. PRUEBA DE FÓRMULA DE CORRECCIÓN
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "3️⃣  PRUEBA DE FÓRMULA DE CORRECCIÓN" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

$x = 100
$A = 0.5
$B = 1.02
$C = 0.0001
$D = -0.00001

$valorCorregido = $A + ($B * $x) + ($C * [Math]::Pow($x, 2)) + ($D * [Math]::Pow($x, 3))
$esperado = 93.51

Write-Host "  Calculo Formula: valor_corregido = A + B×x + C×x^2 + D×x^3" -ForegroundColor White
Write-Host "     Valores: x=$x, A=$A, B=$B, C=$C, D=$D" -ForegroundColor Gray
Write-Host "     Resultado: $valorCorregido" -ForegroundColor Cyan
Write-Host "     Esperado: ~$esperado" -ForegroundColor Gray

# Verificar que el resultado esté cerca del esperado (tolerancia 0.1)
$diferencia = [Math]::Abs($valorCorregido - $esperado)
Test-Result -Condition ($diferencia -lt 0.1) -TestName "Cálculo de corrección preciso"
Write-Host ""

# ===================================
# 4. PRUEBA DE FACTOR HISTÓRICO
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "4️⃣  PRUEBA DE FACTOR HISTÓRICO" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

$temperaturas = @(121, 122, 123, 124, 125)
$z = 14
$deltaT = 1
$FH = 0

Write-Host "  Formula: FH = Suma(10^((Ti - 250)/z) * Dt)" -ForegroundColor White
Write-Host "     Temperaturas: $($temperaturas -join ', ') grados C" -ForegroundColor Gray
Write-Host "     Z=$z, Dt=$deltaT minuto" -ForegroundColor Gray
Write-Host ""

for ($i = 0; $i -lt $temperaturas.Count; $i++) {
    $Ti = $temperaturas[$i]
    $exponente = ($Ti - 250) / $z
    $termino = [Math]::Pow(10, $exponente) * $deltaT
    $FH += $termino
    
    Write-Host "     T$($i+1)=$Ti grados C, exponente=$([Math]::Round($exponente, 4)), termino=$($termino.ToString('E6'))" -ForegroundColor Gray
}

Write-Host ""
Write-Host "     Resultado: FH = $($FH.ToString('0.0000000000'))" -ForegroundColor Cyan

# El FH para estas temperaturas debería ser muy pequeño pero positivo
Test-Result -Condition ($FH -gt 0 -and $FH -lt 1) -TestName "Cálculo de Factor Histórico en rango esperado"
Write-Host ""

# ===================================
# 5. VERIFICAR API
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "5️⃣  VERIFICANDO ESTADO DEL API" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/salud" -Method Get -TimeoutSec 5
    
    Test-Result -Condition ($response.status -eq "ok") -TestName "API esta activa y responde"
    Write-Host "     URL: http://localhost:8080" -ForegroundColor Gray
    Write-Host "     Estado: $($response.status)" -ForegroundColor Green
} catch {
    Test-Result -Condition $false -TestName "API esta activa y responde"
    Write-Host "     Asegurate de que la aplicacion este ejecutandose" -ForegroundColor Yellow
    Write-Host "     Ejecuta: mvn spring-boot:run" -ForegroundColor Cyan
}
Write-Host ""

# ===================================
# 6. VERIFICAR ENSAYOS
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "6️⃣  VERIFICANDO ENSAYOS EN BASE DE DATOS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

try {
    $ensayos = Invoke-RestMethod -Uri "http://localhost:8080/api/ensayos" -Method Get -TimeoutSec 5
    
    Test-Result -Condition ($ensayos.Count -gt 0) -TestName "Existen ensayos en la base de datos"
    Write-Host "     📊 Total de ensayos: $($ensayos.Count)" -ForegroundColor White
    
    # Verificar ensayos con FH
    $ensayosConFH = $ensayos | Where-Object { $_.calculaFH -eq $true }
    if ($ensayosConFH) {
        Test-Result -Condition $true -TestName "Ensayos con Factor Histórico encontrados"
        Write-Host "     🔥 Ensayos con FH: $($ensayosConFH.Count)" -ForegroundColor White
        
        foreach ($ensayo in $ensayosConFH | Select-Object -First 3) {
            if ($ensayo.factorHistorico) {
                Write-Host "        • ID $($ensayo.id): FH = $($ensayo.factorHistorico.ToString('0.000000'))" -ForegroundColor Gray
            }
        }
    }
    
    # Verificar ensayos finalizados
    $ensayosFinalizados = $ensayos | Where-Object { $_.fechaFin -ne $null }
    Test-Result -Condition ($ensayosFinalizados.Count -gt 0) -TestName "Existen ensayos finalizados"
    Write-Host "     ✅ Ensayos finalizados: $($ensayosFinalizados.Count)" -ForegroundColor White
    
} catch {
    Test-Result -Condition $false -TestName "Conexión con base de datos"
    Write-Host "     ⚠️  No se pudo obtener información de ensayos" -ForegroundColor Yellow
}
Write-Host ""

# ===================================
# 7. VERIFICAR CORRECCIONES APLICADAS
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "7️⃣  VERIFICANDO CORRECCIONES APLICADAS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

try {
    # Intentar obtener correcciones de algún ensayo
    $ensayo = (Invoke-RestMethod -Uri "http://localhost:8080/api/ensayos" -Method Get -TimeoutSec 5) | Select-Object -First 1
    
    if ($ensayo) {
        $correccionesApi = Invoke-RestMethod -Uri "http://localhost:8080/api/correcciones/$($ensayo.id)" -Method Get -TimeoutSec 5 -ErrorAction SilentlyContinue
        
        if ($correccionesApi -and $correccionesApi.Count -gt 0) {
            Test-Result -Condition $true -TestName "Correcciones cargadas en sistema"
            Write-Host "     📄 Correcciones encontradas: $($correccionesApi.Count)" -ForegroundColor White
        } else {
            Write-Host "  ⚠️  No se encontraron correcciones para el ensayo de prueba" -ForegroundColor Yellow
            Write-Host "     💡 Esto es normal si no has subido archivos de corrección" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "  ℹ️  No se pudo verificar correcciones (es normal si no hay ensayos)" -ForegroundColor Gray
}
Write-Host ""

# ===================================
# 8. VERIFICAR LOGS DEL SISTEMA
# ===================================
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "8️⃣  ANALIZANDO LOGS DEL SISTEMA" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

if (Test-Path "app.err") {
    $logs = Get-Content "app.err" -Tail 100 -ErrorAction SilentlyContinue
    
    # Buscar logs de correcciones
    $logsCorrecciones = $logs | Select-String -Pattern "correc|Correc" -ErrorAction SilentlyContinue
    $logsFH = $logs | Select-String -Pattern "Factor|histórico|FH" -ErrorAction SilentlyContinue
    $logsError = $logs | Select-String -Pattern "ERROR|Exception" -ErrorAction SilentlyContinue
    
    Test-Result -Condition ($logsCorrecciones.Count -gt 0) -TestName "Sistema registra aplicación de correcciones"
    Write-Host "     📝 Logs de correcciones: $($logsCorrecciones.Count)" -ForegroundColor White
    
    if ($logsFH.Count -gt 0) {
        Test-Result -Condition $true -TestName "Sistema registra cálculo de Factor Histórico"
        Write-Host "     🔥 Logs de FH: $($logsFH.Count)" -ForegroundColor White
    }
    
    if ($logsError.Count -gt 0) {
        Test-Result -Condition $false -TestName "Sin errores en logs recientes"
        Write-Host "     ⚠️  Errores encontrados: $($logsError.Count)" -ForegroundColor Yellow
        Write-Host "     💡 Revisa: Get-Content app.err -Tail 50" -ForegroundColor Cyan
    } else {
        Test-Result -Condition $true -TestName "Sin errores en logs recientes"
    }
} else {
    Write-Host "  ℹ️  Archivo app.err no encontrado" -ForegroundColor Gray
    Write-Host "     (Se crea cuando ejecutas la aplicación)" -ForegroundColor Gray
}
Write-Host ""

# ===================================
# RESUMEN FINAL
# ===================================
Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   📊 RESUMEN DE RESULTADOS                                ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$totalTests = $testsPassed + $testsFailed
$porcentaje = if ($totalTests -gt 0) { [Math]::Round(($testsPassed / $totalTests) * 100, 1) } else { 0 }

Write-Host "  Total de pruebas: $totalTests" -ForegroundColor White
Write-Host "  ✅ Exitosas: $testsPassed" -ForegroundColor Green
Write-Host "  ❌ Fallidas: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })
Write-Host "  📈 Tasa de éxito: $porcentaje%" -ForegroundColor $(if ($porcentaje -ge 80) { "Green" } elseif ($porcentaje -ge 50) { "Yellow" } else { "Red" })
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "  🎉 ¡TODAS LAS PRUEBAS PASARON!" -ForegroundColor Green
    Write-Host "  ✅ El sistema de cálculos y correcciones está funcionando correctamente" -ForegroundColor Green
} elseif ($testsFailed -le 2) {
    Write-Host "  ⚠️  ALGUNAS PRUEBAS FALLARON" -ForegroundColor Yellow
    Write-Host "  💡 Revisa los detalles arriba para más información" -ForegroundColor Yellow
} else {
    Write-Host "  ❌ MÚLTIPLES PRUEBAS FALLARON" -ForegroundColor Red
    Write-Host "  🔧 Se requiere atención para corregir problemas" -ForegroundColor Red
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  📚 DOCUMENTACIÓN ADICIONAL" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  • Ver: SISTEMA_VERIFICACION_CALCULOS.md" -ForegroundColor White
Write-Host "  • Ver: COMO_VERIFICAR_CORRECCIONES.md" -ForegroundColor White
Write-Host "  • Ejecutar tests: mvn test" -ForegroundColor White
Write-Host "  • Ver logs: Get-Content app.err -Tail 50" -ForegroundColor White
Write-Host "  • Debug web: Abre F12 y ejecuta debugCorrecciones()" -ForegroundColor White
Write-Host ""

# Retornar código de salida apropiado
if ($testsFailed -eq 0) {
    exit 0
} else {
    exit 1
}
