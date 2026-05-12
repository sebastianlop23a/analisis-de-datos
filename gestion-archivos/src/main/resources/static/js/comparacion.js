// ====================================
// COMPARACIÓN DE DATOS: MANUAL vs MASIVO
// ====================================

/**
 * Array global para almacenar datos manuales (máximo 10)
 */
let datosManualComparacion = new Array(10).fill(null).map((_, i) => ({
    index: i,
    valor: null
}));

/**
 * Array global para almacenar todos los datos del archivo disponibles
 */
let datosArchivoDisponibles = [];

/**
 * Set de índices de datos seleccionados del archivo
 */
let indicesDatosSeleccionados = new Set();

/**
 * Set de índices de datos seleccionados en la tabla de datos registrados
 */
let indicesSeleccionadosTabla = new Set();

/**
 * Array de datos seleccionados desde la tabla "Datos Registrados"
 */
let datosMasivoSeleccionadosTabla = []; 

/**
 * Seleccionar/deseleccionar todos los datos en la tabla
 */
function seleccionarTodosDatosTabla() {
    const checkAll = document.getElementById('checkAllDatos');
    const checkboxes = document.querySelectorAll('#tablaDatosBody input[type="checkbox"]');
    
    checkboxes.forEach(cb => {
        cb.checked = checkAll.checked;
        const index = parseInt(cb.dataset.index);
        if (checkAll.checked) {
            indicesSeleccionadosTabla.add(index);
        } else {
            indicesSeleccionadosTabla.delete(index);
        }
    });
    
    actualizarContadorSeleccionados();
}

/**
 * Actualizar selección individual en la tabla
 */
function actualizarSeleccionTabla(checkbox) {
    const index = parseInt(checkbox.dataset.index);
    if (checkbox.checked) {
        indicesSeleccionadosTabla.add(index);
    } else {
        indicesSeleccionadosTabla.delete(index);
    }
    
    // Actualizar el checkbox "todos"
    const totalCheckboxes = document.querySelectorAll('#tablaDatosBody input[type="checkbox"]').length;
    const checkAll = document.getElementById('checkAllDatos');
    checkAll.checked = indicesSeleccionadosTabla.size === totalCheckboxes && totalCheckboxes > 0;
    checkAll.indeterminate = indicesSeleccionadosTabla.size > 0 && indicesSeleccionadosTabla.size < totalCheckboxes;
    
    actualizarContadorSeleccionados();
}

/**
 * Actualizar contador de datos seleccionados
 */
function actualizarContadorSeleccionados() {
    const contador = document.getElementById('contadorSeleccionados');
    if (!contador) return;

    const tablaCount = indicesSeleccionadosTabla.size;
    contador.textContent = `${tablaCount} datos seleccionados en la tabla`;
}

/**
 * Devuelve los datos masivos seleccionados desde la tabla de registros
 */
function obtenerDatosMasivoSeleccionados() {
    return datosMasivoSeleccionadosTabla || [];
}

/**
 * Copiar datos seleccionados de la tabla de registros a la comparación
 */
function copiarSeleccionadosATablaComparacion() {
    if (indicesSeleccionadosTabla.size === 0) {
        showToast('Selecciona primero los datos en la tabla de registros', 'warning');
        return;
    }

    const datosTablaActual = window.datosFiltradosActuales && window.datosFiltradosActuales.length > 0
        ? window.datosFiltradosActuales
        : datosAnalisisOriginales;

    if (!datosTablaActual || datosTablaActual.length === 0) {
        showToast('No hay datos en la tabla de registros para copiar', 'warning');
        return;
    }

    const datosSeleccionados = Array.from(indicesSeleccionadosTabla)
        .map(index => datosTablaActual[index])
        .filter(Boolean);

    if (datosSeleccionados.length === 0) {
        showToast('No se pudieron obtener los datos seleccionados de la tabla', 'error');
        return;
    }

    datosMasivoSeleccionadosTabla = datosSeleccionados;
    actualizarTablaComparacion();

    showToast(`${datosSeleccionados.length} datos copiados desde la tabla de registros a la comparación`, 'success');
}

/**
 * Limpiar selección en la tabla
 */
function limpiarSeleccionTabla() {
    indicesSeleccionadosTabla.clear();
    datosMasivoSeleccionadosTabla = [];
    
    // Desmarcar todos los checkboxes
    document.querySelectorAll('#tablaDatosBody input[type="checkbox"]').forEach(cb => {
        cb.checked = false;
    });
    
    // Desmarcar el checkbox "todos"
    const checkAll = document.getElementById('checkAllDatos');
    if (checkAll) {
        checkAll.checked = false;
        checkAll.indeterminate = false;
    }
    
    actualizarContadorSeleccionados();
    actualizarTablaComparacion();
}

/**
 * Cargar todos los datos del archivo para selección
 */
function cargarTodosDatosArchivo() {
    if (!datosAnalisisOriginales || datosAnalisisOriginales.length === 0) {
        document.getElementById('selectorDatosMasivos').innerHTML = '<p style="color: #999; font-style: italic;">No hay datos disponibles</p>';
        return;
    }
    
    datosArchivoDisponibles = datosAnalisisOriginales.slice(0, 50); // Máximo 50 para mostrar
    
    // Crear checkboxes selectores
    const selectorDiv = document.getElementById('selectorDatosMasivos');
    selectorDiv.innerHTML = datosArchivoDisponibles.map((dato, idx) => `
        <label style="display: flex; align-items: center; gap: 8px; padding: 8px; background: white; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; transition: all 0.2s;">
            <input type="checkbox" 
                   data-index="${idx}" 
                   onchange="actualizarSeleccionDatos(this)"
                   style="width: 16px; height: 16px; cursor: pointer;">
            <span style="font-size: 12px; color: #333;">
                <strong>Dato ${idx + 1}:</strong> ${dato.valor.toFixed(4)}
                ${dato.sensor ? `<br><small style="color: #999;">${dato.sensor}</small>` : ''}
            </span>
        </label>
    `).join('');
    
    // Cargar sensores en el extractor
    cargarSensoresExtractor();
}

/**
 * Cargar sensores disponibles en el extractor
 */
function cargarSensoresExtractor() {
    if (!datosAnalisisOriginales || datosAnalisisOriginales.length === 0) {
        return;
    }
    
    // Obtener sensores únicos
    const sensoresUnicos = [...new Set(datosAnalisisOriginales.map(d => d.sensor).filter(s => s))];
    
    const select = document.getElementById('extractorSensor');
    select.innerHTML = '<option value="">Selecciona un sensor...</option>' +
        sensoresUnicos.map(sensor => `<option value="${sensor}">${sensor}</option>`).join('');
}

/**
 * Actualizar la selección de datos
 */
function actualizarSeleccionDatos(checkbox) {
    const index = parseInt(checkbox.dataset.index);
    if (checkbox.checked) {
        indicesDatosSeleccionados.add(index);
    } else {
        indicesDatosSeleccionados.delete(index);
    }
    actualizarTablaComparacion();
}

/**
 * Seleccionar todos los datos
 */
function seleccionarTodosDatosMasivos() {
    document.querySelectorAll('#selectorDatosMasivos input[type="checkbox"]').forEach(cb => {
        cb.checked = true;
        indicesDatosSeleccionados.add(parseInt(cb.dataset.index));
    });
    actualizarTablaComparacion();
}

/**
 * Deseleccionar todos los datos
 */
function deseleccionarTodosDatosMasivos() {
    document.querySelectorAll('#selectorDatosMasivos input[type="checkbox"]').forEach(cb => {
        cb.checked = false;
    });
    indicesDatosSeleccionados.clear();
    actualizarTablaComparacion();
}

/**
 * Extraer datos por rango o cantidad
 */
function extraerDatosRango() {
    const sensor = document.getElementById('extractorSensor').value;
    const cantidad = parseInt(document.getElementById('extractorCantidad').value);
    const desdeStr = document.getElementById('extractorDesde').value;
    const hastaStr = document.getElementById('extractorHasta').value;
    
    if (!sensor) {
        showToast('Por favor selecciona un sensor', 'warning');
        return;
    }
    
    if (!datosAnalisisOriginales || datosAnalisisOriginales.length === 0) {
        showToast('No hay datos disponibles. Carga un ensayo primero.', 'warning');
        return;
    }
    
    // Filtrar datos del sensor seleccionado
    let datosSensor = datosAnalisisOriginales.filter(d => d.sensor === sensor);
    
    if (datosSensor.length === 0) {
        showToast(`No hay datos para el sensor ${sensor}`, 'warning');
        return;
    }
    
    // Determinar método de extracción
    let datosExtraidosTemp = [];
    let metodoUsado = '';
    
    if (desdeStr && hastaStr) {
        // Método por tiempo
        const fechaDesde = parseDateTimeLocal(desdeStr);
        const fechaHasta = parseDateTimeLocal(hastaStr);
        
        if (!fechaDesde || !fechaHasta) {
            showToast('Formato de fecha y hora no válido', 'warning');
            return;
        }
        
        if (fechaDesde >= fechaHasta) {
            showToast('La fecha de inicio debe ser anterior a la fecha de fin', 'warning');
            return;
        }
        
        datosExtraidosTemp = datosSensor.filter(d => {
            const fechaDato = new Date(d.timestamp);
            return fechaDato >= fechaDesde && fechaDato <= fechaHasta;
        });
        
        metodoUsado = `Por tiempo: ${desdeStr} a ${hastaStr}`;
        
    } else if (cantidad >= 5 && cantidad <= 20) {
        // Método por cantidad
        datosExtraidosTemp = datosSensor.slice(0, cantidad);
        metodoUsado = `Por cantidad: ${cantidad} datos`;
        
    } else {
        showToast('Ingresa una cantidad válida (5-20) o un rango de tiempo', 'warning');
        return;
    }
    
    if (datosExtraidosTemp.length === 0) {
        showToast('No se encontraron datos con los criterios especificados', 'warning');
        return;
    }
    
    // Guardar datos extraídos
    datosExtraidos = datosExtraidosTemp.map(d => ({
        valor: d.valor,
        timestamp: d.timestamp,
        sensor: d.sensor,
        anormal: d.anormal
    }));
    
    // Mostrar resultado
    const resultadoDiv = document.getElementById('extractorResultado');
    const infoDiv = document.getElementById('extractorInfo');
    
    infoDiv.innerHTML = `
        <strong>✅ ${datosExtraidos.length} datos extraídos</strong><br>
        <small>Método: ${metodoUsado}</small><br>
        <small>Sensor: ${sensor}</small>
    `;
    
    resultadoDiv.style.display = 'block';
    
    showToast(`Se extrajeron ${datosExtraidos.length} datos del sensor ${sensor}`, 'success');
}

/**
 * Copiar datos extraídos a la tabla de comparación
 */
function copiarDatosExtraidos() {
    if (datosExtraidos.length === 0) {
        showToast('No hay datos extraídos para copiar', 'warning');
        return;
    }
    
    // Limpiar selecciones anteriores
    indicesDatosSeleccionados.clear();
    
    // Agregar los datos extraídos a la selección
    datosExtraidos.forEach((dato, idx) => {
        // Buscar el índice en datosArchivoDisponibles
        const indexEnArchivo = datosArchivoDisponibles.findIndex(d => 
            d.valor === dato.valor && d.timestamp === dato.timestamp && d.sensor === dato.sensor
        );
        
        if (indexEnArchivo !== -1) {
            indicesDatosSeleccionados.add(indexEnArchivo);
        }
    });
    
    // Actualizar checkboxes visuales
    document.querySelectorAll('#selectorDatosMasivos input[type="checkbox"]').forEach(cb => {
        const index = parseInt(cb.dataset.index);
        cb.checked = indicesDatosSeleccionados.has(index);
    });
    
    // Actualizar tabla
    actualizarTablaComparacion();
    
    showToast(`${datosExtraidos.length} datos copiados a la tabla de comparación`, 'success');
}

/**
 * Exportar datos extraídos como CSV
 */
function exportarDatosExtraidos() {
    if (datosExtraidos.length === 0) {
        showToast('No hay datos extraídos para exportar', 'warning');
        return;
    }
    
    let csv = 'Sensor,Timestamp,Valor,Anormal\n';
    
    datosExtraidos.forEach(dato => {
        csv += `${dato.sensor || ''},${dato.timestamp},${dato.valor.toFixed(4)},${dato.anormal ? 'Sí' : 'No'}\n`;
    });
    
    // Descargar como archivo
    const link = document.createElement('a');
    link.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    link.download = `datos-extraidos-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    
    showToast('Datos extraídos exportados a CSV', 'success');
}

/**
 * Editar valor manual en la tabla (inline)
 */
function editarDatoManual(index) {
    const celda = document.getElementById(`celdaManual_${index}`);
    if (!celda) return;
    
    const valorActual = datosManualComparacion[index].valor;
    
    celda.innerHTML = `
        <input type="number" 
               id="input_${index}" 
               value="${valorActual !== null ? valorActual : ''}" 
               placeholder="Ingresa valor"
               step="0.0001"
               style="width: 100%; padding: 6px; border: 2px solid #27ae60; border-radius: 4px; font-weight: 600; font-size: 14px;"
               onblur="guardarDatoManual(${index})"
               onkeypress="if(event.key==='Enter') guardarDatoManual(${index})">
    `;
    
    // Focus automático
    setTimeout(() => {
        const input = document.getElementById(`input_${index}`);
        if (input) input.focus();
    }, 0);
}

/**
 * Guardar dato manual editado
 */
function guardarDatoManual(index) {
    const input = document.getElementById(`input_${index}`);
    if (!input) return;
    
    const valor = input.value;
    
    if (valor === '') {
        datosManualComparacion[index].valor = null;
    } else {
        const numValor = parseFloat(valor);
        if (!isNaN(numValor)) {
            datosManualComparacion[index].valor = numValor;
        }
    }
    
    actualizarTablaComparacion();
}

/**
 * Limpiar todos los datos manuales
 */
function limpiarDatosManual() {
    if (datosManualComparacion.every(d => d.valor === null)) {
        showToast('No hay datos manuales para limpiar', 'info');
        return;
    }
    
    if (confirm('¿Está seguro de que desea limpiar todos los datos manuales?')) {
        datosManualComparacion = datosManualComparacion.map(d => ({ ...d, valor: null }));
        actualizarTablaComparacion();
        showToast('Datos manuales limpiados', 'info');
    }
}

/**
 * Actualizar tabla de comparación completa
 */
function actualizarTablaComparacion() {
    const tbody = document.getElementById('tablaComparacionBody');
    if (!tbody) return;
    
    const datosMasivoSeleccionados = obtenerDatosMasivoSeleccionados();
    const maxFilas = Math.max(
        datosManualComparacion.filter(d => d.valor !== null).length || 0,
        datosMasivoSeleccionados.length,
        10
    );
    
    let html = '';
    
    for (let i = 0; i < maxFilas; i++) {
        const datoManual = datosManualComparacion[i];
        const datoMasivo = datosMasivoSeleccionados[i];
        
        // Columna manual (editable)
        let celdaManual = '';
        if (datoManual) {
            if (datoManual.valor !== null) {
                celdaManual = `
                    <div 
                        id="celdaManual_${i}"
                        onclick="editarDatoManual(${i})"
                        style="
                            padding: 8px; 
                            background: #f0fdf4; 
                            border-radius: 4px; 
                            cursor: pointer;
                            font-weight: 600; 
                            color: #27ae60;
                            border: 1px solid #86efac;
                            transition: all 0.2s;
                        "
                        onmouseover="this.style.background='#dcfce7'; this.style.borderColor='#22c55e';"
                        onmouseout="this.style.background='#f0fdf4'; this.style.borderColor='#86efac';"
                    >
                        ${datoManual.valor.toFixed(4)}
                    </div>
                `;
            } else {
                celdaManual = `
                    <div 
                        id="celdaManual_${i}"
                        onclick="editarDatoManual(${i})"
                        style="
                            padding: 8px; 
                            background: #f9fafb; 
                            border: 2px dashed #cbd5e1;
                            border-radius: 4px; 
                            cursor: pointer;
                            color: #999;
                            transition: all 0.2s;
                        "
                        onmouseover="this.style.background='#f3f4f6'; this.style.borderColor='#94a3b8';"
                        onmouseout="this.style.background='#f9fafb'; this.style.borderColor='#cbd5e1';"
                    >
                        Haz clic para editar
                    </div>
                `;
            }
        }
        
        // Columna masiva (seleccionada)
        let celdaMasivo = '';
        if (datoMasivo) {
            celdaMasivo = `
                <div style="padding: 8px; background: #eff6ff; border-radius: 4px;">
                    <span style="font-weight: 600; color: #3498db;">${datoMasivo.valor.toFixed(4)}</span>
                    ${datoMasivo.sensor ? `<br><small style="color: #999;">${datoMasivo.sensor}</small>` : ''}
                    ${datoMasivo.timestamp ? `<br><small style="color: #ccc;">${new Date(datoMasivo.timestamp).toLocaleTimeString()}</small>` : ''}
                </div>
            `;
        } else {
            celdaMasivo = '<span style="color: #ccc;">-</span>';
        }
        
        // Columna diferencia
        let celdaDiferencia = '';
        if (datoManual && datoManual.valor !== null && datoMasivo) {
            const diferencia = datoManual.valor - datoMasivo.valor;
            const porcentajeDiferencia = datoMasivo.valor !== 0 
                ? ((diferencia / datoMasivo.valor) * 100).toFixed(2)
                : 'N/A';
            
            let colorDiferencia = '#27ae60'; // Verde
            if (diferencia !== 0) {
                colorDiferencia = Math.abs(diferencia) < 1 ? '#f39c12' : '#e74c3c'; // Naranja o Rojo
            }
            
            celdaDiferencia = `
                <div style="padding: 8px; background: #f9f9f9; border-radius: 4px;">
                    <span style="font-weight: 600; color: ${colorDiferencia};">${diferencia.toFixed(4)}</span>
                    <br>
                    <small style="color: #666;">${porcentajeDiferencia}%</small>
                </div>
            `;
        } else if ((datoManual && datoManual.valor !== null) || datoMasivo) {
            celdaDiferencia = '<span style="color: #ccc;">N/A</span>';
        } else {
            celdaDiferencia = '<span style="color: #ccc;">-</span>';
        }
        
        html += `
            <tr style="border-bottom: 1px solid #eee; height: 60px;">
                <td style="padding: 8px; text-align: center; border: 1px solid #ddd; width: 33.33%;">
                    ${celdaManual}
                </td>
                <td style="padding: 8px; text-align: center; border: 1px solid #ddd; width: 33.33%;">
                    ${celdaMasivo}
                </td>
                <td style="padding: 8px; text-align: center; border: 1px solid #ddd; width: 33.33%;">
                    ${celdaDiferencia}
                </td>
            </tr>
        `;
    }
    
    if (html === '') {
        html = `
            <tr style="height: 60px;">
                <td style="text-align: center; color: #999; border: 1px solid #ddd; padding: 12px; width: 33.33%;">
                    <em>Haz clic para editar</em>
                </td>
                <td style="text-align: center; color: #999; border: 1px solid #ddd; padding: 12px; width: 33.33%;">
                    <em>Selecciona datos arriba</em>
                </td>
                <td style="text-align: center; color: #999; border: 1px solid #ddd; padding: 12px; width: 33.33%;">
                    <em>-</em>
                </td>
            </tr>
        `;
    }
    
    tbody.innerHTML = html;
}

/**
 * Función para exportar comparación a CSV
 */
function exportarComparacion() {
    const datosMasivoSeleccionados = obtenerDatosMasivoSeleccionados();
    
    if (datosManualComparacion.every(d => d.valor === null) && datosMasivoSeleccionados.length === 0) {
        showToast('No hay datos para exportar', 'warning');
        return;
    }
    
    let csv = 'Dato Manual,Dato Masivo,Sensor,Diferencia,Porcentaje Diferencia\n';
    
    const maxFilas = Math.max(
        datosManualComparacion.filter(d => d.valor !== null).length,
        datosMasivoSeleccionados.length
    );
    
    for (let i = 0; i < maxFilas; i++) {
        const manual = datosManualComparacion[i];
        const masivo = datosMasivoSeleccionados[i];
        
        const valorManual = manual && manual.valor !== null ? manual.valor.toFixed(4) : '';
        const valorMasivo = masivo ? masivo.valor.toFixed(4) : '';
        const sensor = masivo ? masivo.sensor : '';
        
        let diferencia = '';
        let porcentaje = '';
        
        if (manual && manual.valor !== null && masivo) {
            diferencia = (manual.valor - masivo.valor).toFixed(4);
            porcentaje = masivo.valor !== 0 
                ? ((diferencia / masivo.valor) * 100).toFixed(2) + '%'
                : 'N/A';
        }
        
        csv += `${valorManual},${valorMasivo},${sensor},${diferencia},${porcentaje}\n`;
    }
    
    // Descargar como archivo
    const link = document.createElement('a');
    link.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    link.download = `comparacion-datos-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    
    showToast('Comparación exportada a CSV', 'success');
}

// Integración con cargarAnalisis
const originalCargarAnalisis = window.cargarAnalisis;
window.cargarAnalisis = async function() {
    await originalCargarAnalisis();
    // Resetear selecciones
    indicesDatosSeleccionados.clear();
    indicesSeleccionadosTabla.clear();
    datosManualComparacion = datosManualComparacion.map(d => ({ ...d, valor: null }));
    datosExtraidos = [];
    // Cargar datos
    cargarTodosDatosArchivo();
    actualizarTablaComparacion();
    actualizarContadorSeleccionados();
    
    // Ocultar resultado del extractor
    document.getElementById('extractorResultado').style.display = 'none';
};
