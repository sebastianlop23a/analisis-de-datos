// ====================================
// CONFIGURACIÓN DE LA API
// ====================================

const API_CONFIG = {
    BASE_URL: 'http://localhost:8080/api',
    ENDPOINTS: {
        MAQUINAS: '/maquinas',
        ENSAYOS: '/ensayos',
        ANALISIS: '/analisis',
        REPORTES: '/reportes',
        CARGA: '/carga',
        EXPORTAR: '/exportar',
        UTILIDADES: '/utilidades'
    },
    TIMEOUT: 10000,
    RETRY_ATTEMPTS: 3,
    RETRY_DELAY: 1000
};

// Timeouts y delays
const DELAYS = {
    TOAST_DURATION: 4000,
    CHART_UPDATE: 500,
    API_POLL: 30000, // 30 segundos - reducido para evitar tráfico excesivo
    DEBOUNCE: 300
};

// Estados
const ESTADOS = {
    EN_PROGRESO: 'EN_PROGRESO',
    COMPLETADO: 'COMPLETADO',
    PAUSADO: 'PAUSADO',
    CANCELADO: 'CANCELADO',
    REPORTE_GENERADO: 'REPORTE_GENERADO'
};

// Colores para gráficos
const CHART_COLORS = {
    primary: 'rgb(52, 152, 219)',
    secondary: 'rgb(46, 204, 113)',
    danger: 'rgb(231, 76, 60)',
    warning: 'rgb(243, 156, 18)',
    info: 'rgb(155, 89, 182)',
    light: 'rgb(236, 240, 241)',
    dark: 'rgb(44, 62, 80)',
    palette: [
        'rgb(52, 152, 219)',
        'rgb(46, 204, 113)',
        'rgb(231, 76, 60)',
        'rgb(243, 156, 18)',
        'rgb(155, 89, 182)',
        'rgb(26, 188, 156)',
        'rgb(41, 128, 185)',
        'rgb(39, 174, 96)'
    ]
};

// Objeto CONFIG global para acceso desde app.js
const CONFIG = {
    chartColors: CHART_COLORS.palette
};

// Configuración de Chart.js
Chart.defaults.font.family = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";
Chart.defaults.color = '#7f8c8d';

// Función para actualizar colores de gráficos según el tema
function updateChartTheme(isDarkMode) {
    if (isDarkMode) {
        Chart.defaults.color = '#e4e4e4';
        Chart.defaults.borderColor = '#444';
        Chart.defaults.backgroundColor = 'rgba(52, 152, 219, 0.1)';
    } else {
        Chart.defaults.color = '#7f8c8d';
        Chart.defaults.borderColor = '#ddd';
        Chart.defaults.backgroundColor = 'rgba(52, 152, 219, 0.1)';
    }
}

// Aplicar tema inicial
const savedConfig = JSON.parse(localStorage.getItem('appConfig') || '{}');
updateChartTheme(savedConfig.darkMode === true);
