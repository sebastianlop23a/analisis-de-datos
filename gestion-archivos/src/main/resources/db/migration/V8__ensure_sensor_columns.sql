-- Migration V8: Ensure sensor table contains expected columns
-- This script uses MySQL 8's "ADD COLUMN IF NOT EXISTS" to be idempotent.

ALTER TABLE sensores
  ADD COLUMN IF NOT EXISTS codigo VARCHAR(100) NOT NULL,
  ADD COLUMN IF NOT EXISTS ubicacion VARCHAR(255),
  ADD COLUMN IF NOT EXISTS tipo_sonda VARCHAR(100),
  ADD COLUMN IF NOT EXISTS modelo VARCHAR(100),
  ADD COLUMN IF NOT EXISTS fabricante VARCHAR(255),
  ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS ultima_calibracion DATE,
  ADD COLUMN IF NOT EXISTS proxima_calibracion DATE,
  ADD COLUMN IF NOT EXISTS frecuencia_calibracion_dias INT NOT NULL DEFAULT 365,
  ADD COLUMN IF NOT EXISTS rango_minimo DOUBLE,
  ADD COLUMN IF NOT EXISTS rango_maximo DOUBLE,
  ADD COLUMN IF NOT EXISTS `precision` DOUBLE,
  ADD COLUMN IF NOT EXISTS observaciones TEXT;

-- Create a simple index on proxima_calibracion to speed up alerts queries (if supported)
ALTER TABLE sensores ADD INDEX IF NOT EXISTS idx_proxima_calibracion (proxima_calibracion);

-- NOTE: If you use an older MySQL (<8.0) that doesn't support "IF NOT EXISTS" for ADD COLUMN,
-- run equivalent checks against information_schema and add columns conditionally, or upgrade MySQL.
