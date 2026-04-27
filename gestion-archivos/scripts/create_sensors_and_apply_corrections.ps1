# Script: create_sensors_and_apply_corrections.ps1
# Creates 12 sensors and uploads a simple 2-point calibration CSV for each.
# Usage: Run this while the application is running (default http://localhost:8080)

$baseUrl = "http://localhost:8080"

function Create-Sensor($code, $ubicacion, $tipo, $modelo) {
    $payload = @{
        codigo = $code
        ubicacion = $ubicacion
        tipoSonda = $tipo
        modelo = $modelo
    } | ConvertTo-Json

    try {
        $resp = Invoke-RestMethod -Uri "$baseUrl/api/sensores" -Method Post -Body $payload -ContentType 'application/json'
        Write-Host "Created sensor: $($resp.id) - $($resp.codigo)"
        return $resp
    } catch {
        Write-Warning ("Failed to create sensor " + $code + ": " + $_)
        return $null
    }
}

function Upload-Calibration($sensorId, $filePath, $descripcion, $subidoPor) {
    try {
        $bytes = [System.IO.File]::ReadAllBytes($filePath)
        $content = New-Object System.Net.Http.MultipartFormDataContent
        $fileContent = New-Object System.Net.Http.ByteArrayContent($bytes)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/csv")
        $content.Add($fileContent, "archivo", [System.IO.Path]::GetFileName($filePath))
        if ($descripcion) { $content.Add((New-Object System.Net.Http.StringContent($descripcion)), "descripcion") }
        if ($subidoPor) { $content.Add((New-Object System.Net.Http.StringContent($subidoPor)), "subidoPor") }

        $client = New-Object System.Net.Http.HttpClient
        $resp = $client.PostAsync("$baseUrl/api/sensores/$sensorId/recalibrar", $content).Result
        if ($resp.IsSuccessStatusCode) {
            $body = $resp.Content.ReadAsStringAsync().Result
            Write-Host "Uploaded calibration for sensor $sensorId -> response: $body"
            return $body | ConvertFrom-Json
        } else {
            Write-Warning ("Failed to upload calibration for sensor " + $sensorId + ": " + $resp.StatusCode + " - " + $resp.ReasonPhrase)
            return $null
        }
    } catch {
        Write-Warning ("Failed to upload calibration for sensor " + $sensorId + ": " + $_)
        return $null
    }
}

# Main
$sensors = @()
for ($i = 1; $i -le 12; $i++) {
    $code = "sensor_${i}"
    $ubicacion = "Ubicacion $i"
    $tipo = "PT100"
    $modelo = "Model-$i"

    $s = Create-Sensor -code $code -ubicacion $ubicacion -tipo $tipo -modelo $modelo
    if ($s -ne $null) { $sensors += $s }
}

# Create a temp folder for calibration files
$calibDir = Join-Path $PSScriptRoot "temp_calibrations"
if (-Not (Test-Path $calibDir)) { New-Item -ItemType Directory -Path $calibDir | Out-Null }

foreach ($s in $sensors) {
    $id = $s.id
    $fname = "calib_${($s.codigo)}.csv"
    $fpath = Join-Path $calibDir $fname
    # Simple two-point CSV: reference,instrument
    $content = @(
        "reference,instrument",
        "0,0",
        "100,100"
    ) -join "`n"
    Set-Content -Path $fpath -Value $content -Encoding UTF8

    Upload-Calibration -sensorId $id -filePath $fpath -descripcion "Auto calibración inicial" -subidoPor "script"
}

Write-Host "Done. Created $($sensors.Count) sensors and attempted calibrations."
