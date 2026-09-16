-- ==============================================================================
-- MIGRACIÓN V8: Desglose de Tarifas Oficiales RTM (Regulación MinTransporte / SICOV / RUNT)
-- CDA San Pedro S.A.S.
-- ==============================================================================

-- 1. Agregar columnas para los 7 conceptos desglosados
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS valor_servicio NUMERIC(14, 2) DEFAULT 0.00;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS iva NUMERIC(14, 2) DEFAULT 0.00;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS runt NUMERIC(14, 2) DEFAULT 0.00;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS sicov NUMERIC(14, 2) DEFAULT 0.00;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS operador NUMERIC(14, 2) DEFAULT 0.00;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS seguridad_vial NUMERIC(14, 2) DEFAULT 0.00;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS fupa NUMERIC(14, 2) DEFAULT 0.00;

-- 2. Actualizar tarifas base existentes con desglose oficial de referencia (Valores 2026)

-- PESADO: Total 386.316 (o 450.000 según tipo)
UPDATE tarifas 
SET valor_servicio = 281508.00,
    iva = 53487.00,
    runt = 5500.00,
    sicov = 35492.00,
    operador = 10329.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 386316.00,
    iva_porcentaje = 19.00
WHERE categoria = 'PESADO' AND (codigo = 'RTM-PES-2026' OR tipo_servicio = 'RTM_LEGAL');

-- LIVIANO: Total 320.000 (Servicio: 231.500, IVA: 43.985, RUNT: 5.500, SICOV: 28.500, Operador: 10.515)
UPDATE tarifas 
SET valor_servicio = 231500.00,
    iva = 43985.00,
    runt = 5500.00,
    sicov = 28500.00,
    operador = 10515.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 320000.00,
    iva_porcentaje = 19.00
WHERE categoria = 'LIVIANO' AND (codigo = 'RTM-LIV-2026' OR (tipo_servicio = 'RTM_LEGAL' AND nombre_servicio ILIKE '%LIVIANO%'));

-- PUBLICO: Total 350.000 (Servicio: 255.000, IVA: 48.450, RUNT: 5.500, SICOV: 30.500, Operador: 10.550)
UPDATE tarifas 
SET valor_servicio = 255000.00,
    iva = 48450.00,
    runt = 5500.00,
    sicov = 30500.00,
    operador = 10550.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 350000.00,
    iva_porcentaje = 19.00
WHERE categoria = 'PUBLICO' AND (codigo = 'RTM-PUB-2026' OR tipo_servicio = 'RTM_LEGAL');

-- MOTO: Total 225.000 (Servicio: 158.500, IVA: 30.115, RUNT: 5.500, SICOV: 22.500, Operador: 8.385)
UPDATE tarifas 
SET valor_servicio = 158500.00,
    iva = 30115.00,
    runt = 5500.00,
    sicov = 22500.00,
    operador = 8385.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 225000.00,
    iva_porcentaje = 19.00
WHERE categoria = 'MOTO' AND (codigo = 'RTM-MOTO-2026' OR tipo_servicio = 'RTM_LEGAL');

-- Asegurar que los servicios preventivos tengan su valor base
UPDATE tarifas
SET valor_servicio = ROUND(precio / 1.19, 2),
    iva = precio - ROUND(precio / 1.19, 2),
    iva_porcentaje = 19.00
WHERE (valor_servicio IS NULL OR valor_servicio = 0) AND precio > 0;
