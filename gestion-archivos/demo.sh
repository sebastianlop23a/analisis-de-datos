#!/bin/bash

# Script de demostración del Sistema de Gestión de Ensayos
# Este script muestra un flujo completo de uso del sistema

API_URL="http://localhost:8080/api"
HEADER_JSON="Content-Type: application/json"

echo "=========================================="
echo "Sistema de Gestión de Ensayos - Demo"
echo "=========================================="
echo ""

# 1. CREAR MÁQUINA
echo "1️⃣ Creando máquina..."
MAQUINA_RESPONSE=$(curl -s -X POST "$API_URL/maquinas" \
  -H "$HEADER_JSON" \
  -d '{
    "nombre": "Horno Demo",
    "tipo": "Horno Industrial",
    "descripcion": "Horno para demostración",
    "limiteInferior": 20.0,
    "limiteSuperior": 150.0,
    "unidadMedida": "°C",
    "ubicacion": "Laboratorio Demo",
    "modelo": "HT-DEMO",
    "numeroSerie": "SN-DEMO-001"
  }')

MAQUINA_ID=$(echo $MAQUINA_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "✅ Máquina creada con ID: $MAQUINA_ID"
echo ""

# 2. CREAR ENSAYO
echo "2️⃣ Creando ensayo..."
ENSAYO_RESPONSE=$(curl -s -X POST "$API_URL/ensayos" \
  -H "$HEADER_JSON" \
  -d "{
    \"nombre\": \"Ensayo de Demostración\",
    \"maquina\": {\"id\": $MAQUINA_ID},
    \"descripcion\": \"Prueba del sistema completo\",
    \"responsable\": \"Demo User\"
  }")

ENSAYO_ID=$(echo $ENSAYO_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "✅ Ensayo creado con ID: $ENSAYO_ID"
echo ""

# 3. REGISTRAR DATOS
echo "3️⃣ Registrando datos del ensayo..."
for i in {1..10}; do
  VALOR=$(echo "scale=2; 45 + $RANDOM % 20" | bc)
  curl -s -X POST "$API_URL/ensayos/$ENSAYO_ID/datos" \
    -H "$HEADER_JSON" \
    -d "{\"valor\": $VALOR, \"fuente\": \"SENSOR_$i\"}" > /dev/null
  echo "   Registrado: $VALOR°C"
done
echo "✅ 10 datos registrados"
echo ""

# 4. OBTENER ESTADÍSTICAS
echo "4️⃣ Obteniendo estadísticas..."
curl -s -X GET "$API_URL/analisis/ensayo/$ENSAYO_ID" \
  -H "$HEADER_JSON" | python -m json.tool
echo ""

# 5. OBTENER DATOS ANORMALES
echo "5️⃣ Verificando datos anormales..."
ANORMALES=$(curl -s -X GET "$API_URL/analisis/ensayo/$ENSAYO_ID/anormales" \
  -H "$HEADER_JSON")
echo "Datos anormales encontrados:"
echo $ANORMALES | python -m json.tool
echo ""

# 6. OBTENER RESUMEN
echo "6️⃣ Resumen del ensayo..."
curl -s -X GET "$API_URL/analisis/ensayo/$ENSAYO_ID/resumen" \
  -H "$HEADER_JSON" | python -m json.tool
echo ""

# 7. FINALIZAR ENSAYO
echo "7️⃣ Finalizando ensayo..."
curl -s -X POST "$API_URL/ensayos/$ENSAYO_ID/finalizar" \
  -H "$HEADER_JSON" | python -m json.tool
echo "✅ Ensayo finalizado"
echo ""

# 8. GENERAR REPORTE
echo "8️⃣ Generando reporte..."
curl -s -X POST "$API_URL/reportes/generar/$ENSAYO_ID" \
  -H "$HEADER_JSON" \
  -d '{"tipo": "EXCEL", "generadoPor": "Demo User"}' | python -m json.tool
echo "✅ Reporte generado"
echo ""

# 9. OBTENER REPORTE COMPLETO
echo "9️⃣ Obteniendo reporte completo..."
curl -s -X GET "$API_URL/utilidades/reporte-completo/$ENSAYO_ID" \
  -H "$HEADER_JSON" | python -m json.tool
echo ""

# 10. EXPORTAR A EXCEL
echo "🔟 Exportando datos a Excel..."
curl -s -X GET "$API_URL/exportar/excel/datos/$ENSAYO_ID" \
  -H "$HEADER_JSON" \
  -o "datos_ensayo_$ENSAYO_ID.xlsx"
echo "✅ Archivo descargado: datos_ensayo_$ENSAYO_ID.xlsx"
echo ""

# 11. EXPORTAR A CSV
echo "Exportando datos a CSV..."
curl -s -X GET "$API_URL/exportar/csv/$ENSAYO_ID" \
  -H "$HEADER_JSON" \
  -o "datos_ensayo_$ENSAYO_ID.csv"
echo "✅ Archivo descargado: datos_ensayo_$ENSAYO_ID.csv"
echo ""

echo "=========================================="
echo "✅ Demo completada exitosamente!"
echo "=========================================="
echo ""
echo "Resumen:"
echo "- Máquina ID: $MAQUINA_ID"
echo "- Ensayo ID: $ENSAYO_ID"
echo "- Archivos exportados:"
echo "  - datos_ensayo_$ENSAYO_ID.xlsx"
echo "  - datos_ensayo_$ENSAYO_ID.csv"
