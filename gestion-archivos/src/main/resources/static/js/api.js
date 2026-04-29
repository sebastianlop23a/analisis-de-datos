// ====================================
// FUNCIONES DE API
// ====================================

class ApiClient {
    constructor() {
        this.baseUrl = API_CONFIG.BASE_URL;
    }

    /**
     * Realiza una petición HTTP
     */
    async request(method, endpoint, data = null, retries = 0) {
        try {
            const options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            };

            if (data) {
                options.body = JSON.stringify(data);
            }

            console.log(`[API] ${method} ${this.baseUrl}${endpoint}`, data ? 'con datos:' : '', data);

            const response = await Promise.race([
                fetch(`${this.baseUrl}${endpoint}`, options),
                new Promise((_, reject) =>
                    setTimeout(() => reject(new Error('Timeout')), API_CONFIG.TIMEOUT)
                )
            ]);

            console.log(`[API] Respuesta: ${response.status} ${response.statusText}`);

            if (!response.ok) {
                const errorBody = await response.text();
                console.error('[API] Error Body:', errorBody);
                throw new Error(`HTTP ${response.status}: ${response.statusText} - ${errorBody}`);
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            return response;
        } catch (error) {
            console.error(`[API] Error en ${method} ${endpoint}:`, error.message);
            if (retries < API_CONFIG.RETRY_ATTEMPTS) {
                console.log(`[API] Reintentando (${retries + 1}/${API_CONFIG.RETRY_ATTEMPTS})...`);
                await new Promise(resolve => setTimeout(resolve, API_CONFIG.RETRY_DELAY));
                return this.request(method, endpoint, data, retries + 1);
            }
            throw error;
        }
    }

    /**
     * GET request
     */
    async get(endpoint) {
        return this.request('GET', endpoint);
    }

    /**
     * POST request
     */
    async post(endpoint, data) {
        return this.request('POST', endpoint, data);
    }

    /**
     * PUT request
     */
    async put(endpoint, data) {
        return this.request('PUT', endpoint, data);
    }

    /**
     * DELETE request
     */
    async delete(endpoint) {
        return this.request('DELETE', endpoint);
    }

    /**
     * Upload file
     */
    async uploadFile(endpoint, file) {
        try {
            const formData = new FormData();
            formData.append('archivo', file);

            const response = await Promise.race([
                fetch(`${this.baseUrl}${endpoint}`, {
                    method: 'POST',
                    body: formData
                }),
                new Promise((_, reject) =>
                    setTimeout(() => reject(new Error('Timeout')), API_CONFIG.TIMEOUT * 6)
                )
            ]);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    /**
     * Upload CSV with optional PDF in the same multipart request.
     */
    async uploadFileWithPdf(endpoint, csvFile, pdfFile) {
        try {
            const formData = new FormData();
            formData.append('archivo', csvFile);
            if (pdfFile) formData.append('pdf', pdfFile);

            const response = await Promise.race([
                fetch(`${this.baseUrl}${endpoint}`, {
                    method: 'POST',
                    body: formData
                }),
                new Promise((_, reject) =>
                    setTimeout(() => reject(new Error('Timeout')), API_CONFIG.TIMEOUT * 6)
                )
            ]);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    /**
     * Download file
     */
    async downloadFile(endpoint, filename) {
        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename || 'descarga';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        } catch (error) {
            throw error;
        }
    }
}

const api = new ApiClient();

// ====================================
// API: MAQUINAS
// ====================================

async function obtenerMaquinas() {
    try {
        return await api.get(API_CONFIG.ENDPOINTS.MAQUINAS);
    } catch (error) {
        console.error('Error al obtener máquinas:', error);
        showToast('Error al cargar máquinas', 'error');
        return [];
    }
}

async function obtenerMaquina(id) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.MAQUINAS}/${id}`);
    } catch (error) {
        console.error('Error al obtener máquina:', error);
        return null;
    }
}

async function crearMaquina(datos) {
    try {
        console.log('API: Enviando POST a', API_CONFIG.ENDPOINTS.MAQUINAS, 'con datos:', datos);
        const response = await api.post(API_CONFIG.ENDPOINTS.MAQUINAS, datos);
        console.log('API: Respuesta exitosa:', response);
        showToast('✓ Máquina creada exitosamente', 'success');
        return response;
    } catch (error) {
        console.error('API Error al crear máquina:', error);
        console.error('Detalles del error:', error.message);
        showToast('Error al crear máquina: ' + error.message, 'error');
        return null;
    }
}

async function actualizarMaquina(id, datos) {
    try {
        const response = await api.put(`${API_CONFIG.ENDPOINTS.MAQUINAS}/${id}`, datos);
        showToast('✓ Máquina actualizada', 'success');
        return response;
    } catch (error) {
        console.error('Error al actualizar máquina:', error);
        showToast('Error al actualizar máquina', 'error');
        return null;
    }
}

async function eliminarMaquina(id) {
    try {
        await api.delete(`${API_CONFIG.ENDPOINTS.MAQUINAS}/${id}`);
        showToast('✓ Máquina eliminada', 'success');
        return true;
    } catch (error) {
        console.error('Error al eliminar máquina:', error);
        showToast('Error al eliminar máquina', 'error');
        return false;
    }
}

async function validarValorMaquina(maquinaId, valor) {
    try {
        const response = await api.get(`${API_CONFIG.ENDPOINTS.MAQUINAS}/${maquinaId}/validar/${valor}`);
        return response.valido;
    } catch (error) {
        console.error('Error al validar valor:', error);
        return false;
    }
}

// ====================================
// API: ENSAYOS
// ====================================

async function obtenerEnsayos() {
    try {
        return await api.get(API_CONFIG.ENDPOINTS.ENSAYOS);
    } catch (error) {
        console.error('Error al obtener ensayos:', error);
        return [];
    }
}

async function obtenerEnsayo(id) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${id}`);
    } catch (error) {
        console.error('Error al obtener ensayo:', error);
        return null;
    }
}

async function crearEnsayo(datos) {
    try {
        const response = await api.post(API_CONFIG.ENDPOINTS.ENSAYOS, datos);
        showToast('✓ Ensayo creado exitosamente', 'success');
        return response;
    } catch (error) {
        console.error('Error al crear ensayo:', error);
        showToast('Error al crear ensayo: ' + error.message, 'error');
        return null;
    }
}

async function registrarDato(ensayoId, datos) {
    try {
        const response = await api.post(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${ensayoId}/datos`, datos);
        showToast('✓ Dato registrado', 'success');
        return response;
    } catch (error) {
        console.error('Error al registrar dato:', error);
        showToast('Error al registrar dato: ' + error.message, 'error');
        return null;
    }
}

async function obtenerDatosTemporales(ensayoId) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${ensayoId}/datos-temporales`);
    } catch (error) {
        console.error('Error al obtener datos temporales:', error);
        return [];
    }
}

async function finalizarEnsayo(ensayoId) {
    try {
        const response = await api.post(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${ensayoId}/finalizar`, {});
        showToast('✓ Ensayo finalizado', 'success');
        return response;
    } catch (error) {
        console.error('Error al finalizar ensayo:', error);
        showToast('Error al finalizar ensayo: ' + error.message, 'error');
        return null;
    }
}

async function pausarEnsayo(ensayoId) {
    try {
        const response = await api.post(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${ensayoId}/pausar`, {});
        showToast('✓ Ensayo pausado', 'success');
        return response;
    } catch (error) {
        console.error('Error al pausar ensayo:', error);
        showToast('Error al pausar ensayo', 'error');
        return null;
    }
}

async function reanudarEnsayo(ensayoId) {
    try {
        const response = await api.post(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${ensayoId}/reanudar`, {});
        showToast('✓ Ensayo reanudado', 'success');
        return response;
    } catch (error) {
        console.error('Error al reanudar ensayo:', error);
        showToast('Error al reanudar ensayo', 'error');
        return null;
    }
}

async function cancelarEnsayo(ensayoId) {
    try {
        const response = await api.post(`${API_CONFIG.ENDPOINTS.ENSAYOS}/${ensayoId}/cancelar`, {});
        showToast('✓ Ensayo cancelado', 'success');
        return response;
    } catch (error) {
        console.error('Error al cancelar ensayo:', error);
        showToast('Error al cancelar ensayo', 'error');
        return null;
    }
}

// ====================================
// API: ANÁLISIS
// ====================================

async function obtenerAnalisisEnsayo(ensayoId) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.ANALISIS}/ensayo/${ensayoId}`);
    } catch (error) {
        console.error('Error al obtener análisis:', error);
        return null;
    }
}

async function obtenerAnormalesEnsayo(ensayoId) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.ANALISIS}/ensayo/${ensayoId}/anormales`);
    } catch (error) {
        console.error('Error al obtener anormales:', error);
        return [];
    }
}

async function obtenerResumenAnalisis(ensayoId) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.ANALISIS}/ensayo/${ensayoId}/resumen`);
    } catch (error) {
        console.error('Error al obtener resumen:', error);
        return {};
    }
}

// ====================================
// API: REPORTES
// ====================================

async function obtenerReportes() {
    try {
        return await api.get(API_CONFIG.ENDPOINTS.REPORTES);
    } catch (error) {
        console.error('Error al obtener reportes:', error);
        return [];
    }
}

async function obtenerReporte(ensayoId) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.REPORTES}/ensayo/${ensayoId}`);
    } catch (error) {
        console.error('Error al obtener reporte:', error);
        return null;
    }
}

async function generarReporte(ensayoId, datos) {
    try {
        const response = await api.post(`${API_CONFIG.ENDPOINTS.REPORTES}`, {
            ensayoId: ensayoId,
            tipo: datos.tipo,
            generadoPor: datos.generadoPor
        });
        showToast('✓ Reporte generado exitosamente', 'success');
        // Trigger download according to requested type. Use fetch+blob download which is more reliable
        try {
            if (datos && datos.tipo && datos.tipo.toUpperCase() === 'PDF') {
                await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download/pdf`, `reporte_ensayo_${ensayoId}.pdf`);
            } else {
                await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download`, `reporte_ensayo_${ensayoId}.html`);
            }
        } catch (err) {
            console.warn('No se pudo iniciar descarga automática por fetch+blob:', err);
            // As a last resort try opening the download URL in a new tab — may be blocked by popup blockers
            try {
                const isPdf = datos && datos.tipo && datos.tipo.toUpperCase() === 'PDF';
                const downloadEndpoint = isPdf
                    ? `${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download/pdf`
                    : `${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download`;
                const downloadUrl = `${API_CONFIG.BASE_URL}${downloadEndpoint}`;
                const a = document.createElement('a');
                a.href = downloadUrl;
                a.target = '_blank';
                a.rel = 'noopener';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
            } catch (err2) {
                console.error('No se pudo descargar el reporte automáticamente:', err2);
                showToast('Reporte generado, pero no se pudo iniciar la descarga automáticamente. Intente descargarlo manualmente desde la sección Reportes.', 'warning');
            }
        }
        return response;
    } catch (error) {
        console.error('Error al generar reporte:', error);
        showToast('Error al generar reporte: ' + error.message, 'error');
        return null;
    }
}

async function descargarReporte(ensayoId) {
    try {
        await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download`, `reporte_ensayo_${ensayoId}.html`);
        showToast('✓ Descarga iniciada', 'success');
    } catch (error) {
        console.error('Error al descargar reporte:', error);
        showToast('Error al descargar reporte: ' + error.message, 'error');
    }
}

async function descargarReportePdf(ensayoId) {
    try {
        // Primero intentar HEAD para comprobar el content-type sin descargar todo
        const pdfUrl = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download/pdf`;
        let contentType = '';
        try {
            const headRes = await fetch(pdfUrl, { method: 'HEAD' });
            if (headRes && headRes.ok) {
                contentType = headRes.headers.get('content-type') || '';
            }
        } catch (headErr) {
            // Algunos servidores/proxies no permiten HEAD; ignorar y caer al GET
            console.warn('HEAD request failed, falling back to GET for content-type check', headErr);
        }

        if (!contentType) {
            // Intentar GET pero sólo leer headers (fetch siempre descarga body), so use GET and inspect headers
            const getRes = await fetch(pdfUrl, { method: 'GET' });
            contentType = getRes.headers.get('content-type') || '';
            // Si es PDF, abrir en nueva pestaña usando la URL directa
            if (contentType.includes('application/pdf')) {
                const newWindow = window.open(pdfUrl, '_blank', 'noopener,noreferrer');
                if (!newWindow) {
                    await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download/pdf`, `reporte_ensayo_${ensayoId}.pdf`);
                    showToast('✓ Descarga iniciada (popup bloqueado)', 'warning');
                } else {
                    showToast('✓ PDF abierto en nueva pestaña', 'success');
                }
                return;
            }
            // Si no es PDF, procederemos a descargar el HTML
            showToast('PDF no disponible en servidor — descargando HTML en su lugar', 'warning');
            await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download`, `reporte_ensayo_${ensayoId}.html`);
            return;
        }

        if (contentType.includes('application/pdf')) {
            const newWindow = window.open(pdfUrl, '_blank', 'noopener,noreferrer');
            if (!newWindow) {
                await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download/pdf`, `reporte_ensayo_${ensayoId}.pdf`);
                showToast('✓ Descarga iniciada (popup bloqueado)', 'warning');
            } else {
                showToast('✓ PDF abierto en nueva pestaña', 'success');
            }
        } else {
            // Server returned HTML or other content — download HTML instead
            showToast('PDF no disponible en servidor — descargando HTML en su lugar', 'warning');
            await api.downloadFile(`${API_CONFIG.ENDPOINTS.REPORTES}/${ensayoId}/download`, `reporte_ensayo_${ensayoId}.html`);
        }
    } catch (error) {
        console.error('Error al descargar reporte PDF:', error);
        showToast('Error al descargar reporte PDF: ' + error.message, 'error');
    }
}

// ====================================
// API: CARGA DE DATOS
// ====================================

async function cargarArchivoCSV(ensayoId, file, pdfFile = null) {
    try {
        let response;
        if (pdfFile) {
            response = await api.uploadFileWithPdf(`${API_CONFIG.ENDPOINTS.CARGA}/csv/${ensayoId}`, file, pdfFile);
        } else {
            response = await api.uploadFile(`${API_CONFIG.ENDPOINTS.CARGA}/csv/${ensayoId}`, file);
        }
        showToast('✓ Archivo CSV cargado exitosamente', 'success');
        return response;
    } catch (error) {
        console.error('Error al cargar CSV:', error);
        showToast('Error al cargar CSV: ' + error.message, 'error');
        return null;
    }
}

async function cargarArchivoTXT(ensayoId, file) {
    try {
        const response = await api.uploadFile(`${API_CONFIG.ENDPOINTS.CARGA}/txt/${ensayoId}`, file);
        showToast('✓ Archivo TXT cargado exitosamente', 'success');
        return response;
    } catch (error) {
        console.error('Error al cargar TXT:', error);
        showToast('Error al cargar TXT: ' + error.message, 'error');
        return null;
    }
}

// ====================================
// API: EXPORTACIÓN
// ====================================

async function descargarExcelDatos(ensayoId) {
    try {
        const ensayo = await obtenerEnsayo(ensayoId);
        const filename = `ensayo_${ensayoId}_datos_${new Date().toISOString().slice(0, 10)}.xlsx`;
        await api.downloadFile(`${API_CONFIG.ENDPOINTS.EXPORTAR}/excel/datos/${ensayoId}`, filename);
        showToast('✓ Excel descargado', 'success');
    } catch (error) {
        console.error('Error al descargar Excel:', error);
        showToast('Error al descargar Excel', 'error');
    }
}

async function descargarExcelReporte(ensayoId) {
    try {
        const filename = `reporte_${ensayoId}_${new Date().toISOString().slice(0, 10)}.xlsx`;
        await api.downloadFile(`${API_CONFIG.ENDPOINTS.EXPORTAR}/excel/reporte/${ensayoId}`, filename);
        showToast('✓ Reporte descargado', 'success');
    } catch (error) {
        console.error('Error al descargar reporte:', error);
        showToast('Error al descargar reporte', 'error');
    }
}

async function descargarCSV(ensayoId) {
    try {
        const filename = `ensayo_${ensayoId}_datos_${new Date().toISOString().slice(0, 10)}.csv`;
        await api.downloadFile(`${API_CONFIG.ENDPOINTS.EXPORTAR}/csv/${ensayoId}`, filename);
        showToast('✓ CSV descargado', 'success');
    } catch (error) {
        console.error('Error al descargar CSV:', error);
        showToast('Error al descargar CSV', 'error');
    }
}

// ====================================
// API: UTILIDADES
// ====================================

async function obtenerReporteCompleto(ensayoId) {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.UTILIDADES}/reporte-completo/${ensayoId}`);
    } catch (error) {
        console.error('Error al obtener reporte completo:', error);
        return null;
    }
}

async function obtenerSalud() {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.UTILIDADES}/salud`);
    } catch (error) {
        console.error('Error al verificar salud:', error);
        return null;
    }
}

async function obtenerInfo() {
    try {
        return await api.get(`${API_CONFIG.ENDPOINTS.UTILIDADES}/info`);
    } catch (error) {
        console.error('Error al obtener info:', error);
        return null;
    }
}

// ====================================
// CORRECCIONES API
// ====================================

async function subirArchivoCorreccion(ensayoId, archivo, descripcion = '', subidoPor = 'Sistema') {
    try {
        const formData = new FormData();
        formData.append('archivo', archivo);
        formData.append('descripcion', descripcion);
        formData.append('subidoPor', subidoPor);

        const response = await fetch(`${API_CONFIG.BASE_URL}/correcciones/ensayo/${ensayoId}`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error subiendo corrección:', error);
        throw error;
    }
}

async function obtenerCorreccionesPorEnsayo(ensayoId) {
    try {
        return await api.get(`/correcciones/ensayo/${ensayoId}`);
    } catch (error) {
        console.error('Error obteniendo correcciones:', error);
        return [];
    }
}

async function obtenerCorreccion(id) {
    try {
        return await api.get(`/correcciones/${id}`);
    } catch (error) {
        console.error('Error obteniendo corrección:', error);
        return null;
    }
}
async function eliminarCorreccion(id) {
    try {
        return await api.delete(`/correcciones/${id}`);
    } catch (error) {
        console.error('Error eliminando corrección:', error);
        throw error;
    }
}

// ====================================
// API - SENSORES
// ====================================

async function obtenerSensores(activos = null) {
    try {
        const params = activos !== null ? `?activos=${activos}` : '';
        return await api.get(`/sensores${params}`);
    } catch (error) {
        console.error('Error obteniendo sensores:', error);
        return [];
    }
}

async function obtenerSensor(id) {
    try {
        return await api.get(`/sensores/${id}`);
    } catch (error) {
        console.error('Error obteniendo sensor:', error);
        return null;
    }
}

async function crearSensor(sensor) {
    try {
        return await api.post('/sensores', sensor);
    } catch (error) {
        console.error('Error creando sensor:', error);
        throw error;
    }
}

async function actualizarSensor(id, sensor) {
    try {
        return await api.put(`/sensores/${id}`, sensor);
    } catch (error) {
        console.error('Error actualizando sensor:', error);
        throw error;
    }
}

async function eliminarSensor(id) {
    try {
        return await api.delete(`/sensores/${id}`);
    } catch (error) {
        console.error('Error eliminando sensor:', error);
        throw error;
    }
}

async function obtenerSensoresProximosCalibracion() {
    try {
        return await api.get('/sensores/alertas/calibracion?diasAnticipacion=30');
    } catch (error) {
        console.error('Error obteniendo sensores próximos a calibración:', error);
        return [];
    }
}

// ====================================
// API - LOGTAG DOCUMENTOS
// ====================================

/**
 * Subir documento logtag con categoría
 */
async function subirLogtag(archivo, ensayoId, categoria, descripcion = '', subidoPor = 'Sistema') {
    try {
        const formData = new FormData();
        formData.append('archivo', archivo);
        if (ensayoId) formData.append('ensayoId', ensayoId);
        formData.append('categoria', categoria);
        if (descripcion) formData.append('descripcion', descripcion);
        if (subidoPor) formData.append('subidoPor', subidoPor);

        const response = await fetch(`${API_CONFIG.BASE_URL}/logtag`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error subiendo documento logtag:', error);
        throw error;
    }
}

/**
 * Listar todos los documentos
 */
async function obtenerLogtags() {
    try {
        return await api.get('/logtag');
    } catch (error) {
        console.error('Error obteniendo logtags:', error);
        return [];
    }
}

/**
 * Listar documentos por categoría
 */
async function obtenerLogtagsPorCategoria(categoria) {
    try {
        return await api.get(`/logtag/categoria/${categoria}`);
    } catch (error) {
        console.error('Error obteniendo logtags por categoría:', error);
        return [];
    }
}

/**
 * Listar documentos de un ensayo
 */
async function obtenerLogtagsPorEnsayo(ensayoId) {
    try {
        return await api.get(`/logtag/ensayo/${ensayoId}`);
    } catch (error) {
        console.error('Error obteniendo logtags del ensayo:', error);
        return [];
    }
}

/**
 * Obtener documento por ID
 */
async function obtenerLogtag(id) {
    try {
        return await api.get(`/logtag/${id}`);
    } catch (error) {
        console.error('Error obteniendo logtag:', error);
        return null;
    }
}

/**
 * Eliminar documento
 */
async function eliminarLogtag(id) {
    try {
        return await api.delete(`/logtag/${id}`);
    } catch (error) {
        console.error('Error eliminando logtag:', error);
        throw error;
    }
}

/**
 * Obtener estadísticas de documentos
 */
async function obtenerEstadisticasLogtag() {
    try {
        return await api.get('/logtag/estadisticas');
    } catch (error) {
        console.error('Error obteniendo estadísticas:', error);
        return { total: 0, logtags: 0, sensores: 0 };
    }
}
