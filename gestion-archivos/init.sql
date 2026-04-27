-- Crear base de datos
CREATE DATABASE IF NOT EXISTS gestion_archivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gestion_archivos;

-- Tabla de Máquinas
CREATE TABLE IF NOT EXISTS maquinas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  tipo VARCHAR(100) NOT NULL,
  descripcion TEXT,
  limite_inferior DOUBLE NOT NULL,
  limite_superior DOUBLE NOT NULL,
  unidad_medida VARCHAR(50) NOT NULL,
  activa BOOLEAN DEFAULT TRUE,
  ubicacion VARCHAR(255),
  modelo VARCHAR(100),
  numero_serie VARCHAR(100),
  calcular_fh BOOLEAN DEFAULT FALSE COMMENT 'Indica si se debe calcular Factor Histórico',
  parametro_z DOUBLE DEFAULT 14.0 COMMENT 'Parámetro Z para cálculo FH',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_tipo (tipo),
  INDEX idx_activa (activa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de Ensayos
CREATE TABLE IF NOT EXISTS ensayos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  maquina_id BIGINT NOT NULL,
  fecha_inicio TIMESTAMP NOT NULL,
  fecha_fin TIMESTAMP,
  estado VARCHAR(50) NOT NULL,
  descripcion TEXT,
  responsable VARCHAR(255),
  observaciones LONGTEXT,
  temperatura_promedio DOUBLE,
  temperatura_maxima DOUBLE,
  temperatura_minima DOUBLE,
  total_registros INT,
  registros_anormales INT,
  factor_historico DOUBLE COMMENT 'Factor Histórico calculado (si aplica)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (maquina_id) REFERENCES maquinas(id) ON DELETE CASCADE,
  INDEX idx_estado (estado),
  INDEX idx_maquina (maquina_id),
  INDEX idx_fecha_inicio (fecha_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de Reportes
CREATE TABLE IF NOT EXISTS reportes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ensayo_id BIGINT NOT NULL UNIQUE,
  fecha_generacion TIMESTAMP NOT NULL,
  ruta VARCHAR(500),
  tipo VARCHAR(50) NOT NULL,
  contenido LONGTEXT,
  generado_por VARCHAR(255),
  num_paginas INT,
  media DOUBLE,
  desviacion_estandar DOUBLE,
  datos_anormales INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (ensayo_id) REFERENCES ensayos(id) ON DELETE CASCADE,
  INDEX idx_tipo (tipo),
  INDEX idx_ensayo (ensayo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar datos de prueba (con nombres únicos para evitar conflictos)
INSERT INTO maquinas (nombre, tipo, descripcion, limite_inferior, limite_superior, unidad_medida, ubicacion, modelo, numero_serie) VALUES
('Horno Térmico A1 - Test', 'Horno', 'Horno industrial de precisión', 20, 150, '°C', 'Laboratorio 1', 'HT-2000', 'SN-001'),
('Cámara Térmica B2 - Test', 'Cámara Térmica', 'Cámara de pruebas ambientales', -10, 80, '°C', 'Laboratorio 2', 'CT-500', 'SN-002'),
('Incubadora C3 - Test', 'Incubadora', 'Incubadora de cultivos', 37, 45, '°C', 'Laboratorio 3', 'INC-100', 'SN-003')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre);  -- Ignora duplicados si existen

-- Crear usuario para la aplicación
CREATE USER IF NOT EXISTS 'usuario'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON gestion_archivos.* TO 'usuario'@'%';
FLUSH PRIVILEGES;
