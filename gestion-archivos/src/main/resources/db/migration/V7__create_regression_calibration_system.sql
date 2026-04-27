-- Migration: Add calibration_sessions table and related tables for regression-based calibration system
-- This migration creates the new calibration system while maintaining backward compatibility with the legacy system

-- Create calibration_sessions table
CREATE TABLE IF NOT EXISTS calibration_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    calibration_date DATETIME NOT NULL,
    uploaded_date DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    active_model_type VARCHAR(50),
    name VARCHAR(255),
    description VARCHAR(1000),
    uploaded_by VARCHAR(255),
    source_file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size_bytes BIGINT,
    drift_data VARCHAR(2000),
    metadata LONGTEXT,
    expiration_date DATETIME,
    INDEX idx_device_id (device_id),
    INDEX idx_device_active (device_id, is_active),
    INDEX idx_calibration_date (calibration_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create calibration_points table
CREATE TABLE IF NOT EXISTS calibration_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calibration_id BIGINT NOT NULL,
    point_order INT NOT NULL,
    patron_reading DOUBLE NOT NULL,
    instrument_reading DOUBLE NOT NULL,
    correction DOUBLE NOT NULL,
    metadata VARCHAR(500),
    FOREIGN KEY (calibration_id) REFERENCES calibration_sessions(id) ON DELETE CASCADE,
    INDEX idx_calibration_id (calibration_id),
    INDEX idx_device_point (calibration_id, point_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create regression_models table
CREATE TABLE IF NOT EXISTS regression_models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calibration_id BIGINT NOT NULL,
    model_type VARCHAR(50) NOT NULL,
    coefficient0 DOUBLE NOT NULL,
    coefficient1 DOUBLE NOT NULL,
    coefficient2 DOUBLE,
    coefficient3 DOUBLE,
    r_squared DOUBLE NOT NULL,
    standard_error DOUBLE,
    FOREIGN KEY (calibration_id) REFERENCES calibration_sessions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_calibration_model (calibration_id, model_type),
    INDEX idx_calibration_id (calibration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add calibration_session_id column to legacy calibraciones_sensor table
ALTER TABLE calibraciones_sensor ADD COLUMN calibration_session_id BIGINT DEFAULT NULL AFTER notas;
ALTER TABLE calibraciones_sensor ADD INDEX idx_calibration_session_id (calibration_session_id);

-- Add comment for backward compatibility
ALTER TABLE calibraciones_sensor COMMENT = 'DEPRECATED: Use calibration_sessions table. This table is maintained for backward compatibility only.';
