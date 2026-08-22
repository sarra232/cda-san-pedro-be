-- ==============================================================================
-- MIGRACIÓN V4: Catálogo Integral de Servicios y Tarifas CDA San Pedro
-- Permite múltiples servicios por categoría, creación, edición y eliminación.
-- ==============================================================================

-- 1. Eliminar restricción de categoría única para permitir múltiples servicios por categoría
ALTER TABLE tarifas DROP CONSTRAINT IF EXISTS tarifas_categoria_key;

-- 2. Agregar columnas de código y tipo de servicio
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS codigo VARCHAR(50);
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS tipo_servicio VARCHAR(50) DEFAULT 'RTM_LEGAL';

-- 3. Actualizar códigos de los registros existentes
UPDATE tarifas SET codigo = 'RTM-MOTO-2026', tipo_servicio = 'RTM_LEGAL' WHERE categoria = 'MOTO' AND (codigo IS NULL OR codigo = '');
UPDATE tarifas SET codigo = 'RTM-LIV-2026', tipo_servicio = 'RTM_LEGAL' WHERE categoria = 'LIVIANO' AND (codigo IS NULL OR codigo = '');
UPDATE tarifas SET codigo = 'RTM-PUB-2026', tipo_servicio = 'RTM_LEGAL' WHERE categoria = 'PUBLICO' AND (codigo IS NULL OR codigo = '');
UPDATE tarifas SET codigo = 'RTM-PES-2026', tipo_servicio = 'RTM_LEGAL' WHERE categoria = 'PESADO' AND (codigo IS NULL OR codigo = '');

-- 4. Insertar servicios adicionales del catálogo oficial
INSERT INTO tarifas (id, codigo, categoria, tipo_servicio, nombre_servicio, descripcion, precio, iva_porcentaje, activo)
VALUES 
    (gen_random_uuid(), 'PREV-LIV-2026', 'LIVIANO', 'PREVENTIVA', 'Revisión Preventiva General - Livianos', 'Diagnóstico preventivo de frenos, suspensión, luces y alineación para vehículos livianos', 120000.00, 0.00, true),
    (gen_random_uuid(), 'PREV-MOTO-2026', 'MOTO', 'PREVENTIVA', 'Revisión Preventiva - Motocicletas', 'Revisión de seguridad y diagnóstico general de frenos y luces para motos', 80000.00, 0.00, true),
    (gen_random_uuid(), 'PERIT-LIV-2026', 'LIVIANO', 'PERITAJE', 'Peritaje Comercial para Compra/Venta', 'Inspección técnica integral, estado de carrocería, chasis, motor y avalúo técnico', 180000.00, 0.00, true),
    (gen_random_uuid(), 'REINSP-GEN-2026', 'LIVIANO', 'REINSPECCION', 'Reinspección Reglamentaria (2do Intento)', 'Segunda revisión dentro de los 15 días hábiles conforme a la Resolución 5202', 0.00, 0.00, true)
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_tarifas_codigo ON tarifas(codigo);
CREATE INDEX IF NOT EXISTS idx_tarifas_tipo_servicio ON tarifas(tipo_servicio);
