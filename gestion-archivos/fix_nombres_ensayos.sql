-- Script para actualizar nombres de ensayos que están mal guardados
USE gestion_archivos;

-- Ver los ensayos actuales
SELECT id, nombre, maquina_id, estado FROM ensayos ORDER BY id;

-- Actualizar ensayos que tienen nombres vacíos o que siguen el patrón "ensayo_X"
-- Asignarles un nombre basado en la máquina y la fecha
UPDATE ensayos e
INNER JOIN maquinas m ON e.maquina_id = m.id
SET e.nombre = CONCAT(
    m.nombre, 
    ' - ', 
    DATE_FORMAT(e.fecha_inicio, '%Y%m%d_%H%i')
)
WHERE e.nombre IS NULL 
   OR e.nombre = '' 
   OR e.nombre REGEXP '^[Ee]nsayo #?[0-9]+$'
   OR e.nombre REGEXP '^ensayo_[0-9]+$';

-- Ver los resultados actualizados
SELECT id, nombre, maquina_id, estado FROM ensayos ORDER BY id;
