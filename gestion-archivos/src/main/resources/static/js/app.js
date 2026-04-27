// ====================================
// APLICACIÓN PRINCIPAL
// ====================================

console.log('🔄 CARGANDO app.js - Versión:', new Date().toISOString());

// Variables globales
let chartEstados = null;
let chartMaquinas = null;
let chartDatos = null;
let chartAnormales = null;
let chartBoxplot = null;
let chartCuartiles = null;
let chartTemporal = null;
let chartActividad = null;
let chartRendimiento = null;
let maquinasCache = [];
let ensayosCache = [];
let reportesCache = [];
let sensoresCache = [];
let datosFiltradosActuales = null; // Almacena datos filtrados cuando se aplica filtro

// Estados de filtros individuales por gráfica
let filtrosGraficas = {
    distribucion: null,
    anormales: null,
    boxplot: null,
    cuartiles: null,
    temporal: null
};
let reportesSinFiltrar = []; // Para filtrado por fecha
let datosAnalisisOriginales = []; // Para filtrado de datos por fecha/hora

// ====================================
// INICIALIZACIÓN
// ====================================

document.addEventListener('DOMContentLoaded', async () => {
    console.log('Iniciando aplicación...');

    // Setup event listeners
    setupNavigationListeners();
    setupFormListeners();

    // Cargar datos iniciales
    await inicializarApp();

    // Auto-actualizar datos cada 5 segundos (usamos bucle adaptativo)
    startPolling();
});

// Polling adaptativo: evita llamadas en bucle cuando la pestaña no está visible
let pollingActive = true;
let pollingBackoff = 1;

function startPolling() {
    pollingActive = true;
    (async function pollLoop() {
        while (pollingActive) {
            try {
                // Solo ejecutar si la pestaña está visible
                if (document.visibilityState === 'visible') {
                    await actualizarDatos();
                    // reset backoff en caso de éxito
                    pollingBackoff = 1;
                }
            } catch (err) {
                console.error('Error en polling:', err);
                // aumentar backoff hasta 8x
                pollingBackoff = Math.min(pollingBackoff * 2, 8);
            }

            // esperar antes del siguiente intento (con backoff)
            const delay = DELAYS.API_POLL * pollingBackoff;
            await new Promise(resolve => setTimeout(resolve, delay));
        }
    })();
}

function stopPolling() {
    pollingActive = false;
}

// Pausar polling cuando la pestaña esté oculta para evitar trafico innecesario
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
        console.log('Pestaña oculta: pausando polling');
    } else {
        console.log('Pestaña visible: reanudando polling');
    }
});

async function inicializarApp() {
    try {
        // Verificar estado del API
        const salud = await obtenerSalud();
        if (salud) {
            updateStatusApi(true);
        } else {
            updateStatusApi(false);
        }

        // Cargar datos iniciales
        await cargarMaquinas();
        await cargarEnsayos();
        await cargarSelectsEnsayos();  // Cargar selects de ensayos/correcciones desde el inicio
        await cargarReportes();
        await actualizarDashboard();
    } catch (error) {
        console.error('Error al inicializar:', error);
        showToast('Error al conectar con el API', 'error');
        updateStatusApi(false);
    }
}

async function actualizarDatos() {
    try {
        await cargarEnsayos();
        await cargarReportes();
        // No actualizar maquinas constantemente
    } catch (error) {
        console.error('Error al actualizar datos:', error);
    }
}

// ====================================
// NAVEGACIÓN
// ====================================

function setupNavigationListeners() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const section = item.getAttribute('data-section');
            showSection(section);
        });
    });
}

function showSection(sectionId) {
    // Ocultar todas las secciones
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });

    // Mostrar sección seleccionada
    const section = document.getElementById(sectionId);
    if (section) {
        section.classList.add('active');

        // Actualizar navegación
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-section="${sectionId}"]`).classList.add('active');

        // Ejecutar inicializaciones específicas de la sección
        if (sectionId === 'dashboard') {
            actualizarDashboard();
        } else if (sectionId === 'maquinas') {
            cargarMaquinas();
        } else if (sectionId === 'sensores') {
            cargarSensores();
        } else if (sectionId === 'ensayos') {
            cargarSelectsEnsayos();
            actualizarListaCorrecciones();
        } else if (sectionId === 'analisis') {
            cargarSelectsAnalisis();
        } else if (sectionId === 'reportes') {
            cargarSelectsReportes();
            cargarReportes();
        }
    }
}

// ====================================
// FORMULARIOS
// ====================================

function setupFormListeners() {
    // Formulario de máquina
    const formMaquina = document.getElementById('formMaquina');
    if (formMaquina) {
        formMaquina.addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitFormMaquina();
        });
    }

    // Formulario de ensayo
    const formEnsayo = document.getElementById('formEnsayo');
    if (formEnsayo) {
        formEnsayo.addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitFormEnsayo();
        });
    }

    // Formulario de dato
    const formDato = document.getElementById('formDato');
    if (formDato) {
        formDato.addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitFormDato();
        });
    }

    // Formulario de reporte
    const formReporte = document.getElementById('formReporte');
    if (formReporte) {
        formReporte.addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitFormReporte();
        });
    }

    // Formulario de sensor
    const formSensor = document.getElementById('formSensor');
    if (formSensor) {
        formSensor.addEventListener('submit', submitFormSensor);
    }

    // Agregar listeners adicionales
    setupFormListenersExtra();
    // Setup model buttons for ensayo
    try { setupEnsayoModelButtons(); } catch (e) { /* ignore */ }
}

// ====================================
// FORMULARIOS: MÁQUINAS
// ====================================

async function submitFormMaquina() {
    console.log('Iniciando submitFormMaquina...');
    
    const nombre = document.getElementById('maquinaNombre').value;
    const tipo = document.getElementById('maquinaTipo').value;
    const limiteInf = parseFloat(document.getElementById('maquinaLimiteInf').value);
    const limiteSup = parseFloat(document.getElementById('maquinaLimiteSup').value);
    const unidad = document.getElementById('maquinaUnidad').value;
    const descripcion = document.getElementById('maquinaDescripcion').value;
    const ubicacion = document.getElementById('maquinaUbicacion').value;
    const calcularFH = document.getElementById('maquinaCalcularFH').checked;
    const parametroZ = parseFloat(document.getElementById('maquinaParametroZ').value) || 14.0;

    console.log('Datos del formulario:', { nombre, tipo, limiteInf, limiteSup, unidad, descripcion, ubicacion, calcularFH, parametroZ });

    if (!nombre || !tipo || isNaN(limiteInf) || isNaN(limiteSup) || !unidad) {
        showToast('Por favor, rellena todos los campos requeridos', 'warning');
        console.warn('Campos incompletos');
        return;
    }

    if (limiteInf >= limiteSup) {
        showToast('El límite inferior debe ser menor que el superior', 'warning');
        console.warn('Validación de límites fallida');
        return;
    }

    const datos = {
        nombre,
        tipo,
        limiteInferior: limiteInf,
        limiteSuperior: limiteSup,
        unidadMedida: unidad,
        descripcion: descripcion || '',
        ubicacion: ubicacion || '',
        activa: true,
        calcularFH: calcularFH,
        parametroZ: parametroZ
    };

    console.log('Enviando datos:', JSON.stringify(datos));

    const resultado = await crearMaquina(datos);
    console.log('Resultado de crearMaquina:', resultado);
    
    if (resultado) {
        document.getElementById('formMaquina').reset();
        console.log('Formulario reseteado, cargando máquinas...');
        await cargarMaquinas();
        await cargarSelectsEnsayos();
    } else {
        console.error('Error: crearMaquina retornó null o false');
    }
}

async function cargarMaquinas() {
    try {
        const maquinas = await obtenerMaquinas();
        maquinasCache = maquinas;

        const listHtml = maquinas.map(m => `
            <div class="item">
                <div class="item-header">
                    <div class="item-title">${m.nombre}</div>
                    <span class="item-status status-${m.activa ? 'completado' : 'cancelado'}">
                        ${m.activa ? 'Activa' : 'Inactiva'}
                    </span>
                </div>
                <div class="item-details">
                    <div class="detail">
                        <span class="detail-label">Tipo</span>
                        <span class="detail-value">${m.tipo}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Rango</span>
                        <span class="detail-value">${m.limiteInferior} - ${m.limiteSuperior} ${m.unidadMedida}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Ubicación</span>
                        <span class="detail-value">${m.ubicacion || 'N/A'}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Descripción</span>
                        <span class="detail-value">${m.descripcion || 'Sin descripción'}</span>
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-info" onclick="editarMaquina(${m.id})">Editar</button>
                    <button class="btn btn-danger" onclick="confirmarEliminarMaquina(${m.id})">Eliminar</button>
                </div>
            </div>
        `).join('');

        document.getElementById('maquinasList').innerHTML = listHtml || '<p class="loading">No hay máquinas registradas</p>';
    } catch (error) {
        console.error('Error al cargar máquinas:', error);
    }
}

async function editarMaquina(id) {
    const maquina = maquinasCache.find(m => m.id === id);
    if (!maquina) return;

    document.getElementById('maquinaNombre').value = maquina.nombre;
    document.getElementById('maquinaTipo').value = maquina.tipo;
    document.getElementById('maquinaLimiteInf').value = maquina.limiteInferior;
    document.getElementById('maquinaLimiteSup').value = maquina.limiteSuperior;
    document.getElementById('maquinaUnidad').value = maquina.unidadMedida;
    document.getElementById('maquinaDescripcion').value = maquina.descripcion || '';
    document.getElementById('maquinaUbicacion').value = maquina.ubicacion || '';

    showSection('maquinas');
    document.querySelector('#formMaquina button').textContent = 'Actualizar Máquina';
    document.getElementById('formMaquina').onsubmit = async (e) => {
        e.preventDefault();
        await actualizarMaquina(id, {
            nombre: document.getElementById('maquinaNombre').value,
            tipo: document.getElementById('maquinaTipo').value,
            limiteInferior: parseFloat(document.getElementById('maquinaLimiteInf').value),
            limiteSuperior: parseFloat(document.getElementById('maquinaLimiteSup').value),
            unidadMedida: document.getElementById('maquinaUnidad').value,
            descripcion: document.getElementById('maquinaDescripcion').value,
            ubicacion: document.getElementById('maquinaUbicacion').value,
            activa: true
        });
        document.getElementById('formMaquina').reset();
        document.querySelector('#formMaquina button').textContent = 'Crear Máquina';
        setupFormListeners();
        await cargarMaquinas();
    };
}

async function confirmarEliminarMaquina(id) {
    if (confirm('¿Estás seguro de que deseas eliminar esta máquina?')) {
        if (await eliminarMaquina(id)) {
            await cargarMaquinas();
        }
    }
}

// ====================================
// FORMULARIOS: ENSAYOS
// ====================================

async function submitFormEnsayo() {
    const nombre = document.getElementById('ensayoNombre').value;
    const maquinaId = parseInt(document.getElementById('ensayoMaquina').value);
    const responsable = document.getElementById('ensayoResponsable').value;
    const descripcion = document.getElementById('ensayoDescripcion').value;
    const modeloSeleccionado = document.getElementById('ensayoModeloSeleccionado') ? document.getElementById('ensayoModeloSeleccionado').value : null;

    if (!nombre || !maquinaId) {
        showToast('Nombre y máquina son requeridos', 'warning');
        return;
    }

    console.log('=== Creando ensayo ===');
    console.log('Nombre:', nombre);
    console.log('Máquina ID:', maquinaId);

    const datos = {
        nombre: nombre.trim(),
        maquinaId,
        responsable: responsable || null,
        descripcion: descripcion || null,
        observaciones: modeloSeleccionado ? `ModeloPreferido:${modeloSeleccionado}` : null
    };

    console.log('Datos a enviar:', datos);

    const resultado = await crearEnsayo(datos);
    if (resultado) {
        document.getElementById('formEnsayo').reset();
        await cargarEnsayos();
        await cargarSelectsEnsayos();
        await actualizarDashboard();
    }
}

// Model selection buttons for ensayo creation
function setupEnsayoModelButtons() {
    const buttons = document.querySelectorAll('#ensayoModelButtons .model-btn');
    const selectedSpan = document.getElementById('ensayoModelSelected');
    const hidden = document.getElementById('ensayoModeloSeleccionado');
    if (!buttons || !selectedSpan || !hidden) return;
    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            // toggle active state
            buttons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const m = btn.getAttribute('data-model');
            selectedSpan.textContent = m;
            hidden.value = m;
        });
    });
}

async function submitFormDato() {
    const ensayoId = parseInt(document.getElementById('datoEnsayo').value);
    const valor = parseFloat(document.getElementById('datoValor').value);
    const fuente = document.getElementById('datoFuente').value || 'MANUAL';

    if (!ensayoId || !valor) {
        showToast('Ensayo y valor son requeridos', 'warning');
        return;
    }

    const datos = {
        valor,
        fuente,
        timestamp: new Date().toISOString()
    };

    const resultado = await registrarDato(ensayoId, datos);
    if (resultado) {
        document.getElementById('formDato').reset();
        await cargarEnsayos();
        // Sincronizar selector de correcciones con el ensayo actual y actualizar lista
        const selCorrecciones = document.getElementById('correccionesEnsayo');
        const filtroCorrecciones = document.getElementById('filtroEnsayoCorrecciones');
        if (selCorrecciones) selCorrecciones.value = ensayoId;
        if (filtroCorrecciones) filtroCorrecciones.value = ensayoId;
        console.log('[DEBUG] Actualizando correcciones después de registrar dato para ensayoId:', ensayoId);
        await actualizarListaCorrecciones();
        // Scroll automático a la sección de correcciones
        const correccionesSection = document.querySelector('[data-section="ensayos"]');
        if (correccionesSection) {
            setTimeout(() => {
                correccionesSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }, 300);
        }
    }
}

async function cargarSelectsEnsayos() {
    try {
        const ensayos = await obtenerEnsayos();
        const ensayosActivos = ensayos.filter(e => e.estado === ESTADOS.EN_PROGRESO);

        // Select para crear dato
        const selectDato = document.getElementById('datoEnsayo');
        if (selectDato) {
            selectDato.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayosActivos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }

        // Select para cargar archivo
        const selectCarga = document.getElementById('cargaEnsayo');
        if (selectCarga) {
            selectCarga.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayosActivos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }

        // Select para correcciones (sincronizado)
        const selectCorrecciones = document.getElementById('correccionesEnsayo');
        const filtroCorrecciones = document.getElementById('filtroEnsayoCorrecciones');
        if (selectCorrecciones) {
            selectCorrecciones.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayosActivos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }
        if (filtroCorrecciones) {
            filtroCorrecciones.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayosActivos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }

        // Select para análisis
        const selectAnalisis = document.getElementById('analisisEnsayo');
        if (selectAnalisis) {
            selectAnalisis.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayos.map(e => `<option value="${e.id}">${e.nombre} (${e.estado})</option>`).join('');
        }

        // Select para reportes
        const selectReporte = document.getElementById('reporteEnsayo');
        if (selectReporte) {
            selectReporte.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }

        // Select para descargas
        const selectDescarga = document.getElementById('descargaEnsayo');
        if (selectDescarga) {
            selectDescarga.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }
    } catch (error) {
        console.error('Error al cargar selects de ensayos:', error);
    }
}

async function cargarMaquinas() {
    try {
        const maquinas = await obtenerMaquinas();
        maquinasCache = maquinas;

        const selectEnsayo = document.getElementById('ensayoMaquina');
        if (selectEnsayo) {
            selectEnsayo.innerHTML = '<option value="">Selecciona una máquina</option>' +
                maquinas.map(m => `<option value="${m.id}">${m.nombre} (${m.tipo})</option>`).join('');
        }

        const listHtml = maquinas.map(m => `
            <div class="item">
                <div class="item-header">
                    <div class="item-title">🏭 ${m.nombre}</div>
                    <span class="item-status status-${m.activa ? 'completado' : 'cancelado'}">
                        ${m.activa ? '🟢 Activa' : '🔴 Inactiva'}
                    </span>
                </div>
                <div class="item-details">
                    <div class="detail">
                        <span class="detail-label">Tipo</span>
                        <span class="detail-value">${m.tipo}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Rango</span>
                        <span class="detail-value">${m.limiteInferior} - ${m.limiteSuperior} ${m.unidadMedida}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Ubicación</span>
                        <span class="detail-value">${m.ubicacion || 'N/A'}</span>
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-info" onclick="editarMaquina(${m.id})">Editar</button>
                    <button class="btn btn-danger" onclick="confirmarEliminarMaquina(${m.id})">Eliminar</button>
                </div>
            </div>
        `).join('');

        document.getElementById('maquinasList').innerHTML = listHtml || '<p class="loading">No hay máquinas</p>';
    } catch (error) {
        console.error('Error:', error);
    }
}

async function cargarEnsayos() {
    try {
        const ensayos = await obtenerEnsayos();
        ensayosCache = ensayos;

        const listHtml = ensayos.map(e => {
            return `
            <div class="item">
                <div class="item-header">
                    <div class="item-title">${e.nombre}</div>
                    <span class="item-status status-${e.estado.toLowerCase()}">
                        ${getEstadoEmoji(e.estado)} ${e.estado}
                    </span>
                </div>
                <div class="item-details">
                    <div class="detail">
                        <span class="detail-label">Máquina</span>
                        <span class="detail-value">${e.maquina?.nombre || 'N/A'}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Responsable</span>
                        <span class="detail-value">${e.responsable || 'N/A'}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Inicio</span>
                        <span class="detail-value">${formatDate(e.fechaInicio)}</span>
                    </div>
                </div>
                <div class="item-actions">
                    ${e.estado === ESTADOS.EN_PROGRESO ? `
                        <button class="btn btn-warning" onclick="pausarEnsayoUI(${e.id})">Pausar</button>
                        <button class="btn btn-success" onclick="finalizarEnsayoUI(${e.id})">Finalizar</button>
                    ` : e.estado === ESTADOS.PAUSADO ? `
                        <button class="btn btn-info" onclick="reanudarEnsayoUI(${e.id})">Reanudar</button>
                    ` : ''}
                    <button class="btn btn-danger" onclick="cancelarEnsayoUI(${e.id})">Cancelar</button>
                </div>
            </div>
        `;
        }).join('');

        document.getElementById('ensayosList').innerHTML = listHtml || '<p class="loading">No hay ensayos</p>';
    } catch (error) {
        console.error('Error:', error);
    }
}

async function pausarEnsayoUI(id) {
    await pausarEnsayo(id);
    await cargarEnsayos();
}

async function reanudarEnsayoUI(id) {
    await reanudarEnsayo(id);
    await cargarEnsayos();
}

async function finalizarEnsayoUI(id) {
    if (confirm('¿Estás seguro de que deseas finalizar este ensayo?')) {
        await finalizarEnsayo(id);
        await cargarEnsayos();
        await cargarReportes();
        await actualizarDashboard();
    }
}

async function cancelarEnsayoUI(id) {
    if (confirm('¿Estás seguro de que deseas cancelar este ensayo?')) {
        await cancelarEnsayo(id);
        await cargarEnsayos();
    }
}

// ====================================
// CARGA DE ARCHIVOS
// ====================================

async function cargarArchivo() {
    const ensayoId = document.getElementById('cargaEnsayo').value;
    const archivo = document.getElementById('cargaArchivo').files[0];

    if (!ensayoId || !archivo) {
        showToast('Selecciona ensayo y archivo', 'warning');
        return;
    }

    const extension = archivo.name.split('.').pop().toLowerCase();

    try {
        if (extension === 'csv') {
            await cargarArchivoCSV(parseInt(ensayoId), archivo);
        } else if (extension === 'txt') {
            await cargarArchivoTXT(parseInt(ensayoId), archivo);
        } else if (extension === 'pdf') {
            // Subir sólo PDF: llamar al endpoint de PDF
            try {
                const response = await api.uploadFile(`${API_CONFIG.ENDPOINTS.CARGA}/pdf/${ensayoId}`, archivo);
                showToast('✓ PDF subido y procesado', 'success');
            } catch (err) {
                console.error('Error al subir PDF:', err);
                showToast('Error al subir PDF: ' + (err.message || err), 'error');
                return;
            }
        } else {
            showToast('Solo se soportan archivos CSV, TXT y PDF', 'warning');
            return;
        }

        document.getElementById('formDato').reset();
        await cargarEnsayos();
        // Mantener select de correcciones en el mismo ensayo y actualizar vista
        const selCorrecciones = document.getElementById('correccionesEnsayo');
        const filtroCorrecciones = document.getElementById('filtroEnsayoCorrecciones');
        if (selCorrecciones) selCorrecciones.value = ensayoId;
        if (filtroCorrecciones) filtroCorrecciones.value = ensayoId;
        console.log('[DEBUG] Actualizando correcciones después de cargar archivo para ensayoId:', ensayoId);
        await actualizarListaCorrecciones();
        // Scroll automático a la sección de correcciones
        const correccionesSection = document.querySelector('[data-section="ensayos"]');
        if (correccionesSection) {
            setTimeout(() => {
                correccionesSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }, 300);
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

// Subir solo PDF de sensores (sin CSV)
async function subirPdfSolo() {
    const ensayoId = document.getElementById('cargaEnsayoPdf').value || document.getElementById('cargaEnsayo').value;
    const archivo = document.getElementById('soloPdf').files[0];

    if (!ensayoId || !archivo) {
        showToast('Selecciona ensayo y archivo PDF', 'warning');
        return;
    }

    try {
        showToast('Subiendo PDF...', 'info');
        const response = await api.uploadFile(`${API_CONFIG.ENDPOINTS.CARGA}/pdf/${ensayoId}`, archivo);
        if (response && response.sensores) {
            showToast('✓ PDF procesado. Sensores detectados: ' + JSON.stringify(response.sensores), 'success');
        } else if (response && response.mensaje) {
            showToast('✓ PDF procesado: ' + response.mensaje, 'success');
        } else {
            showToast('PDF procesado correctamente', 'success');
        }
        // Actualizar UI si es necesario
        await cargarEnsayos();
    } catch (error) {
        console.error('Error al subir PDF:', error);
        showToast('Error al subir PDF: ' + (error.message || error), 'error');
    }
}

// ====================================
// ANÁLISIS
// ====================================

async function cargarSelectsAnalisis() {
    try {
        const ensayos = await obtenerEnsayos();
        const selectAnalisis = document.getElementById('analisisEnsayo');
        if (selectAnalisis) {
            selectAnalisis.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }
    } catch (error) {
        console.error('Error al cargar selects de análisis:', error);
    }
}

async function cargarAnalisis() {
    const ensayoId = document.getElementById('analisisEnsayo').value;
    console.log('cargarAnalisis llamada con ensayoId:', ensayoId);
    
    if (!ensayoId) {
        console.log('No hay ensayoId seleccionado');
        // Limpiar filtros y datos cuando no hay ensayo seleccionado
        limpiarFiltroHora();
        datosAnalisisOriginales = [];
        const tbody = document.getElementById('tablaDatosBody');
        tbody.innerHTML = '<tr><td colspan="6" class="loading">Selecciona un ensayo para ver los datos</td></tr>';
        
        // Limpiar selector de sensores
        const sensorSelect = document.getElementById('filtroSensor');
        if (sensorSelect) sensorSelect.innerHTML = '<option value="">Todos los sensores</option>';
        
        return;
    }

    try {
        console.log('Obteniendo análisis...');
        // Obtener análisis
        const analisis = await obtenerAnalisisEnsayo(parseInt(ensayoId));
        console.log('Análisis obtenido:', analisis);
        
        if (!analisis) {
            showToast('No hay análisis disponible para este ensayo', 'warning');
            return;
        }

        // Actualizar tarjetas de estadísticas básicas
        document.getElementById('statTotal').textContent = analisis.totalDatos || 0;
        document.getElementById('statMedia').textContent = (analisis.media || 0).toFixed(2);
        document.getElementById('statDesv').textContent = (analisis.desviacionEstandar || 0).toFixed(2);
        document.getElementById('statMax').textContent = (analisis.maximo || 0).toFixed(2);
        document.getElementById('statMin').textContent = (analisis.minimo || 0).toFixed(2);
        document.getElementById('statAnormales').textContent = analisis.datosAnormales || 0;

        // Actualizar tarjetas de estadísticas adicionales
        document.getElementById('statRango').textContent = (analisis.rango || 0).toFixed(2);
        document.getElementById('statCoefVar').textContent = (analisis.coeficienteVariacion || 0).toFixed(2) + '%';
        document.getElementById('statPorcentajeAnormales').textContent = (analisis.porcentajeAnormales || 0).toFixed(2) + '%';

        // Calcular cuartiles de los datos
        const datosTemporales = await obtenerDatosTemporales(parseInt(ensayoId));
        console.log('Datos temporales obtenidos:', datosTemporales ? datosTemporales.length : 0);
        
        if (datosTemporales && datosTemporales.length > 0) {
            // Guardar datos originales para filtrado
            datosAnalisisOriginales = [...datosTemporales];
            
            const valoresOrdenados = datosTemporales.map(d => d.valor).sort((a, b) => a - b);
            const q1 = calcularCuartil(valoresOrdenados, 0.25);
            const q2 = calcularCuartil(valoresOrdenados, 0.50);
            const q3 = calcularCuartil(valoresOrdenados, 0.75);
            
            document.getElementById('statQ1').textContent = q1.toFixed(2);
            document.getElementById('statQ2').textContent = q2.toFixed(2);
            document.getElementById('statQ3').textContent = q3.toFixed(2);

            // Mostrar Factor Histórico si está disponible
            if (analisis.factorHistorico !== null && analisis.factorHistorico !== undefined) {
                document.getElementById('statFHContainer').style.display = 'flex';
                document.getElementById('statFactorHistorico').textContent = analisis.factorHistorico.toFixed(6);
                document.getElementById('statParametroZ').textContent = analisis.parametroZ || 14.0;
            } else {
                document.getElementById('statFHContainer').style.display = 'none';
            }

            // Gráfico de distribución
            crearGraficoDistribucion(datosTemporales, analisis.media);

            // Gráfico de anormales
            crearGraficoAnormales(datosTemporales);

            // Nuevas gráficas
            crearGraficoBoxplot(analisis, valoresOrdenados);
            crearGraficoCuartiles(analisis);
            crearGraficoTemporal(datosTemporales);

            // Análisis por sensor
            crearAnalisisPorSensor(datosTemporales, analisis.media);

            // Crear gráficas por sensor
            crearGraficasPorSensor(datosTemporales);

            // Tabla de datos
            llenarTablaDatos(datosAnalisisOriginales);
            
            // Mostrar estado de filtro al cargar por primera vez
            actualizarFiltroActivoTexto(null, null, '');
            
            // Resetear filtros individuales de gráficas
            filtrosGraficas = {
                distribucion: null,
                anormales: null,
                boxplot: null,
                cuartiles: null,
                temporal: null,
                sensores: {} // Inicializar filtros de sensores vacíos
            };
            
            // Poblar selector de sensores para filtro
            poblarSelectorSensoresFiltro(datosAnalisisOriginales);
        }
    } catch (error) {
        console.error('Error al cargar análisis:', error);
    }
}

// Función para calcular cuartiles
function calcularCuartil(valoresOrdenados, percentil) {
    if (valoresOrdenados.length === 0) return 0;
    const index = percentil * (valoresOrdenados.length - 1);
    const lower = Math.floor(index);
    const upper = Math.ceil(index);
    const weight = index % 1;
    
    if (lower === upper) {
        return valoresOrdenados[lower];
    }
    
    return valoresOrdenados[lower] * (1 - weight) + valoresOrdenados[upper] * weight;
}

function crearGraficoDistribucion(datos, media) {
    const ctx = document.getElementById('chartDatos');
    if (!ctx) return;

    // Preparar datos para histograma
    const valores = datos.map(d => d.valor);
    const min = Math.min(...valores);
    const max = Math.max(...valores);
    const binSize = (max - min) / 10 || 1;

    const bins = Array(10).fill(0);
    const labels = [];

    for (let i = 0; i < 10; i++) {
        const rangoMin = min + (i * binSize);
        const rangoMax = min + ((i + 1) * binSize);
        labels.push(`${rangoMin.toFixed(1)}-${rangoMax.toFixed(1)}`);
        bins[i] = valores.filter(v => v >= rangoMin && v < rangoMax).length;
    }

    if (chartDatos) chartDatos.destroy();

    chartDatos = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Frecuencia',
                data: bins,
                backgroundColor: 'rgba(52, 152, 219, 0.7)',
                borderColor: 'rgba(52, 152, 219, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: true }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function crearGraficoAnormales(datos) {
    const ctx = document.getElementById('chartAnormales');
    if (!ctx) return;

    const normales = datos.filter(d => !d.anormal).length;
    const anormales = datos.filter(d => d.anormal).length;

    if (chartAnormales) chartAnormales.destroy();

    chartAnormales = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Normales', 'Anormales'],
            datasets: [{
                data: [normales, anormales],
                backgroundColor: [
                    'rgba(46, 204, 113, 0.7)',
                    'rgba(231, 76, 60, 0.7)'
                ],
                borderColor: [
                    'rgba(46, 204, 113, 1)',
                    'rgba(231, 76, 60, 1)'
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: true }
            }
        }
    });
}

function crearGraficoBoxplot(analisis, valoresOrdenados) {
    const ctx = document.getElementById('chartBoxplot');
    if (!ctx) return;

    const q1 = analisis.q1 || calcularCuartil(valoresOrdenados, 0.25);
    const mediana = analisis.mediana || calcularCuartil(valoresOrdenados, 0.50);
    const q3 = analisis.q3 || calcularCuartil(valoresOrdenados, 0.75);
    const min = analisis.minimo;
    const max = analisis.maximo;
    const media = analisis.media;

    if (chartBoxplot) chartBoxplot.destroy();

    chartBoxplot = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Boxplot'],
            datasets: [
                {
                    label: 'Mínimo - Q1',
                    data: [q1 - min],
                    backgroundColor: 'rgba(52, 152, 219, 0.6)',
                    borderColor: 'rgba(52, 152, 219, 1)',
                    borderWidth: 2
                },
                {
                    label: 'Q1 - Mediana',
                    data: [mediana - q1],
                    backgroundColor: 'rgba(46, 204, 113, 0.6)',
                    borderColor: 'rgba(46, 204, 113, 1)',
                    borderWidth: 2
                },
                {
                    label: 'Mediana - Q3',
                    data: [q3 - mediana],
                    backgroundColor: 'rgba(241, 196, 15, 0.6)',
                    borderColor: 'rgba(241, 196, 15, 1)',
                    borderWidth: 2
                },
                {
                    label: 'Q3 - Máximo',
                    data: [max - q3],
                    backgroundColor: 'rgba(231, 76, 60, 0.6)',
                    borderColor: 'rgba(231, 76, 60, 1)',
                    borderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            indexAxis: 'y',
            scales: {
                x: {
                    stacked: true,
                    title: {
                        display: true,
                        text: 'Valor'
                    }
                },
                y: {
                    stacked: true
                }
            },
            plugins: {
                legend: { display: true },
                tooltip: {
                    callbacks: {
                        afterLabel: function(context) {
                            if (context.datasetIndex === 0) return `Mín: ${min.toFixed(2)}`;
                            if (context.datasetIndex === 1) return `Q1: ${q1.toFixed(2)}`;
                            if (context.datasetIndex === 2) return `Mediana: ${mediana.toFixed(2)}`;
                            if (context.datasetIndex === 3) return `Máx: ${max.toFixed(2)}`;
                            return '';
                        }
                    }
                }
            }
        }
    });
}

function crearGraficoCuartiles(analisis) {
    const ctx = document.getElementById('chartCuartiles');
    if (!ctx) return;

    const media = analisis.media;
    const q1 = analisis.q1;
    const q3 = analisis.q3;
    const mediaMinusQ1 = analisis.mediaMinusQ1 || (media - q1);
    const q3MinusMedia = analisis.q3MinusMedia || (q3 - media);
    const maximoMinusQ3 = analisis.maximoMinusQ3 || (analisis.maximo - q3);
    const q1MinusMinimo = analisis.q1MinusMinimo || (q1 - analisis.minimo);

    if (chartCuartiles) chartCuartiles.destroy();

    chartCuartiles = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Media - Q1', 'Q3 - Media', 'Q1 - Mín', 'Máx - Q3'],
            datasets: [{
                label: 'Distancias',
                data: [mediaMinusQ1, q3MinusMedia, q1MinusMinimo, maximoMinusQ3],
                backgroundColor: [
                    'rgba(52, 152, 219, 0.7)',
                    'rgba(46, 204, 113, 0.7)',
                    'rgba(241, 196, 15, 0.7)',
                    'rgba(231, 76, 60, 0.7)'
                ],
                borderColor: [
                    'rgba(52, 152, 219, 1)',
                    'rgba(46, 204, 113, 1)',
                    'rgba(241, 196, 15, 1)',
                    'rgba(231, 76, 60, 1)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return `Distancia: ${context.parsed.y.toFixed(4)}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Distancia'
                    }
                }
            }
        }
    });
}

function crearGraficoTemporal(datos) {
    const ctx = document.getElementById('chartTemporal');
    if (!ctx) return;

    // Ordenar por timestamp
    const datosOrdenados = [...datos].sort((a, b) => 
        new Date(a.timestamp) - new Date(b.timestamp)
    );

    const labels = datosOrdenados.map(d => {
        const fecha = new Date(d.timestamp);
        return fecha.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    });
    const valores = datosOrdenados.map(d => d.valor);
    const colores = datosOrdenados.map(d => d.anormal ? 
        'rgba(231, 76, 60, 0.8)' : 'rgba(52, 152, 219, 0.8)'
    );

    if (chartTemporal) chartTemporal.destroy();

    chartTemporal = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Temperatura',
                data: valores,
                borderColor: 'rgba(52, 152, 219, 1)',
                backgroundColor: 'rgba(52, 152, 219, 0.1)',
                pointBackgroundColor: colores,
                pointBorderColor: colores,
                pointRadius: 4,
                pointHoverRadius: 6,
                borderWidth: 2,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: true },
                tooltip: {
                    callbacks: {
                        afterLabel: function(context) {
                            const dato = datosOrdenados[context.dataIndex];
                            return [
                                `Sensor: ${dato.sensor || 'N/A'}`,
                                `Estado: ${dato.anormal ? 'Anormal' : 'Normal'}`
                            ];
                        }
                    }
                }
            },
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Tiempo'
                    },
                    ticks: {
                        maxRotation: 45,
                        minRotation: 45
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: 'Valor'
                    }
                }
            }
        }
    });
}

function crearAnalisisPorSensor(datos, mediaGeneral) {
    const container = document.getElementById('analisisSensores');
    
    // Agrupar datos por sensor
    const datosPorSensor = {};
    datos.forEach(d => {
        const sensor = d.sensor || 'Sin Sensor';
        if (!datosPorSensor[sensor]) {
            datosPorSensor[sensor] = [];
        }
        datosPorSensor[sensor].push(d);
    });
    
    const sensores = Object.keys(datosPorSensor);
    
    if (sensores.length === 0) {
        container.innerHTML = '<p class="empty-state">No hay datos para analizar</p>';
        return;
    }
    
    let html = '<h4>Comparación Entre Sensores</h4>';
    html += '<table class="data-table">';
    html += '<thead><tr>';
    html += '<th>Sensor</th>';
    html += '<th>Registros</th>';
    html += '<th>Media</th>';
    html += '<th>Mínimo</th>';
    html += '<th>Máximo</th>';
    html += '<th>Diferencia</th>';
    html += '<th>Desv. Est.</th>';
    html += '<th>Anormales</th>';
    html += '</tr></thead><tbody>';
    
    sensores.forEach(sensor => {
        const datosSensor = datosPorSensor[sensor];
        const valores = datosSensor.map(d => d.valor);
        
        const media = valores.reduce((a, b) => a + b, 0) / valores.length;
        const min = Math.min(...valores);
        const max = Math.max(...valores);
        const diferencia = max - min;
        
        // Calcular desviación estándar
        const varianza = valores.reduce((sum, val) => sum + Math.pow(val - media, 2), 0) / valores.length;
        const desv = Math.sqrt(varianza);
        
        const anormales = datosSensor.filter(d => d.anormal).length;
        
        html += '<tr>';
        html += `<td><strong><code>${sensor}</code></strong></td>`;
        html += `<td>${datosSensor.length}</td>`;
        html += `<td>${media.toFixed(4)}</td>`;
        html += `<td>${min.toFixed(4)}</td>`;
        html += `<td>${max.toFixed(4)}</td>`;
        html += `<td>${diferencia.toFixed(4)}</td>`;
        html += `<td>${desv.toFixed(4)}</td>`;
        html += `<td>${anormales}</td>`;
        html += '</tr>';
    });
    
    html += '</tbody></table>';
    
    // Tabla de desviaciones respecto a la media general
    if (sensores.length > 1) {
        html += '<h4 style="margin-top: 20px;">Análisis de Desviaciones Respecto a la Media General</h4>';
        html += '<table class="data-table">';
        html += '<thead><tr>';
        html += '<th>Sensor</th>';
        html += '<th>Media del Sensor</th>';
        html += '<th>Media General</th>';
        html += '<th>Desviación Absoluta</th>';
        html += '<th>Desviación Relativa (%)</th>';
        html += '</tr></thead><tbody>';
        
        sensores.forEach(sensor => {
            const datosSensor = datosPorSensor[sensor];
            const valores = datosSensor.map(d => d.valor);
            const mediaSensor = valores.reduce((a, b) => a + b, 0) / valores.length;
            
            const desviacionAbs = mediaSensor - mediaGeneral;
            const desviacionRel = (mediaGeneral !== 0) ? (desviacionAbs / mediaGeneral) * 100 : 0;
            
            const colorAbs = desviacionAbs > 0 ? 'color: #e74c3c;' : 'color: #3498db;';
            const colorRel = Math.abs(desviacionRel) > 5 ? 'color: #e74c3c; font-weight: bold;' : 'color: #2ecc71;';
            
            html += '<tr>';
            html += `<td><strong><code>${sensor}</code></strong></td>`;
            html += `<td>${mediaSensor.toFixed(4)}</td>`;
            html += `<td>${mediaGeneral.toFixed(4)}</td>`;
            html += `<td style="${colorAbs}">${desviacionAbs > 0 ? '+' : ''}${desviacionAbs.toFixed(4)}</td>`;
            html += `<td style="${colorRel}">${desviacionRel > 0 ? '+' : ''}${desviacionRel.toFixed(2)}%</td>`;
            html += '</tr>';
        });
        
        html += '</tbody></table>';
        
        // Leyenda
        html += '<div style="margin-top: 10px; padding: 10px; background: #f8f9fa; border-left: 3px solid #3498db;">';
        html += '<p style="margin: 5px 0;"><strong>📌 Interpretación:</strong></p>';
        html += '<p style="margin: 5px 0;">• <span style="color: #e74c3c;">Rojo:</span> Desviación mayor al 5% (requiere atención)</p>';
        html += '<p style="margin: 5px 0;">• <span style="color: #2ecc71;">Verde:</span> Desviación menor al 5% (dentro de rango aceptable)</p>';
        html += '<p style="margin: 5px 0;">• <strong>Positivo (+):</strong> El sensor mide por encima de la media</p>';
        html += '<p style="margin: 5px 0;">• <strong>Negativo (-):</strong> El sensor mide por debajo de la media</p>';
        html += '</div>';
    }
    
    container.innerHTML = html;
}

// Variables globales para las gráficas de sensores
let graficasSensoresData = null;
let chartsSensoresIndividuales = [];
let sensoresDisponibles = [];
let sensoresSeleccionados = new Set();

function crearSelectoresSensores(datos) {
    const sensores = [...new Set(datos.map(d => d.sensor).filter(s => s))];
    sensoresDisponibles = sensores;
    
    // Por defecto, seleccionar todos los sensores
    if (sensoresSeleccionados.size === 0) {
        sensores.forEach(s => sensoresSeleccionados.add(s));
    }
    
    const container = document.getElementById('selectoresSensores');
    if (!container) return;
    
    const colores = CONFIG.chartColors;
    
    container.innerHTML = sensores.map((sensor, index) => {
        const color = colores[index % colores.length];
        const isChecked = sensoresSeleccionados.has(sensor);
        
        return `
        <label style="
            display: flex; 
            align-items: center; 
            gap: 8px; 
            cursor: pointer; 
            padding: 8px 12px; 
            background: ${isChecked ? 'rgba(52, 152, 219, 0.15)' : 'rgba(255,255,255,0.03)'};
            border: 2px solid ${isChecked ? color : 'rgba(255,255,255,0.1)'};
            border-radius: 8px;
            transition: all 0.2s ease;
            font-size: 14px;
        " 
        onmouseover="this.style.background='rgba(52, 152, 219, 0.2)'; this.style.transform='translateY(-2px)'"
        onmouseout="this.style.background='${isChecked ? 'rgba(52, 152, 219, 0.15)' : 'rgba(255,255,255,0.03)'}'; this.style.transform='translateY(0)'">
            <input type="checkbox" 
                   value="${sensor}" 
                   ${isChecked ? 'checked' : ''}
                   onchange="toggleSensor('${sensor}')"
                   style="width: 16px; height: 16px; cursor: pointer;">
            <span style="
                width: 12px; 
                height: 12px; 
                background: ${color}; 
                border-radius: 3px;
                display: inline-block;
            "></span>
            <span style="font-weight: 500;">${sensor}</span>
        </label>
    `;
    }).join('');
}

function seleccionarTodosSensores() {
    sensoresDisponibles.forEach(s => sensoresSeleccionados.add(s));
    crearSelectoresSensores(graficasSensoresData);
    if (graficasSensoresData) {
        crearGraficasPorSensor(graficasSensoresData);
    }
}

function deseleccionarTodosSensores() {
    sensoresSeleccionados.clear();
    crearSelectoresSensores(graficasSensoresData);
    if (graficasSensoresData) {
        crearGraficasPorSensor(graficasSensoresData);
    }
}

function toggleSensor(sensor) {
    if (sensoresSeleccionados.has(sensor)) {
        sensoresSeleccionados.delete(sensor);
    } else {
        sensoresSeleccionados.add(sensor);
    }
    
    // Regenerar gráficas con los sensores seleccionados
    if (graficasSensoresData) {
        crearGraficasPorSensor(graficasSensoresData);
    }
}

function crearGraficasPorSensor(datos) {
    console.log('crearGraficasPorSensor llamada con', datos.length, 'datos');
    graficasSensoresData = datos;
    
    // Crear selectores si no existen
    if (sensoresDisponibles.length === 0) {
        crearSelectoresSensores(datos);
    }
    
    const checkbox = document.getElementById('modoComparacion');
    const modoComparacion = checkbox ? checkbox.checked : false;
    
    if (modoComparacion) {
        crearGraficaComparacion(datos);
    } else {
        crearGraficasIndividuales(datos);
    }
}

function crearGraficasIndividuales(datos) {
    const container = document.getElementById('graficasSensores');
    
    if (!container) {
        console.error('Contenedor graficasSensores no encontrado');
        return;
    }
    
    console.log('Creando gráficas individuales...');
    
    // Limpiar gráficas anteriores
    chartsSensoresIndividuales.forEach(chart => {
        try {
            chart.destroy();
        } catch(e) {
            console.warn('Error al destruir gráfica:', e);
        }
    });
    chartsSensoresIndividuales = [];
    
    // Agrupar datos por sensor
    const datosPorSensor = {};
    datos.forEach(d => {
        const sensor = d.sensor || 'Sin Sensor';
        if (!datosPorSensor[sensor]) {
            datosPorSensor[sensor] = [];
        }
        datosPorSensor[sensor].push(d);
    });
    
    // Filtrar solo los sensores seleccionados
    const todosLosSensores = Object.keys(datosPorSensor);
    const sensores = todosLosSensores.filter(s => sensoresSeleccionados.has(s));
    
    console.log('Sensores encontrados:', sensores.length, sensores);
    
    if (sensores.length === 0) {
        container.innerHTML = `
            <div style="padding: 40px; text-align: center; background: rgba(231, 76, 60, 0.1); border-radius: 12px; border: 2px dashed rgba(231, 76, 60, 0.3);">
                <div style="font-size: 48px; margin-bottom: 15px;">⚠️</div>
                <h4 style="color: #e74c3c; margin-bottom: 10px;">No hay sensores seleccionados</h4>
                <p style="color: #888; margin: 0;">Por favor, selecciona al menos un sensor para visualizar las gráficas</p>
            </div>
        `;
        return;
    }
    
    let html = '';
    sensores.forEach((sensor, index) => {
        html += `
            <div class="chart-container" style="margin-bottom: 20px; position: relative;">
                <div class="chart-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                    <h4 style="color: var(--color-dark); margin: 0;">
                        📊 Sensor: <code>${sensor}</code>
                    </h4>
                    <div style="display: flex; gap: 8px; align-items: center;">
                        <div class="chart-filter">
                            <input type="time" id="filtroInicioSensor${index}" class="filter-time" placeholder="Inicio">
                            <input type="time" id="filtroFinSensor${index}" class="filter-time" placeholder="Fin">
                            <button onclick="filtrarGraficaSensor('${sensor}', ${index})" class="btn-filter">Filtrar</button>
                            <button onclick="limpiarFiltroSensor('${sensor}', ${index})" class="btn-filter" style="background: #e74c3c;">✕</button>
                        </div>
                        <button onclick="resetZoom(${index})" class="btn btn-sm" style="padding: 5px 12px; font-size: 12px; background: #95a5a6;">
                            🔄 Restablecer Zoom
                        </button>
                    </div>
                </div>
                <p style="font-size: 12px; color: #888; margin: 0 0 10px 0;">
                    🔍 <em>Seleccionar: Mantén presionado y arrastra | Rueda del ratón: Zoom | Arrastrar: Mover | Doble clic: Restablecer</em>
                </p>
                <canvas id="chartSensor${index}"></canvas>
            </div>
        `;
    });
    
    container.innerHTML = html;
    
    console.log('HTML insertado, creando gráficas...');
    
    // Esperar un momento para que el DOM se actualice
    setTimeout(() => {
        // Crear una gráfica para cada sensor
        sensores.forEach((sensor, index) => {
            const datosSensor = datosPorSensor[sensor].sort((a, b) => 
                new Date(a.timestamp) - new Date(b.timestamp)
            );
            
            console.log(`Creando gráfica para sensor ${sensor} con ${datosSensor.length} datos`);
            
            const ctx = document.getElementById(`chartSensor${index}`);
            if (!ctx) {
                console.error(`Canvas chartSensor${index} no encontrado`);
                return;
            }
            
            const colores = CONFIG.chartColors;
            const colorPrincipal = colores[index % colores.length];
            
            const chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: datosSensor.map(d => formatDate(d.timestamp)),
                    datasets: [{
                        label: sensor,
                        data: datosSensor.map(d => d.valor),
                        borderColor: colorPrincipal,
                        backgroundColor: colorPrincipal.replace('rgb', 'rgba').replace(')', ', 0.1)'),
                        borderWidth: 2,
                        pointRadius: 3,
                        pointHoverRadius: 5,
                        tension: 0.3,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    },
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top'
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return `${context.dataset.label}: ${context.parsed.y.toFixed(4)}`;
                                }
                            }
                        },
                        zoom: {
                            zoom: {
                                wheel: {
                                    enabled: true,
                                    speed: 0.1
                                },
                                pinch: {
                                    enabled: true
                                },
                                drag: {
                                    enabled: true,
                                    backgroundColor: 'rgba(52, 152, 219, 0.3)',
                                    borderColor: 'rgba(52, 152, 219, 0.8)',
                                    borderWidth: 1,
                                    threshold: 10
                                },
                                mode: 'xy'
                            },
                            pan: {
                                enabled: true,
                                mode: 'xy'
                            },
                            limits: {
                                y: {min: 'original', max: 'original'},
                                x: {min: 'original', max: 'original'}
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            title: {
                                display: true,
                                text: 'Valor'
                            }
                        },
                        x: {
                            title: {
                                display: true,
                                text: 'Tiempo'
                            },
                            ticks: {
                                maxRotation: 45,
                                minRotation: 45
                            }
                        }
                    }
                }
            });
            
            console.log(`Gráfica creada para sensor ${sensor}`);
            chartsSensoresIndividuales.push(chart);
        });
    }, 100); // Esperar 100ms para que el DOM se actualice
}

function resetZoom(index) {
    if (chartsSensoresIndividuales[index]) {
        chartsSensoresIndividuales[index].resetZoom();
    }
}

function resetZoomComparacion() {
    if (chartsSensoresIndividuales[0]) {
        chartsSensoresIndividuales[0].resetZoom();
    }
}

function crearGraficaComparacion(datos) {
    const container = document.getElementById('graficasSensores');
    
    // Limpiar gráficas anteriores
    chartsSensoresIndividuales.forEach(chart => chart.destroy());
    chartsSensoresIndividuales = [];
    
    // Agrupar datos por sensor
    const datosPorSensor = {};
    datos.forEach(d => {
        const sensor = d.sensor || 'Sin Sensor';
        if (!datosPorSensor[sensor]) {
            datosPorSensor[sensor] = [];
        }
        datosPorSensor[sensor].push(d);
    });
    
    // Filtrar solo los sensores seleccionados
    const todosLosSensores = Object.keys(datosPorSensor);
    const sensores = todosLosSensores.filter(s => sensoresSeleccionados.has(s));
    
    if (sensores.length === 0) {
        container.innerHTML = `
            <div style="padding: 40px; text-align: center; background: rgba(231, 76, 60, 0.1); border-radius: 12px; border: 2px dashed rgba(231, 76, 60, 0.3);">
                <div style="font-size: 48px; margin-bottom: 15px;">⚠️</div>
                <h4 style="color: #e74c3c; margin-bottom: 10px;">No hay sensores seleccionados</h4>
                <p style="color: #888; margin: 0;">Selecciona al menos un sensor para comparar</p>
            </div>
        `;
        return;
    }
    
    // Crear HTML para la gráfica de comparación
    
    container.innerHTML = `
        <div class="chart-container" style="position: relative;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                <h4 style="color: var(--color-dark); margin: 0;">
                    📊 Comparación de Sensores (${sensores.length} sensores)
                </h4>
                <button onclick="resetZoomComparacion()" class="btn btn-sm" style="padding: 5px 12px; font-size: 12px; background: #95a5a6;">
                    🔄 Restablecer Zoom
                </button>
            </div>
            <p style="font-size: 12px; color: #888; margin: 0 0 10px 0;">
                🔍 <em>Seleccionar: Mantén presionado y arrastra | Rueda del ratón: Zoom | Arrastrar: Mover | Doble clic: Restablecer</em>
            </p>
            <canvas id="chartComparacion"></canvas>
        </div>
    `;
    
    const ctx = document.getElementById('chartComparacion');
    if (!ctx) return;
    
    // Obtener todos los timestamps únicos y ordenarlos
    const todosLosDatos = [];
    sensores.forEach(sensor => {
        datosPorSensor[sensor].forEach(d => {
            todosLosDatos.push(d);
        });
    });
    
    const todosLosTimestamps = [...new Set(todosLosDatos.map(d => d.timestamp))].sort();
    const labels = todosLosTimestamps.map(t => formatDate(t));
    
    // Crear datasets para cada sensor
    const colores = CONFIG.chartColors;
    const datasets = sensores.map((sensor, index) => {
        const datosSensor = datosPorSensor[sensor].sort((a, b) => 
            new Date(a.timestamp) - new Date(b.timestamp)
        );
        
        const color = colores[index % colores.length];
        
        return {
            label: sensor,
            data: datosSensor.map(d => d.valor),
            borderColor: color,
            backgroundColor: color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
            borderWidth: 2,
            pointRadius: 3,
            pointHoverRadius: 5,
            tension: 0.3,
            fill: false
        };
    });
    
    const chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: datosPorSensor[sensores[0]].map(d => formatDate(d.timestamp)),
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return `${context.dataset.label}: ${context.parsed.y.toFixed(4)}`;
                        }
                    }
                },
                zoom: {
                    zoom: {
                        wheel: {
                            enabled: true,
                            speed: 0.1
                        },
                        pinch: {
                            enabled: true
                        },
                        drag: {
                            enabled: true,
                            backgroundColor: 'rgba(52, 152, 219, 0.3)',
                            borderColor: 'rgba(52, 152, 219, 0.8)',
                            borderWidth: 1,
                            threshold: 10
                        },
                        mode: 'xy'
                    },
                    pan: {
                        enabled: true,
                        mode: 'xy'
                    },
                    limits: {
                        y: {min: 'original', max: 'original'},
                        x: {min: 'original', max: 'original'}
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: false,
                    title: {
                        display: true,
                        text: 'Valor'
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Tiempo'
                    },
                    ticks: {
                        maxRotation: 45,
                        minRotation: 45
                    }
                }
            }
        }
    });
    
    chartsSensoresIndividuales.push(chart);
}

function toggleModoComparacion() {
    if (graficasSensoresData) {
        crearGraficasPorSensor(graficasSensoresData);
    }
}

function llenarTablaDatos(datos) {
    const tbody = document.getElementById('tablaDatosBody');
    tbody.innerHTML = datos.map((d, idx) => `
        <tr>
            <td>${idx + 1}</td>
            <td>${formatDate(d.timestamp)}</td>
            <td><code>${d.sensor || 'N/A'}</code></td>
            <td>${d.valor.toFixed(2)}</td>
            <td>${d.anormal ? '🔴 Sí' : '🟢 No'}</td>
            <td>${d.fuente || 'N/A'}</td>
        </tr>
    `).join('');
}

// ====================================
// REPORTES
// ====================================

async function submitFormReporte() {
    const ensayoId = parseInt(document.getElementById('reporteEnsayo').value);
    const generadoPor = document.getElementById('reporteGeneradoPor').value || 'Sistema';

    if (!ensayoId) {
        showToast('Selecciona un ensayo', 'warning');
        return;
    }

    // Generar reporte HTML por defecto (no preguntamos el tipo)
    const resultado = await generarReporte(ensayoId, { tipo: 'HTML', generadoPor });
    if (resultado) {
        document.getElementById('formReporte').reset();
        await cargarReportes();
        showToast('Reporte generado. Usa los botones de descarga para obtenerlo en el formato deseado', 'success');
    }
}

async function cargarSelectsReportes() {
    try {
        const ensayos = await obtenerEnsayos();
        const selectReporte = document.getElementById('reporteEnsayo');
        if (selectReporte) {
            selectReporte.innerHTML = '<option value="">Selecciona un ensayo</option>' +
                ensayos.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        }
    } catch (error) {
        console.error('Error al cargar selects de reportes:', error);
    }
}

async function cargarReportes() {
    try {
        const reportes = await obtenerReportes();
        reportesCache = reportes;
        reportesSinFiltrar = reportes; // Guardar lista completa para filtrado

        const listHtml = reportes.map(r => `
            <div class="item">
                <div class="item-header">
                    <div class="item-title">${r.ensayo?.nombre || 'Reporte - Ensayo ' + r.ensayoId}</div>
                    <span class="item-status status-completado">Generado</span>
                </div>
                <div class="item-details">
                    <div class="detail">
                        <span class="detail-label">Generado por</span>
                        <span class="detail-value">${r.generadoPor || 'Sistema'}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Fecha</span>
                        <span class="detail-value">${formatDate(r.fechaGeneracion)}</span>
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-primary" onclick="descargarReportePdf(${r.ensayoId})" title="Descargar como PDF">PDF</button>
                    <button class="btn btn-success" onclick="descargarExcelReporte(${r.ensayoId})" title="Descargar como Excel">Excel</button>
                    <button class="btn btn-info" onclick="descargarCSV(${r.ensayoId})" title="Descargar como CSV">CSV</button>
                    <button class="btn btn-secondary" onclick="descargarReporte(${r.ensayoId})" title="Descargar como HTML">HTML</button>
                </div>
            </div>
        `).join('');

        document.getElementById('reportesList').innerHTML = listHtml || '<p class="loading">No hay reportes</p>';
    } catch (error) {
        console.error('Error:', error);
    }
}

// ====================================
// DASHBOARD
// ====================================

async function actualizarDashboard() {
    try {
        const maquinas = await obtenerMaquinas();
        const ensayos = await obtenerEnsayos();
        const reportes = await obtenerReportes();
        const sensores = await obtenerSensores();
        const info = await obtenerInfo();
        
        sensoresCache = sensores;

        // Actualizar contadores principales
        const ensayosProgreso = ensayos.filter(e => e.estado === ESTADOS.EN_PROGRESO).length;
        const ensayosCompletados = ensayos.filter(e => e.estado === ESTADOS.COMPLETADO).length;
        const ensayosPausados = ensayos.filter(e => e.estado === ESTADOS.PAUSADO).length;
        
        document.getElementById('countMaquinas').textContent = maquinas.length;
        document.getElementById('countMaquinasDetalle').textContent = `${maquinas.filter(m => m.activo).length || maquinas.length} activas`;
        
        document.getElementById('countEnsayosProgreso').textContent = ensayosProgreso;
        document.getElementById('countEnsayosProgresoDetalle').textContent = ensayosPausados > 0 ? `${ensayosPausados} pausados` : 'Procesando';
        
        document.getElementById('countEnsayosCompletados').textContent = ensayosCompletados;
        document.getElementById('countEnsayosCompletadosDetalle').textContent = `${Math.round((ensayosCompletados / (ensayos.length || 1)) * 100)}% del total`;
        
        document.getElementById('countReportes').textContent = reportes.length;
        document.getElementById('countReportesDetalle').textContent = `${reportes.filter(r => r.tipo === 'PDF').length || 0} PDF generados`;
        
        document.getElementById('countSensores').textContent = sensores.length;
        document.getElementById('countSensoresDetalle').textContent = `${sensores.filter(s => s.activo).length || sensores.length} activos`;
        
        // Calcular datos capturados (estimación)
        const totalDatos = ensayos.reduce((sum, e) => sum + (e.cantidadDatos || 0), 0);
        document.getElementById('countDatos').textContent = formatNumber(totalDatos);

        // Gráficos
        crearGraficoEstados(ensayos);
        crearGraficoMaquinas(ensayos, maquinas);
        crearGraficoActividad(ensayos);
        crearGraficoRendimiento(maquinas, ensayos);

        // Info del sistema
        if (info) {
            document.getElementById('systemInfo').innerHTML = `
                <p><strong>Versión:</strong> ${info.version || 'v1.0.0'}</p>
                <p><strong>Endpoints Disponibles:</strong> ${Object.keys(info.endpoints || {}).length}</p>
                <p><strong>Estado:</strong> <span style="color: #2ecc71;">✓ Sistema funcionando correctamente</span></p>
                <p><strong>Total de Máquinas:</strong> ${maquinas.length}</p>
                <p><strong>Total de Ensayos:</strong> ${ensayos.length}</p>
                <p><strong>Total de Sensores:</strong> ${sensores.length}</p>
            `;
        }
        
        // Actividad reciente
        mostrarActividadReciente(ensayos, reportes);
    } catch (error) {
        console.error('Error al actualizar dashboard:', error);
    }
}

function crearGraficoEstados(ensayos) {
    const ctx = document.getElementById('chartEstados');
    if (!ctx) return;

    const estados = {
        'En Progreso': ensayos.filter(e => e.estado === ESTADOS.EN_PROGRESO).length,
        'Completado': ensayos.filter(e => e.estado === ESTADOS.COMPLETADO).length,
        'Pausado': ensayos.filter(e => e.estado === ESTADOS.PAUSADO).length,
        'Cancelado': ensayos.filter(e => e.estado === ESTADOS.CANCELADO).length
    };

    if (chartEstados) chartEstados.destroy();

    chartEstados = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(estados),
            datasets: [{
                data: Object.values(estados),
                backgroundColor: [
                    'rgba(243, 156, 18, 0.8)',
                    'rgba(46, 204, 113, 0.8)',
                    'rgba(52, 152, 219, 0.8)',
                    'rgba(231, 76, 60, 0.8)'
                ],
                borderColor: [
                    'rgba(243, 156, 18, 1)',
                    'rgba(46, 204, 113, 1)',
                    'rgba(52, 152, 219, 1)',
                    'rgba(231, 76, 60, 1)'
                ],
                borderWidth: 2,
                hoverOffset: 10
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { 
                    position: 'bottom',
                    labels: {
                        padding: 15,
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    enabled: true,
                    backgroundColor: 'rgba(0,0,0,0.8)',
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true
            }
        }
    });
}

function crearGraficoMaquinas(ensayos, maquinas) {
    const ctx = document.getElementById('chartMaquinas');
    if (!ctx) return;

    const dataPorMaquina = {};
    maquinas.forEach(m => {
        dataPorMaquina[m.nombre] = ensayos.filter(e => e.maquinaId === m.id).length;
    });

    if (chartMaquinas) chartMaquinas.destroy();

    chartMaquinas = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Object.keys(dataPorMaquina),
            datasets: [{
                label: 'Cantidad de Ensayos',
                data: Object.values(dataPorMaquina),
                backgroundColor: 'rgba(52, 152, 219, 0.8)',
                borderColor: 'rgba(52, 152, 219, 1)',
                borderWidth: 2,
                borderRadius: 5,
                hoverBackgroundColor: 'rgba(52, 152, 219, 1)'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            indexAxis: 'y',
            plugins: {
                legend: { display: false },
                tooltip: {
                    enabled: true,
                    backgroundColor: 'rgba(0,0,0,0.8)',
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            return `Ensayos: ${context.parsed.x}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0,0,0,0.05)'
                    }
                },
                y: {
                    grid: {
                        display: false
                    }
                }
            },
            animation: {
                duration: 1000,
                easing: 'easeInOutQuart'
            }
        }
    });
}

// Nuevo gráfico de actividad por hora
function crearGraficoActividad(ensayos) {
    const ctx = document.getElementById('chartActividad');
    if (!ctx) return;

    // Agrupar ensayos por hora del día
    const actividadPorHora = new Array(24).fill(0);
    ensayos.forEach(e => {
        if (e.fechaInicio) {
            const fecha = new Date(e.fechaInicio);
            const hora = fecha.getHours();
            actividadPorHora[hora]++;
        }
    });

    if (chartActividad) chartActividad.destroy();

    chartActividad = new Chart(ctx, {
        type: 'line',
        data: {
            labels: Array.from({length: 24}, (_, i) => `${i}:00`),
            datasets: [{
                label: 'Ensayos Iniciados',
                data: actividadPorHora,
                borderColor: 'rgba(155, 89, 182, 1)',
                backgroundColor: 'rgba(155, 89, 182, 0.2)',
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointRadius: 4,
                pointHoverRadius: 6,
                pointBackgroundColor: 'rgba(155, 89, 182, 1)',
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    enabled: true,
                    backgroundColor: 'rgba(0,0,0,0.8)',
                    padding: 12
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0,0,0,0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            },
            animation: {
                duration: 1000,
                easing: 'easeInOutQuart'
            }
        }
    });
}

// Nuevo gráfico de rendimiento de máquinas
function crearGraficoRendimiento(maquinas, ensayos) {
    const ctx = document.getElementById('chartRendimiento');
    if (!ctx) return;

    const rendimientoPorMaquina = maquinas.map(m => {
        const ensayosMaquina = ensayos.filter(e => e.maquinaId === m.id);
        const completados = ensayosMaquina.filter(e => e.estado === ESTADOS.COMPLETADO).length;
        const total = ensayosMaquina.length;
        return {
            nombre: m.nombre,
            rendimiento: total > 0 ? (completados / total) * 100 : 0
        };
    });

    if (chartRendimiento) chartRendimiento.destroy();

    chartRendimiento = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: rendimientoPorMaquina.map(r => r.nombre),
            datasets: [{
                label: 'Rendimiento (%)',
                data: rendimientoPorMaquina.map(r => r.rendimiento),
                borderColor: 'rgba(26, 188, 156, 1)',
                backgroundColor: 'rgba(26, 188, 156, 0.3)',
                borderWidth: 3,
                pointRadius: 5,
                pointHoverRadius: 7,
                pointBackgroundColor: 'rgba(26, 188, 156, 1)',
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    enabled: true,
                    backgroundColor: 'rgba(0,0,0,0.8)',
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            return `Rendimiento: ${context.parsed.r.toFixed(1)}%`;
                        }
                    }
                }
            },
            scales: {
                r: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        stepSize: 20
                    },
                    grid: {
                        color: 'rgba(0,0,0,0.1)'
                    }
                }
            },
            animation: {
                duration: 1000,
                easing: 'easeInOutQuart'
            }
        }
    });
}

// Mostrar actividad reciente
function mostrarActividadReciente(ensayos, reportes) {
    const actividades = [];
    
    // Agregar ensayos recientes
    ensayos.slice(-5).reverse().forEach(e => {
        actividades.push({
            tipo: 'ensayo',
            fecha: new Date(e.fechaInicio),
            texto: `Ensayo "${e.nombre}" ${e.estado === ESTADOS.COMPLETADO ? 'completado' : 'iniciado'}`,
            icono: e.estado === ESTADOS.COMPLETADO ? '✓' : '⏳',
            color: e.estado === ESTADOS.COMPLETADO ? '#2ecc71' : '#f39c12'
        });
    });
    
    // Agregar reportes recientes
    reportes.slice(-3).reverse().forEach(r => {
        actividades.push({
            tipo: 'reporte',
            fecha: new Date(r.fechaGeneracion),
            texto: `Reporte generado: ${r.nombre}`,
            icono: '📄',
            color: '#3498db'
        });
    });
    
    // Ordenar por fecha
    actividades.sort((a, b) => b.fecha - a.fecha);
    
    // Mostrar solo las 5 más recientes
    const html = actividades.slice(0, 5).map(a => `
        <div class="activity-item">
            <span class="activity-icon" style="color: ${a.color}">${a.icono}</span>
            <span class="activity-text">${a.texto}</span>
            <span class="activity-time">${formatTimeAgo(a.fecha)}</span>
        </div>
    `).join('');
    
    document.getElementById('actividadReciente').innerHTML = html || '<p class="loading">No hay actividad reciente</p>';
}

// Formatear números grandes
function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
}

// Formatear tiempo relativo
function formatTimeAgo(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    
    if (seconds < 60) return 'Hace unos segundos';
    if (seconds < 3600) return `Hace ${Math.floor(seconds / 60)} min`;
    if (seconds < 86400) return `Hace ${Math.floor(seconds / 3600)} h`;
    return `Hace ${Math.floor(seconds / 86400)} días`;
}

// ====================================
// UTILIDADES
// ====================================

function updateStatusApi(online) {
    const badge = document.getElementById('statusApi');
    if (online) {
        badge.textContent = '● Online';
        badge.classList.add('online');
        badge.classList.remove('offline');
    } else {
        badge.textContent = '● Offline';
        badge.classList.add('offline');
        badge.classList.remove('online');
    }
}

function getEstadoEmoji(estado) {
    const emojis = {
        'EN_PROGRESO': '⏳',
        'COMPLETADO': '✓',
        'PAUSADO': '⏸️',
        'CANCELADO': '✕',
        'REPORTE_GENERADO': '✓'
    };
    return emojis[estado] || '•';
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('es-ES', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast show ${type}`;

    setTimeout(() => {
        toast.classList.remove('show');
    }, DELAYS.TOAST_DURATION);
}

function showModal(title, content) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = content;
    document.getElementById('modal').classList.add('active');
}

function cerrarModal() {
    document.getElementById('modal').classList.remove('active');
}

// ====================================
// CORRECCIONES (CORRECTIONS)
// ====================================

function setupFormListenersExtra() {
    // Existing form listeners...

    // Recalibración de sensores (nuevo flujo) - escucha al formulario si existe
    const formRecalibrar = document.getElementById('formRecalibrarSensor');
    if (formRecalibrar) {
        formRecalibrar.addEventListener('submit', async (e) => {
            e.preventDefault();
            await subirCalibracionSensor();
        });
    }

    // Reportes form
    const formReportes = document.getElementById('formReportes');
    if (formReportes) {
        formReportes.addEventListener('submit', async (e) => {
            e.preventDefault();
            await generarReporte();
        });
    }

    // Cargar datos en selectores
    cargarEnsayosEnSelectores();
}

async function cargarEnsayosEnSelectores() {
    try {
        const ensayos = await obtenerEnsayos();
        const selectores = [
                'reporteEnsayo',
                'descargaEnsayo'
            ];

        selectores.forEach(id => {
            const select = document.getElementById(id);
            if (select) {
                select.innerHTML = '<option value="">Selecciona un ensayo</option>';
                ensayos.forEach(ensayo => {
                    const option = document.createElement('option');
                    option.value = ensayo.id;
                    option.textContent = `${ensayo.nombre} (Máquina: ${ensayo.maquina?.nombre || 'N/A'})`;
                    select.appendChild(option);
                });
            }
        });
    } catch (error) {
        console.error('Error cargando ensayos en selectores:', error);
    }
}

async function cargarSensoresEnSelectores() {
    try {
        const sensores = await obtenerSensores();
        const sel = document.getElementById('recalibrarSensorSelect');
        if (!sel) return;
        sel.innerHTML = '<option value="">Selecciona un sensor</option>';
        sensores.forEach(s => {
            const option = document.createElement('option');
            option.value = s.id;
            option.textContent = `${s.codigo} - ${s.ubicacion}`;
            sel.appendChild(option);
        });
    } catch (error) {
        console.error('Error cargando sensores en selectores:', error);
    }
}

async function subirCalibracionSensor() {
    const selSensor = document.getElementById('recalibrarSensorSelect');
    const fileInput = document.getElementById('recalibrarArchivo');
    const txtDescripcion = document.getElementById('recalibrarDescripcion');
    const txtSubidoPor = document.getElementById('recalibrarSubidoPor');

    if (!selSensor || !fileInput) {
        showToast('Formulario de recalibración no disponible.', 'error');
        return;
    }

    const sensorId = selSensor.value;
    const archivo = fileInput.files && fileInput.files[0];
    const descripcion = txtDescripcion ? txtDescripcion.value : '';
    const subidoPor = txtSubidoPor ? txtSubidoPor.value : '';

    if (!sensorId || !archivo) {
        showToast('Debe seleccionar un sensor y un archivo', 'warning');
        return;
    }

    try {
        const formData = new FormData();
        formData.append('file', archivo);
        if (descripcion) formData.append('description', descripcion);
        if (subidoPor) formData.append('uploadedBy', subidoPor);

        const endpoint = `${API_CONFIG.BASE_URL}/calibrations/upload/${sensorId}`;
        const resp = await fetch(endpoint, { method: 'POST', body: formData });

        const body = await resp.json().catch(() => null);
        if (!resp.ok) {
            const msg = body && body.error ? body.error : `HTTP ${resp.status}`;
            showToast('Error subiendo calibración: ' + msg, 'error');
            console.error('Upload failed', resp.status, body);
            return;
        }

        showToast('Calibración subida correctamente', 'success');
        document.getElementById('formRecalibrarSensor').reset();
        await cargarSensores();
        await cargarSensoresEnSelectores();
    } catch (err) {
        console.error('Error en subida de calibración:', err);
        showToast('Error al subir calibración: ' + err.message, 'error');
    }
}

async function subirCorreccion() {
    const selEnsayo = document.getElementById('correccionesEnsayo');
    const fileInput = document.getElementById('correccionesArchivo');
    const txtDescripcion = document.getElementById('correccionesDescripcion');
    const txtSubidoPor = document.getElementById('correccionesSubidoPor');

    if (!selEnsayo || !fileInput || !txtDescripcion || !txtSubidoPor) {
        console.error('Elemento de formulario de correcciones no encontrado en el DOM');
        showToast('Formulario de correcciones no disponible. Recarga la página.', 'error');
        return;
    }

    const ensayoId = selEnsayo.value;
    const archivo = fileInput.files && fileInput.files[0];
    const descripcion = txtDescripcion.value;
    const subidoPor = txtSubidoPor.value || 'Sistema';

    if (!ensayoId || !archivo) {
        showToast('Debe seleccionar un ensayo y un archivo', 'warning');
        return;
    }

    try {
        const resultado = await subirArchivoCorreccion(ensayoId, archivo, descripcion, subidoPor);
        showToast(`Corrección subida correctamente: ${resultado.nombreArchivo}`, 'success');
        
        // Limpiar formulario
        document.getElementById('formCorrecciones').reset();
        
        // Actualizar lista
        await actualizarListaCorrecciones();
    } catch (error) {
        console.error('Error subiendo corrección:', error);
        showToast('Error al subir corrección: ' + error.message, 'error');
    }
}

// subirCalibracionSensor removed — per-sensor recalibration endpoint/UI no longer used

async function actualizarListaCorrecciones() {
    // Check if the corrections table exists (it may not if we're in Ensayos section)
    const tabla = document.getElementById('correccionesTable');
    if (!tabla) {
        console.log('[DEBUG] correccionesTable no encontrada, omitiendo actualización de lista');
        return;
    }

    const filtroSelect = document.getElementById('filtroEnsayoCorrecciones');
    if (!filtroSelect) {
        console.log('[DEBUG] filtroEnsayoCorrecciones no encontrado, omitiendo actualización de lista');
        tabla.innerHTML = '<p class="empty-state">Tabla de correcciones no disponible</p>';
        return;
    }

    const ensayoId = filtroSelect.value;
    
    try {
        let correcciones = [];
        
        if (ensayoId) {
            correcciones = await obtenerCorreccionesPorEnsayo(ensayoId);
        } else {
            // Obtener correcciones de todos los ensayos
            const ensayos = await obtenerEnsayos();
            for (const ensayo of ensayos) {
                const cors = await obtenerCorreccionesPorEnsayo(ensayo.id);
                correcciones.push(...cors);
            }
        }
        
        if (correcciones.length === 0) {
            tabla.innerHTML = '<p class="empty-state">No hay correcciones subidas</p>';
            return;
        }

        let html = '<table class="data-table"><thead><tr><th>Ensayo</th><th>Archivo</th><th>Tipo</th><th>Tamaño</th><th>Fecha</th><th>Descripción</th><th>Subido por</th><th>Acciones</th></tr></thead><tbody>';

        for (const correccion of correcciones) {
            const tamanio = (correccion.tamanioBytes / 1024).toFixed(2) + ' KB';
            html += `
                <tr>
                    <td>${correccion.ensayo?.nombre || 'N/A'}</td>
                    <td>${correccion.nombreArchivo}</td>
                    <td><span class="badge badge-${correccion.tipoArchivo.toLowerCase()}">${correccion.tipoArchivo}</span></td>
                    <td>${tamanio}</td>
                    <td>${formatDate(correccion.fechaSubida)}</td>
                    <td>${correccion.descripcion || '-'}</td>
                    <td>${correccion.subidoPor || 'Sistema'}</td>
                    <td>
                        <button class="btn btn-sm btn-success" onclick="descargarCorreccion(${correccion.id}, '${correccion.nombreArchivo}')">Descargar</button>
                        <button class="btn btn-sm btn-danger" onclick="eliminarCorreccion(${correccion.id})">Eliminar</button>
                    </td>
                </tr>
            `;
        }

        html += '</tbody></table>';
        tabla.innerHTML = html;
    } catch (error) {
        console.error('Error actualizando lista de correcciones:', error);
        if (tabla) {
            tabla.innerHTML = '<p class="error">Error cargando correcciones</p>';
        }
    }
}

async function descargarCorreccion(id, nombreArchivo) {
    try {
        // Use API_CONFIG.BASE_URL to build the download URL (avoid undefined API_BASE_URL)
        window.location.href = `${API_CONFIG.BASE_URL}/correcciones/${id}/download`;
        showToast(`Descargando: ${nombreArchivo}`, 'info');
    } catch (error) {
        console.error('Error descargando corrección:', error);
        showToast('Error al descargar corrección', 'error');
    }
}

async function eliminarCorreccion(id) {
    if (!confirm('¿Está seguro de que desea eliminar esta corrección?')) {
        return;
    }

    try {
        await api.delete(`/correcciones/${id}`);
        showToast('Corrección eliminada correctamente', 'success');
        await actualizarListaCorrecciones();
    } catch (error) {
        console.error('Error eliminando corrección:', error);
        showToast('Error al eliminar corrección', 'error');
    }
}

// ====================================
// SENSORES
// ====================================

async function cargarSensores() {
    try {
        const sensores = await obtenerSensores();
        sensoresCache = sensores;

        const listHtml = sensores.map(s => {
            const requiereCalibracion = calcularDiasCalibracion(s) <= 30;
            const estadoClase = s.activo ? 'success' : 'secondary';
            const estadoTexto = s.activo ? '🟢 Activo' : '🔴 Inactivo';
            const alertaCalibracion = requiereCalibracion ? '<span class="badge badge-warning">⚠️ Requiere Calibración</span>' : '';
            
            return `
            <div class="item">
                <div class="item-header">
                    <div class="item-title">🌡️ ${s.codigo} - ${s.ubicacion}</div>
                    <span class="item-status status-${estadoClase.toLowerCase()}">
                        ${estadoTexto}
                    </span>
                </div>
                <div class="item-details">
                    <div class="detail">
                        <span class="detail-label">Tipo</span>
                        <span class="detail-value">${s.tipoSonda}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Modelo</span>
                        <span class="detail-value">${s.modelo}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Rango</span>
                        <span class="detail-value">${s.rangoMinimo || 'N/A'}°C a ${s.rangoMaximo || 'N/A'}°C</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Precisión</span>
                        <span class="detail-value">±${s.precision || 'N/A'}°C</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Última Calibración</span>
                        <span class="detail-value">${formatDate(s.ultimaCalibracion) || 'Sin registrar'}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Próxima Calibración</span>
                        <span class="detail-value">${formatDate(s.proximaCalibracion) || 'N/A'} ${alertaCalibracion}</span>
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-info" onclick="editarSensor(${s.id})">Editar</button>
                    ${s.activo ? 
                        `<button class="btn btn-warning" onclick="desactivarSensor(${s.id})">Desactivar</button>` :
                        `<button class="btn btn-success" onclick="activarSensor(${s.id})">Activar</button>`
                    }
                    <button class="btn btn-danger" onclick="eliminarSensorUI(${s.id})">Eliminar</button>
                </div>
            </div>
        `;
        }).join('');

        document.getElementById('sensoresList').innerHTML = listHtml || '<p class="loading">No hay sensores registrados</p>';
        // Actualizar selectores relacionados con sensores (si existe la función)
        if (typeof cargarSensoresEnSelectores === 'function') {
            await cargarSensoresEnSelectores();
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('Error cargando sensores', 'error');
    }
}

function calcularDiasCalibracion(sensor) {
    if (!sensor.proximaCalibracion) return 9999;
    const hoy = new Date();
    const proxima = new Date(sensor.proximaCalibracion);
    const diff = proxima - hoy;
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

async function submitFormSensor(e) {
    e.preventDefault();
    
    const getVal = id => document.getElementById(id) ? document.getElementById(id).value : null;
    const datos = {
        codigo: (getVal('sensorCodigo') || '').trim(),
        tipoSonda: getVal('sensorTipoSonda') || null,
        // modelo y fabricante eliminados del formulario
        frecuenciaCalibracionDias: parseInt(getVal('sensorFrecuencia')) || 365,
        // Optional legacy fields: try to read if present
        rangoMinimo: getVal('sensorRangoMin') ? parseFloat(getVal('sensorRangoMin')) : null,
        rangoMaximo: getVal('sensorRangoMax') ? parseFloat(getVal('sensorRangoMax')) : null,
        precision: getVal('sensorPrecision') ? parseFloat(getVal('sensorPrecision')) : null,
        ultimaCalibracion: getVal('sensorUltimaCalibracion') || null,
        notas: (getVal('sensorNotas') || '').trim() || null,
        activo: true
    };

    console.log('Creando sensor:', datos);
    
    try {
        const sensor = await crearSensor(datos);
        console.log('Sensor creado:', sensor);
        showToast('Sensor registrado correctamente', 'success');
        document.getElementById('formSensor').reset();
        await cargarSensores();
    } catch (error) {
        console.error('Error creando sensor:', error);
        showToast('Error al registrar sensor: ' + error.message, 'error');
    }
}

async function filtrarSensores(filtro) {
    try {
        let sensores;
        
        if (filtro === 'activos') {
            sensores = await obtenerSensores(true);
        } else if (filtro === 'calibrar') {
            sensores = await obtenerSensoresProximosCalibracion();
        } else {
            sensores = await obtenerSensores();
        }
        
        sensoresCache = sensores;
        
        const listHtml = sensores.map(s => {
            const requiereCalibracion = calcularDiasCalibracion(s) <= 30;
            const estadoClase = s.activo ? 'success' : 'secondary';
            const estadoTexto = s.activo ? '🟢 Activo' : '🔴 Inactivo';
            const alertaCalibracion = requiereCalibracion ? '<span class="badge badge-warning">⚠️ Requiere Calibración</span>' : '';
            
            return `
            <div class="item">
                <div class="item-header">
                    <div class="item-title">🌡️ ${s.codigo} - ${s.ubicacion}</div>
                    <span class="item-status status-${estadoClase.toLowerCase()}">
                        ${estadoTexto}
                    </span>
                </div>
                <div class="item-details">
                    <div class="detail">
                        <span class="detail-label">Tipo</span>
                        <span class="detail-value">${s.tipoSonda}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Modelo</span>
                        <span class="detail-value">${s.modelo}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Rango</span>
                        <span class="detail-value">${s.rangoMinimo || 'N/A'}°C a ${s.rangoMaximo || 'N/A'}°C</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Última Calibración</span>
                        <span class="detail-value">${formatDate(s.ultimaCalibracion) || 'Sin registrar'}</span>
                    </div>
                    <div class="detail">
                        <span class="detail-label">Próxima Calibración</span>
                        <span class="detail-value">${formatDate(s.proximaCalibracion) || 'N/A'} ${alertaCalibracion}</span>
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-info" onclick="editarSensor(${s.id})">Editar</button>
                    ${s.activo ? 
                        `<button class="btn btn-warning" onclick="desactivarSensor(${s.id})">Desactivar</button>` :
                        `<button class="btn btn-success" onclick="activarSensor(${s.id})">Activar</button>`
                    }
                    <button class="btn btn-danger" onclick="eliminarSensorUI(${s.id})">Eliminar</button>
                </div>
            </div>
        `;
        }).join('');

        document.getElementById('sensoresList').innerHTML = listHtml || '<p class="loading">No hay sensores que coincidan con el filtro</p>';
    } catch (error) {
        console.error('Error filtrando sensores:', error);
        showToast('Error aplicando filtro', 'error');
    }
}

async function desactivarSensor(id) {
    try {
        const sensor = await obtenerSensor(id);
        sensor.activo = false;
        await actualizarSensor(id, sensor);
        showToast('Sensor desactivado', 'success');
        await cargarSensores();
    } catch (error) {
        console.error('Error desactivando sensor:', error);
        showToast('Error al desactivar sensor', 'error');
    }
}

async function activarSensor(id) {
    try {
        const sensor = await obtenerSensor(id);
        sensor.activo = true;
        await actualizarSensor(id, sensor);
        showToast('Sensor activado', 'success');
        await cargarSensores();
    } catch (error) {
        console.error('Error activando sensor:', error);
        showToast('Error al activar sensor', 'error');
    }
}

async function eliminarSensorUI(id) {
    if (!confirm('¿Está seguro de que desea eliminar este sensor?')) {
        return;
    }
    
    try {
        await eliminarSensor(id);
        showToast('Sensor eliminado', 'success');
        await cargarSensores();
    } catch (error) {
        console.error('Error eliminando sensor:', error);
        showToast('Error al eliminar sensor', 'error');
    }
}

async function editarSensor(id) {
    try {
        const s = await obtenerSensor(id);
        if (!s) { showToast('Sensor no encontrado', 'error'); return; }

        // Populate simplified form fields
        document.getElementById('sensorCodigo').value = s.codigo || '';
        if (document.getElementById('sensorTipoSonda')) document.getElementById('sensorTipoSonda').value = s.tipoSonda || '';
        // Populate only existing fields (modelo/fabricante removed)
        if (document.getElementById('sensorFrecuencia')) document.getElementById('sensorFrecuencia').value = s.frecuenciaCalibracionDias || 365;
        if (document.getElementById('sensorRangoMin')) document.getElementById('sensorRangoMin').value = s.rangoMinimo || '';
        if (document.getElementById('sensorRangoMax')) document.getElementById('sensorRangoMax').value = s.rangoMaximo || '';
        if (document.getElementById('sensorUltimaCalibracion')) document.getElementById('sensorUltimaCalibracion').value = s.ultimaCalibracion ? (new Date(s.ultimaCalibracion)).toISOString().split('T')[0] : '';

        // Indicate edit mode on register button
        const btn = document.getElementById('btnRegistrarSensor');
        btn.dataset.editId = id;
        btn.textContent = 'Actualizar Sensor';
        // Scroll to form
        document.querySelector('#sensores').scrollIntoView({ behavior: 'smooth' });
        showToast('Editando sensor ' + s.codigo, 'info');
    } catch (e) {
        console.error('Error iniciando edición:', e);
        showToast('Error iniciando edición', 'error');
    }
}

// ====================================
// FUNCIONES DE DEBUG PARA CORRECCIONES
// ====================================

/**
 * Función de debug para verificar correcciones aplicadas
 * Usa en la consola: debugCorrecciones(ensayoId)
 */
window.debugCorrecciones = async function(ensayoId) {
    console.log('🔍 ========================================');
    console.log('🔍 DEBUG DE CORRECCIONES');
    console.log('🔍 ========================================');
    
    try {
        // Obtener correcciones del ensayo
        const correcciones = await obtenerCorreccionesPorEnsayo(ensayoId);
        console.log(`📋 Correcciones encontradas: ${correcciones.length}`);
        
        if (correcciones.length > 0) {
            console.log('\n📄 Archivos de corrección:');
            correcciones.forEach((corr, idx) => {
                console.log(`  ${idx + 1}. ${corr.nombreArchivo} (${corr.tipoArchivo})`);
                console.log(`     - Tamaño: ${(corr.tamanioBytes / 1024).toFixed(2)} KB`);
                console.log(`     - Fecha: ${formatDate(corr.fechaSubida)}`);
                console.log(`     - Ruta: ${corr.rutaArchivo}`);
            });
        } else {
            console.log('⚠️ No hay correcciones subidas para este ensayo');
        }
        
        // Obtener datos temporales
        const datos = await obtenerDatosTemporales(ensayoId);
        console.log(`\n📊 Total de datos temporales: ${datos.length}`);
        
        if (datos.length > 0) {
            const datosCorregidos = datos.filter(d => d.fuente && d.fuente.includes('CORRECCION'));
            console.log(`✅ Datos con corrección aplicada: ${datosCorregidos.length}`);
            console.log(`📌 Datos sin corrección: ${datos.length - datosCorregidos.length}`);
            
            // Mostrar ejemplos de sensores
            const sensoresUnicos = [...new Set(datos.map(d => d.sensor))];
            console.log(`\n🔌 Sensores detectados: ${sensoresUnicos.length}`);
            sensoresUnicos.slice(0, 5).forEach(sensor => {
                const datosSensor = datos.filter(d => d.sensor === sensor);
                const corregidosSensor = datosSensor.filter(d => d.fuente && d.fuente.includes('CORRECCION'));
                console.log(`   - ${sensor}: ${datosSensor.length} datos (${corregidosSensor.length} corregidos)`);
            });
            
            // Mostrar ejemplos de datos
            if (datosCorregidos.length > 0) {
                console.log('\n✅ Ejemplos de datos CORREGIDOS:');
                datosCorregidos.slice(0, 3).forEach(d => {
                    console.log(`   Sensor: ${d.sensor}, Valor: ${d.valor.toFixed(4)}, Fuente: ${d.fuente}`);
                });
            }
            
            const datosSinCorregir = datos.filter(d => !d.fuente || !d.fuente.includes('CORRECCION'));
            if (datosSinCorregir.length > 0) {
                console.log('\n📌 Ejemplos de datos SIN CORRECCIÓN:');
                datosSinCorregir.slice(0, 3).forEach(d => {
                    console.log(`   Sensor: ${d.sensor}, Valor: ${d.valor.toFixed(4)}, Fuente: ${d.fuente || 'N/A'}`);
                });
            }
        }
        
        console.log('\n🔍 ========================================');
        console.log('💡 Las correcciones se aplican automáticamente al cargar archivos CSV');
        console.log('💡 Busca filas con fondo azul claro en la tabla de datos');
        console.log('💡 La columna "Corregido" muestra ✓ Sí para datos con corrección');
        console.log('🔍 ========================================');
        
    } catch (error) {
        console.error('❌ Error en debug de correcciones:', error);
    }
};

console.log('💡 Función de debug disponible: debugCorrecciones(ensayoId)');
console.log('   Ejemplo: debugCorrecciones(1)');

// ========================================
// FILTRADO DE REPORTES POR FECHA
// ========================================

function filtrarReportesPorFecha() {
    const fechaDesde = document.getElementById('filtroFechaDesde').value;
    const fechaHasta = document.getElementById('filtroFechaHasta').value;
    const tipo = document.getElementById('filtroTipo').value;
    
    let reportesFiltrados = [...reportesSinFiltrar];
    
    // Filtrar por fecha desde
    if (fechaDesde) {
        const desde = new Date(fechaDesde);
        desde.setHours(0, 0, 0, 0);
        reportesFiltrados = reportesFiltrados.filter(r => {
            const fechaReporte = new Date(r.fechaGeneracion);
            fechaReporte.setHours(0, 0, 0, 0);
            return fechaReporte >= desde;
        });
    }
    
    // Filtrar por fecha hasta
    if (fechaHasta) {
        const hasta = new Date(fechaHasta);
        hasta.setHours(23, 59, 59, 999);
        reportesFiltrados = reportesFiltrados.filter(r => {
            const fechaReporte = new Date(r.fechaGeneracion);
            return fechaReporte <= hasta;
        });
    }
    
    // Filtrar por tipo
    if (tipo) {
        reportesFiltrados = reportesFiltrados.filter(r => r.tipo === tipo);
    }
    
    // Mostrar reportes filtrados
    const listHtml = reportesFiltrados.map(r => `
        <div class="item">
            <div class="item-header">
                <div class="item-title">📄 ${r.ensayo?.nombre || 'Reporte - Ensayo ' + r.ensayoId}</div>
                <span class="item-status status-completado">✓ Generado</span>
            </div>
            <div class="item-details">
                <div class="detail">
                    <span class="detail-label">Tipo</span>
                    <span class="detail-value">${r.tipo}</span>
                </div>
                <div class="detail">
                    <span class="detail-label">Generado por</span>
                    <span class="detail-value">${r.generadoPor || 'Sistema'}</span>
                </div>
                <div class="detail">
                    <span class="detail-label">Fecha</span>
                    <span class="detail-value">${formatDate(r.fechaGeneracion)}</span>
                </div>
            </div>
            <div class="item-actions">
                <button class="btn btn-secondary" onclick="descargarReporte(${r.ensayoId})">Descargar HTML</button>
                <button class="btn btn-info" onclick="descargarExcelReporte(${r.ensayoId})">Descargar Excel</button>
                <button class="btn btn-primary" onclick="descargarReportePdf(${r.ensayoId})">Descargar PDF</button>
            </div>
        </div>
    `).join('');
    
    document.getElementById('reportesList').innerHTML = listHtml || '<p class="loading">No hay reportes que coincidan con los filtros</p>';
    
    // Mostrar mensaje con cantidad filtrada
    if (fechaDesde || fechaHasta || tipo) {
        showToast(`Mostrando ${reportesFiltrados.length} de ${reportesSinFiltrar.length} reportes`, 'info');
    }
}

function limpiarFiltrosReportes() {
    document.getElementById('filtroFechaDesde').value = '';
    document.getElementById('filtroFechaHasta').value = '';
    document.getElementById('filtroTipo').value = '';
    cargarReportes(); // Recargar todos los reportes
    showToast('Filtros eliminados', 'success');
}

// ====================================
// CONFIGURACIÓN
// ====================================

// Cargar configuración guardada
function cargarConfiguracion() {
    const config = JSON.parse(localStorage.getItem('appConfig') || '{}');
    
    document.getElementById('configPollingInterval').value = config.pollingInterval || 5;
    document.getElementById('configPageSize').value = config.pageSize || 50;
    document.getElementById('configTempThreshold').value = config.tempThreshold || 2.0;
    document.getElementById('configLogRetention').value = config.logRetention || 30;
    document.getElementById('configNotifications').checked = config.notifications !== false;
    document.getElementById('configDarkMode').checked = config.darkMode === true;
    
    // Aplicar tema oscuro si está activado
    if (config.darkMode) {
        document.body.classList.add('dark-mode');
    }
    
    // Listener para cambio en tiempo real del modo oscuro
    const darkModeCheckbox = document.getElementById('configDarkMode');
    if (darkModeCheckbox) {
        darkModeCheckbox.addEventListener('change', function() {
            if (this.checked) {
                document.body.classList.add('dark-mode');
                if (typeof updateChartTheme === 'function') {
                    updateChartTheme(true);
                }
            } else {
                document.body.classList.remove('dark-mode');
                if (typeof updateChartTheme === 'function') {
                    updateChartTheme(false);
                }
            }
            
            // Recargar gráficos al cambiar modo
            if (typeof actualizarDashboard === 'function') {
                actualizarDashboard();
            }
        });
    }
}

// Guardar configuración
async function guardarConfiguracion(event) {
    event.preventDefault();
    
    const config = {
        pollingInterval: parseInt(document.getElementById('configPollingInterval').value),
        pageSize: parseInt(document.getElementById('configPageSize').value),
        tempThreshold: parseFloat(document.getElementById('configTempThreshold').value),
        logRetention: parseInt(document.getElementById('configLogRetention').value),
        notifications: document.getElementById('configNotifications').checked,
        darkMode: document.getElementById('configDarkMode').checked
    };
    
    localStorage.setItem('appConfig', JSON.stringify(config));
    
    // Aplicar cambios inmediatamente
    aplicarConfiguracion(config);
    
    showToast('Configuración guardada exitosamente', 'success');
}

// Aplicar configuración
function aplicarConfiguracion(config) {
    // Actualizar tema oscuro
    if (config.darkMode) {
        document.body.classList.add('dark-mode');
        if (typeof updateChartTheme === 'function') {
            updateChartTheme(true);
        }
    } else {
        document.body.classList.remove('dark-mode');
        if (typeof updateChartTheme === 'function') {
            updateChartTheme(false);
        }
    }
    
    // Actualizar intervalo de polling
    if (config.pollingInterval) {
        console.log(`Intervalo de polling actualizado a ${config.pollingInterval} segundos`);
    }
    
    // Recargar gráficos al aplicar configuración
    if (typeof actualizarDashboard === 'function') {
        actualizarDashboard();
    }
}

// Resetear configuración a valores predeterminados
function resetearConfiguracion() {
    if (confirm('¿Desea restablecer todos los valores a la configuración predeterminada?')) {
        localStorage.removeItem('appConfig');
        cargarConfiguracion();
        document.body.classList.remove('dark-mode');
        showToast('Configuración restablecida', 'info');
    }
}

// Limpiar caché
function limpiarCache() {
    if (confirm('¿Desea limpiar toda la caché del sistema?')) {
        maquinasCache = [];
        ensayosCache = [];
        reportesCache = [];
        reportesSinFiltrar = [];
        
        // Limpiar gráficos
        if (chartEstados) chartEstados.destroy();
        if (chartMaquinas) chartMaquinas.destroy();
        if (chartDatos) chartDatos.destroy();
        if (chartAnormales) chartAnormales.destroy();
        if (chartBoxplot) chartBoxplot.destroy();
        if (chartCuartiles) chartCuartiles.destroy();
        if (chartTemporal) chartTemporal.destroy();
        
        chartEstados = null;
        chartMaquinas = null;
        chartDatos = null;
        chartAnormales = null;
        chartBoxplot = null;
        chartCuartiles = null;
        chartTemporal = null;
        
        showToast('Caché limpiada exitosamente', 'success');
        agregarLog('Caché del sistema limpiada');
    }
}

// Recargar todos los datos
async function recargarDatos() {
    showToast('Recargando datos del sistema...', 'info');
    agregarLog('Recargando todos los datos...');
    
    try {
        await inicializarApp();
        showToast('Datos recargados exitosamente', 'success');
        agregarLog('Datos recargados correctamente');
    } catch (error) {
        showToast('Error al recargar datos', 'error');
        agregarLog('Error al recargar datos: ' + error.message);
    }
}

// Exportar configuración
function exportarConfiguracion() {
    const config = JSON.parse(localStorage.getItem('appConfig') || '{}');
    const dataStr = JSON.stringify(config, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `configuracion_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
    
    showToast('Configuración exportada', 'success');
    agregarLog('Configuración exportada correctamente');
}

// Importar configuración
function importarConfiguracion() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    
    input.onchange = (e) => {
        const file = e.target.files[0];
        const reader = new FileReader();
        
        reader.onload = (event) => {
            try {
                const config = JSON.parse(event.target.result);
                localStorage.setItem('appConfig', JSON.stringify(config));
                cargarConfiguracion();
                aplicarConfiguracion(config);
                showToast('Configuración importada exitosamente', 'success');
                agregarLog('Configuración importada correctamente');
            } catch (error) {
                showToast('Error al importar configuración', 'error');
                agregarLog('Error al importar configuración: archivo inválido');
            }
        };
        
        reader.readAsText(file);
    };
    
    input.click();
}

// Descargar logs
function descargarLogs() {
    const logs = document.getElementById('systemLogs').value;
    const dataBlob = new Blob([logs], { type: 'text/plain' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `logs_${new Date().toISOString().split('T')[0]}.txt`;
    link.click();
    URL.revokeObjectURL(url);
    
    showToast('Logs descargados', 'success');
}

// Limpiar logs
function limpiarLogs() {
    if (confirm('¿Desea limpiar todos los logs del sistema?')) {
        document.getElementById('systemLogs').value = 'Sistema iniciado correctamente...\n';
        showToast('Logs limpiados', 'success');
    }
}

// Agregar entrada a los logs
function agregarLog(mensaje) {
    const logs = document.getElementById('systemLogs');
    if (logs) {
        const timestamp = new Date().toLocaleString('es-ES');
        logs.value += `[${timestamp}] ${mensaje}\n`;
        logs.scrollTop = logs.scrollHeight;
    }
}

// Setup form listener para configuración
document.addEventListener('DOMContentLoaded', () => {
    const formConfig = document.getElementById('formConfiguracion');
    if (formConfig) {
        formConfig.addEventListener('submit', guardarConfiguracion);
    }
    
    // Cargar configuración al iniciar
    cargarConfiguracion();
    
    // Cargar ensayos para verificación de FH
    cargarEnsayosParaFH();
});

// Cargar ensayos en el select de verificación FH
async function cargarEnsayosParaFH() {
    try {
        const ensayos = await obtenerEnsayos();
        const select = document.getElementById('ensayoFH');
        if (!select) return;
        
        // Limpiar opciones previas
        select.innerHTML = '<option value="">Seleccione un ensayo...</option>';
        
        // Agregar ensayos completados
        ensayos.filter(e => e.estado === ESTADOS.COMPLETADO)
               .forEach(e => {
                   const option = document.createElement('option');
                   option.value = e.id;
                   option.textContent = `${e.nombre} - ${e.fechaInicio || 'Sin fecha'}`;
                   select.appendChild(option);
               });
    } catch (error) {
        console.error('Error cargando ensayos para FH:', error);
    }
}

// Verificar Factor Histórico
async function verificarFH() {
    const ensayoId = document.getElementById('ensayoFH').value;
    const resultadoDiv = document.getElementById('fhResultado');
    const datosDiv = document.getElementById('fhDatos');
    
    if (!ensayoId) {
        showToast('Por favor seleccione un ensayo', 'warning');
        return;
    }
    
    showToast('Verificando Factor Histórico...', 'info');
    
    try {
        // Obtener reporte del ensayo
        const reportes = await obtenerReportes();
        const reporte = reportes.find(r => r.ensayoId && r.ensayoId.toString() === ensayoId.toString());
        
        if (!reporte) {
            datosDiv.innerHTML = `
                <p style="color: var(--color-warning);"><strong>⚠ No se encontró reporte para este ensayo</strong></p>
                <p>Genere un reporte primero para verificar el FH.</p>
            `;
            resultadoDiv.style.display = 'block';
            return;
        }
        
        // Obtener datos del reporte
        const response = await fetch(`${API_BASE_URL}/reportes/${reporte.id}`);
        const reporteCompleto = await response.json();
        
        const tieneFH = reporteCompleto.calculaFH === true || reporteCompleto.calculaFH === 'true';
        const factorHistorico = reporteCompleto.factorHistorico;
        
        let html = '';
        
        if (tieneFH && factorHistorico !== null && factorHistorico !== undefined) {
            html = `
                <p style="color: var(--color-success); font-size: 1.1em;"><strong>✓ El cálculo de FH está funcionando correctamente</strong></p>
                <div style="margin-top: 15px; padding: 15px; background: white; border-radius: 5px;">
                    <p><strong>Factor Histórico calculado:</strong> ${factorHistorico.toFixed(6)}</p>
                    <p><strong>Media:</strong> ${reporteCompleto.media ? reporteCompleto.media.toFixed(4) : 'N/A'}</p>
                    <p><strong>Desviación Estándar:</strong> ${reporteCompleto.desviacionEstandar ? reporteCompleto.desviacionEstandar.toFixed(4) : 'N/A'}</p>
                    <p><strong>Total de Datos:</strong> ${reporteCompleto.totalDatos || 0}</p>
                    <p><strong>Error Estándar:</strong> ${reporteCompleto.errorEstandar ? reporteCompleto.errorEstandar.toFixed(6) : 'N/A'}</p>
                    <p><strong>Valor t:</strong> ${reporteCompleto.valorT ? reporteCompleto.valorT.toFixed(4) : 'N/A'}</p>
                    
                    ${reporteCompleto.limiteConfianzaInferior && reporteCompleto.limiteConfianzaSuperior ? `
                        <p style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #ddd;">
                            <strong>Intervalo de Confianza 95%:</strong><br>
                            [${reporteCompleto.limiteConfianzaInferior.toFixed(4)}, ${reporteCompleto.limiteConfianzaSuperior.toFixed(4)}]
                        </p>
                    ` : ''}
                </div>
                
                <div style="margin-top: 15px; padding: 10px; background: #e8f5e9; border-radius: 5px;">
                    <p style="margin: 0; color: #2e7d32; font-size: 0.9em;">
                        ℹ️ <strong>Información:</strong> El Factor Histórico (FH) es la razón entre la media y el error estándar, 
                        y representa la precisión relativa de las mediciones.
                    </p>
                </div>
            `;
        } else if (tieneFH && (factorHistorico === null || factorHistorico === undefined)) {
            html = `
                <p style="color: var(--color-warning); font-size: 1.1em;"><strong>⚠ El FH está habilitado pero no se calculó</strong></p>
                <div style="margin-top: 15px; padding: 15px; background: white; border-radius: 5px;">
                    <p><strong>CalculaFH:</strong> ${tieneFH ? 'Sí' : 'No'}</p>
                    <p><strong>Factor Histórico:</strong> No disponible</p>
                    <p style="margin-top: 10px; color: var(--color-text-light);">
                        Posibles causas:<br>
                        • Error en el cálculo del error estándar<br>
                        • Desviación estándar es cero<br>
                        • Total de datos insuficiente (n < 2)
                    </p>
                </div>
            `;
        } else {
            html = `
                <p style="color: var(--color-info); font-size: 1.1em;"><strong>ℹ️ El cálculo de FH no está habilitado para este ensayo</strong></p>
                <div style="margin-top: 15px; padding: 15px; background: white; border-radius: 5px;">
                    <p><strong>CalculaFH:</strong> No</p>
                    <p style="margin-top: 10px; color: var(--color-text-light);">
                        Para habilitar el cálculo del Factor Histórico, active la opción al generar el reporte.
                    </p>
                </div>
            `;
        }
        
        datosDiv.innerHTML = html;
        resultadoDiv.style.display = 'block';
        showToast('Verificación completada', 'success');
        agregarLog(`FH verificado para ensayo ID ${ensayoId}`);
        
    } catch (error) {
        console.error('Error verificando FH:', error);
        datosDiv.innerHTML = `
            <p style="color: var(--color-danger);"><strong>✗ Error al verificar el Factor Histórico</strong></p>
            <p>${error.message}</p>
        `;
        resultadoDiv.style.display = 'block';
        showToast('Error al verificar FH', 'error');
    }
}

// ====================================
// FUNCIONES DE FILTRADO DE DATOS
// ====================================

function aplicarFiltroHora() {
    const horaInicioInput = document.getElementById('filtroHoraInicio');
    const horaFinInput = document.getElementById('filtroHoraFin');
    const sensorSelect = document.getElementById('filtroSensor');
    
    if (!horaInicioInput || !horaFinInput || !sensorSelect) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicioStr = horaInicioInput.value;
    const horaFinStr = horaFinInput.value;
    const sensorSeleccionado = sensorSelect.value;
    
    if (!horaInicioStr || !horaFinStr) {
        showToast('Por favor selecciona ambas horas', 'warning');
        return;
    }
    
    // Convertir horas a minutos desde medianoche para comparación
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    if (datosAnalisisOriginales.length === 0) {
        showToast('No hay datos para filtrar', 'warning');
        return;
    }
    
    // Filtrar datos por rango de hora y sensor
    const datosFiltrados = datosAnalisisOriginales.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        
        // Filtro de hora
        const cumpleHora = minutosDato >= minutosInicio && minutosDato <= minutosFin;
        
        // Filtro de sensor (si se seleccionó uno específico)
        const cumpleSensor = !sensorSeleccionado || dato.sensor === sensorSeleccionado;
        
        return cumpleHora && cumpleSensor;
    });
    
    // Actualizar tabla con datos filtrados
    llenarTablaDatos(datosFiltrados);
    
    // Almacenar datos filtrados para usar en gráficas
    datosFiltradosActuales = datosFiltrados;
    
    // Actualizar gráficas con datos filtrados
    actualizarGraficasConFiltro();
    actualizarFiltroActivoTexto(horaInicioStr, horaFinStr, sensorSeleccionado);
    
    const filtroTexto = sensorSeleccionado ? ` + Sensor: ${sensorSeleccionado}` : '';
    showToast(`Mostrando ${datosFiltrados.length} de ${datosAnalisisOriginales.length} registros${filtroTexto}`, 'info');
}

function limpiarFiltroHora() {
    const horaInicioInput = document.getElementById('filtroHoraInicio');
    const horaFinInput = document.getElementById('filtroHoraFin');
    const sensorSelect = document.getElementById('filtroSensor');
    
    if (horaInicioInput) horaInicioInput.value = '';
    if (horaFinInput) horaFinInput.value = '';
    if (sensorSelect) sensorSelect.value = '';
    
    // Mostrar todos los datos
    llenarTablaDatos(datosAnalisisOriginales);
    
    // Limpiar datos filtrados
    datosFiltradosActuales = null;
    
    // Resetear filtros individuales de gráficas
    filtrosGraficas = {
        distribucion: null,
        anormales: null,
        boxplot: null,
        cuartiles: null,
        temporal: null,
        sensores: {} // Limpiar también filtros de sensores
    };
    
    // Limpiar inputs de filtros individuales
    document.querySelectorAll('.filter-time').forEach(input => input.value = '');
    
    // Actualizar gráficas con todos los datos
    actualizarGraficasConFiltro();
    actualizarFiltroActivoTexto(null, null, '');
    
    showToast('Filtro limpiado - mostrando todos los datos', 'info');
}

// Función auxiliar para convertir hora HH:MM a minutos desde medianoche
function convertirHoraAMinutos(horaStr) {
    const [horas, minutos] = horaStr.split(':').map(Number);
    return horas * 60 + minutos;
}

function actualizarFiltroActivoTexto(horaInicioStr, horaFinStr, sensorSeleccionado) {
    const filtroActivo = document.getElementById('filtroActivoTexto');
    if (!filtroActivo) return;
    
    if (horaInicioStr && horaFinStr) {
        filtroActivo.textContent = `Filtro activo: ${horaInicioStr} - ${horaFinStr}` + (sensorSeleccionado ? ` · Sensor: ${sensorSeleccionado}` : ' · Todos los sensores');
    } else {
        filtroActivo.textContent = 'No hay filtro activo.';
    }
}

// Función para filtrar gráfica de un sensor específico
function filtrarGraficaSensor(sensor, index) {
    const inicioInput = document.getElementById(`filtroInicioSensor${index}`);
    const finInput = document.getElementById(`filtroFinSensor${index}`);
    
    if (!inicioInput || !finInput) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicio = inicioInput.value;
    const horaFin = finInput.value;
    
    if (!horaInicio || !horaFin) {
        showToast('Selecciona ambas horas para filtrar', 'warning');
        return;
    }
    
    const minutosInicio = convertirHoraAMinutos(horaInicio);
    const minutosFin = convertirHoraAMinutos(horaFin);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    // Guardar filtro para este sensor
    if (!filtrosGraficas.sensores) {
        filtrosGraficas.sensores = {};
    }
    filtrosGraficas.sensores[sensor] = {
        inicio: horaInicio,
        fin: horaFin
    };
    
    // Actualizar gráfica específica
    actualizarGraficaSensor(sensor, index);
    
    showToast(`Filtro aplicado a sensor ${sensor}: ${horaInicio} - ${horaFin}`, 'info');
}

// Función para limpiar filtro de un sensor específico
function limpiarFiltroSensor(sensor, index) {
    const inicioInput = document.getElementById(`filtroInicioSensor${index}`);
    const finInput = document.getElementById(`filtroFinSensor${index}`);
    
    if (inicioInput) inicioInput.value = '';
    if (finInput) finInput.value = '';
    
    // Limpiar filtro para este sensor
    if (filtrosGraficas.sensores) {
        delete filtrosGraficas.sensores[sensor];
    }
    
    // Actualizar gráfica específica
    actualizarGraficaSensor(sensor, index);
    
    showToast(`Filtro limpiado para sensor ${sensor}`, 'info');
}

// Función para actualizar una gráfica de sensor específica
function actualizarGraficaSensor(sensor, index) {
    if (!graficasSensoresData || graficasSensoresData.length === 0) {
        showToast('No hay datos disponibles para actualizar gráfica', 'warning');
        return;
    }
    
    // Filtrar datos del sensor
    let datosSensor = graficasSensoresData.filter(d => d.sensor === sensor);
    
    // Aplicar filtro individual si existe
    if (filtrosGraficas.sensores && filtrosGraficas.sensores[sensor]) {
        const filtro = filtrosGraficas.sensores[sensor];
        datosSensor = filtrarDatosPorHora(datosSensor, filtro.inicio, filtro.fin);
    }
    
    // Ordenar por timestamp
    datosSensor.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    
    // Actualizar gráfica
    if (chartsSensoresIndividuales[index]) {
        chartsSensoresIndividuales[index].data.labels = datosSensor.map(d => formatDate(d.timestamp));
        chartsSensoresIndividuales[index].data.datasets[0].data = datosSensor.map(d => d.valor);
        chartsSensoresIndividuales[index].update();
        
        console.log(`📊 Gráfica del sensor ${sensor} actualizada con ${datosSensor.length} datos`);
    }
}

// Función para filtrar gráfica de distribución
function filtrarGraficaDistribucion() {
    const horaInicioInput = document.getElementById('filtroDistribucionInicio');
    const horaFinInput = document.getElementById('filtroDistribucionFin');
    
    if (!horaInicioInput || !horaFinInput) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicioStr = horaInicioInput.value;
    const horaFinStr = horaFinInput.value;
    
    if (!horaInicioStr || !horaFinStr) {
        showToast('Por favor selecciona ambas horas', 'warning');
        return;
    }
    
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    // Filtrar datos para esta gráfica
    const datosFiltrados = datosAnalisisOriginales.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        return minutosDato >= minutosInicio && minutosDato <= minutosFin;
    });
    
    // Guardar filtro y actualizar gráfica
    filtrosGraficas.distribucion = { inicio: horaInicioStr, fin: horaFinStr };
    actualizarGraficaIndividual('distribucion', datosFiltrados);
    showToast(`Gráfica de distribución filtrada: ${datosFiltrados.length} datos`, 'info');
}

function limpiarFiltroDistribucion() {
    filtrosGraficas.distribucion = null;
    actualizarGraficaIndividual('distribucion', datosAnalisisOriginales);
    showToast('Filtro de distribución limpiado', 'info');
}

// Función para filtrar gráfica de anormales
function filtrarGraficaAnormales() {
    const horaInicioInput = document.getElementById('filtroAnormalesInicio');
    const horaFinInput = document.getElementById('filtroAnormalesFin');
    
    if (!horaInicioInput || !horaFinInput) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicioStr = horaInicioInput.value;
    const horaFinStr = horaFinInput.value;
    
    if (!horaInicioStr || !horaFinStr) {
        showToast('Por favor selecciona ambas horas', 'warning');
        return;
    }
    
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    const datosFiltrados = datosAnalisisOriginales.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        return minutosDato >= minutosInicio && minutosDato <= minutosFin;
    });
    
    filtrosGraficas.anormales = { inicio: horaInicioStr, fin: horaFinStr };
    actualizarGraficaIndividual('anormales', datosFiltrados);
    showToast(`Gráfica de anormales filtrada: ${datosFiltrados.length} datos`, 'info');
}

function limpiarFiltroAnormales() {
    filtrosGraficas.anormales = null;
    actualizarGraficaIndividual('anormales', datosAnalisisOriginales);
    showToast('Filtro de anormales limpiado', 'info');
}

// Función para filtrar gráfica de boxplot
function filtrarGraficaBoxplot() {
    const horaInicioInput = document.getElementById('filtroBoxplotInicio');
    const horaFinInput = document.getElementById('filtroBoxplotFin');
    
    if (!horaInicioInput || !horaFinInput) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicioStr = horaInicioInput.value;
    const horaFinStr = horaFinInput.value;
    
    if (!horaInicioStr || !horaFinStr) {
        showToast('Por favor selecciona ambas horas', 'warning');
        return;
    }
    
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    const datosFiltrados = datosAnalisisOriginales.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        return minutosDato >= minutosInicio && minutosDato <= minutosFin;
    });
    
    filtrosGraficas.boxplot = { inicio: horaInicioStr, fin: horaFinStr };
    actualizarGraficaIndividual('boxplot', datosFiltrados);
    showToast(`Gráfica de boxplot filtrada: ${datosFiltrados.length} datos`, 'info');
}

function limpiarFiltroBoxplot() {
    filtrosGraficas.boxplot = null;
    actualizarGraficaIndividual('boxplot', datosAnalisisOriginales);
    showToast('Filtro de boxplot limpiado', 'info');
}

// Función para filtrar gráfica de cuartiles
function filtrarGraficaCuartiles() {
    const horaInicioInput = document.getElementById('filtroCuartilesInicio');
    const horaFinInput = document.getElementById('filtroCuartilesFin');
    
    if (!horaInicioInput || !horaFinInput) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicioStr = horaInicioInput.value;
    const horaFinStr = horaFinInput.value;
    
    if (!horaInicioStr || !horaFinStr) {
        showToast('Por favor selecciona ambas horas', 'warning');
        return;
    }
    
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    const datosFiltrados = datosAnalisisOriginales.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        return minutosDato >= minutosInicio && minutosDato <= minutosFin;
    });
    
    filtrosGraficas.cuartiles = { inicio: horaInicioStr, fin: horaFinStr };
    actualizarGraficaIndividual('cuartiles', datosFiltrados);
    showToast(`Gráfica de cuartiles filtrada: ${datosFiltrados.length} datos`, 'info');
}

function limpiarFiltroCuartiles() {
    filtrosGraficas.cuartiles = null;
    actualizarGraficaIndividual('cuartiles', datosAnalisisOriginales);
    showToast('Filtro de cuartiles limpiado', 'info');
}

// Función para filtrar gráfica temporal
function filtrarGraficaTemporal() {
    const horaInicioInput = document.getElementById('filtroTemporalInicio');
    const horaFinInput = document.getElementById('filtroTemporalFin');
    
    if (!horaInicioInput || !horaFinInput) {
        showToast('Controles de filtro no encontrados', 'error');
        return;
    }
    
    const horaInicioStr = horaInicioInput.value;
    const horaFinStr = horaFinInput.value;
    
    if (!horaInicioStr || !horaFinStr) {
        showToast('Por favor selecciona ambas horas', 'warning');
        return;
    }
    
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    if (minutosInicio >= minutosFin) {
        showToast('La hora de inicio debe ser anterior a la hora de fin', 'warning');
        return;
    }
    
    const datosFiltrados = datosAnalisisOriginales.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        return minutosDato >= minutosInicio && minutosDato <= minutosFin;
    });
    
    filtrosGraficas.temporal = { inicio: horaInicioStr, fin: horaFinStr };
    actualizarGraficaIndividual('temporal', datosFiltrados);
    showToast(`Gráfica temporal filtrada: ${datosFiltrados.length} datos`, 'info');
}

function limpiarFiltroTemporal() {
    filtrosGraficas.temporal = null;
    actualizarGraficaIndividual('temporal', datosAnalisisOriginales);
    showToast('Filtro temporal limpiado', 'info');
}

// Función para actualizar una gráfica individual con datos filtrados
function actualizarGraficaIndividual(tipoGrafica, datos) {
    if (!datos || datos.length === 0) {
        showToast('No hay datos disponibles para actualizar la gráfica', 'warning');
        return;
    }
    
    const analisis = calcularAnalisis(datos);
    const valoresOrdenados = datos.map(d => d.valor).sort((a, b) => a - b);
    
    switch (tipoGrafica) {
        case 'distribucion':
            if (chartDatos) {
                crearGraficoDistribucion(datos, analisis.media);
            }
            break;
        case 'anormales':
            if (chartAnormales) {
                crearGraficoAnormales(datos);
            }
            break;
        case 'boxplot':
            if (chartBoxplot) {
                crearGraficoBoxplot(analisis, valoresOrdenados);
            }
            break;
        case 'cuartiles':
            if (chartCuartiles) {
                crearGraficoCuartiles(analisis);
            }
            break;
        case 'temporal':
            if (chartTemporal) {
                crearGraficoTemporal(datos);
            }
            break;
    }
}

// Función para actualizar todas las gráficas con sus filtros individuales
function actualizarGraficasConFiltro() {
    if (datosAnalisisOriginales.length === 0) {
        showToast('No hay datos disponibles para actualizar gráficas', 'warning');
        return;
    }
    
    // Actualizar cada gráfica con su filtro individual
    Object.keys(filtrosGraficas).forEach(tipoGrafica => {
        if (tipoGrafica === 'sensores') {
            // Los filtros de sensores se manejan individualmente
            return;
        }
        const filtro = filtrosGraficas[tipoGrafica];
        const datosParaGrafica = filtro ? 
            filtrarDatosPorHora(datosAnalisisOriginales, filtro.inicio, filtro.fin) : 
            datosAnalisisOriginales;
        
        actualizarGraficaIndividual(tipoGrafica, datosParaGrafica);
    });
    
    // Actualizar gráficas por sensor con el filtro general (si existe)
    const datosParaSensores = datosFiltradosActuales || datosAnalisisOriginales;
    crearGraficasPorSensor(datosParaSensores);
    
    console.log(`📊 Gráficas actualizadas con filtros individuales`);
}

// Función auxiliar para filtrar datos por rango de hora
function filtrarDatosPorHora(datos, horaInicioStr, horaFinStr) {
    const minutosInicio = convertirHoraAMinutos(horaInicioStr);
    const minutosFin = convertirHoraAMinutos(horaFinStr);
    
    return datos.filter(dato => {
        const fechaDato = new Date(dato.timestamp);
        const minutosDato = fechaDato.getHours() * 60 + fechaDato.getMinutes();
        return minutosDato >= minutosInicio && minutosDato <= minutosFin;
    });
}

// Función para calcular análisis estadístico básico de datos
function calcularAnalisis(datos) {
    if (!datos || datos.length === 0) {
        return {
            media: 0,
            desviacionEstandar: 0,
            minimo: 0,
            maximo: 0,
            totalDatos: 0,
            datosAnormales: 0,
            porcentajeAnormales: 0
        };
    }
    
    const valores = datos.map(d => d.valor);
    const media = valores.reduce((a, b) => a + b, 0) / valores.length;
    
    // Calcular desviación estándar
    const varianza = valores.reduce((a, b) => a + Math.pow(b - media, 2), 0) / valores.length;
    const desviacionEstandar = Math.sqrt(varianza);
    
    const minimo = Math.min(...valores);
    const maximo = Math.max(...valores);
    
    // Contar anormales (asumiendo que hay una propiedad 'anormal' en los datos)
    const datosAnormales = datos.filter(d => d.anormal === true).length;
    const porcentajeAnormales = (datosAnormales / datos.length) * 100;
    
    return {
        media: media,
        desviacionEstandar: desviacionEstandar,
        minimo: minimo,
        maximo: maximo,
        totalDatos: datos.length,
        datosAnormales: datosAnormales,
        porcentajeAnormales: porcentajeAnormales
    };
}

// Función para poblar el selector de sensores en el filtro
function poblarSelectorSensoresFiltro(datos) {
    const sensorSelect = document.getElementById('filtroSensor');
    if (!sensorSelect) return;
    
    // Obtener sensores únicos de los datos
    const sensoresUnicos = [...new Set(datos.map(d => d.sensor).filter(s => s))].sort();
    
    // Limpiar opciones existentes (excepto "Todos los sensores")
    sensorSelect.innerHTML = '<option value="">Todos los sensores</option>';
    
    // Agregar sensores como opciones
    sensoresUnicos.forEach(sensor => {
        const option = document.createElement('option');
        option.value = sensor;
        option.textContent = sensor;
        sensorSelect.appendChild(option);
    });
}

// Log de carga del archivo
console.log('✅ app.js cargado correctamente');
console.log('Funciones disponibles:', typeof cargarAnalisis, typeof crearGraficasPorSensor);
