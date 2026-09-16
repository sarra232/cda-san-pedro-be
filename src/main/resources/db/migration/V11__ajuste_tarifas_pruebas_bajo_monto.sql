-- ==============================================================================
-- MIGRACIÓN V11: Ajuste de Tarifas de Prueba para Facturación Electrónica (< $2.000 COP)
-- CDA San Pedro S.A.S.
-- Mantiene el desglose proporcional de los 7 conceptos pero asegura montos <= $2.000 COP
-- ==============================================================================

-- 1. PESADO: Total $1.900,00 COP
UPDATE tarifas 
SET valor_servicio = 1380.00,
    iva = 262.20,
    runt = 30.00,
    sicov = 170.00,
    operador = 50.00,
    seguridad_vial = 5.00,
    fupa = 2.80,
    precio = 1900.00,
    iva_porcentaje = 19.00
WHERE categoria = 'PESADO' AND (codigo = 'RTM-PES-2026' OR tipo_servicio = 'RTM_LEGAL');

-- 2. PUBLICO: Total $1.750,00 COP
UPDATE tarifas 
SET valor_servicio = 1270.00,
    iva = 241.30,
    runt = 30.00,
    sicov = 150.00,
    operador = 50.00,
    seguridad_vial = 5.00,
    fupa = 3.70,
    precio = 1750.00,
    iva_porcentaje = 19.00
WHERE categoria = 'PUBLICO' AND (codigo = 'RTM-PUB-2026' OR tipo_servicio = 'RTM_LEGAL');

-- 3. LIVIANO: Total $1.600,00 COP
UPDATE tarifas 
SET valor_servicio = 1155.00,
    iva = 219.45,
    runt = 30.00,
    sicov = 140.00,
    operador = 50.00,
    seguridad_vial = 4.00,
    fupa = 1.55,
    precio = 1600.00,
    iva_porcentaje = 19.00
WHERE categoria = 'LIVIANO' AND (codigo = 'RTM-LIV-2026' OR (tipo_servicio = 'RTM_LEGAL' AND nombre_servicio ILIKE '%LIVIANO%'));

-- 4. MOTO: Total $1.200,00 COP
UPDATE tarifas 
SET valor_servicio = 850.00,
    iva = 161.50,
    runt = 30.00,
    sicov = 110.00,
    operador = 43.50,
    seguridad_vial = 3.00,
    fupa = 2.00,
    precio = 1200.00,
    iva_porcentaje = 19.00
WHERE categoria = 'MOTO' AND (codigo = 'RTM-MOTO-2026' OR tipo_servicio = 'RTM_LEGAL');

-- 5. PERITAJE LIVIANO: Total $1.500,00 COP
UPDATE tarifas
SET valor_servicio = 1260.50,
    iva = 239.50,
    runt = 0.00,
    sicov = 0.00,
    operador = 0.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 1500.00,
    iva_porcentaje = 19.00
WHERE codigo = 'PERIT-LIV-2026';

-- 6. PREVENTIVA LIVIANOS: Total $1.000,00 COP
UPDATE tarifas
SET valor_servicio = 840.34,
    iva = 159.66,
    runt = 0.00,
    sicov = 0.00,
    operador = 0.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 1000.00,
    iva_porcentaje = 19.00
WHERE codigo = 'PREV-LIV-2026';

-- 7. PREVENTIVA MOTOS: Total $800,00 COP
UPDATE tarifas
SET valor_servicio = 672.27,
    iva = 127.73,
    runt = 0.00,
    sicov = 0.00,
    operador = 0.00,
    seguridad_vial = 0.00,
    fupa = 0.00,
    precio = 800.00,
    iva_porcentaje = 19.00
WHERE codigo = 'PREV-MOTO-2026';
