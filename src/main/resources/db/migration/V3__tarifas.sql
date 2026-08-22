-- ==============================================================================
-- MIGRACIÓN V3: Tarifas y Precios Oficiales Dinámicos por Categoría de Vehículo
-- CDA San Pedro S.A.S.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS tarifas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria VARCHAR(50) NOT NULL UNIQUE,
    nombre_servicio VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio NUMERIC(14, 2) NOT NULL CHECK (precio >= 0),
    iva_porcentaje NUMERIC(5, 2) DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tarifas_categoria ON tarifas(categoria);
CREATE INDEX IF NOT EXISTS idx_tarifas_activo ON tarifas(activo);

-- Seed de Tarifas de Referencia Iniciales (Año 2026)
INSERT INTO tarifas (id, categoria, nombre_servicio, descripcion, precio, iva_porcentaje, activo)
VALUES 
    (gen_random_uuid(), 'MOTO', 'RTM & Emisiones - Motocicletas (4T / 2T)', 'Revisión Técnico-Mecánica y análisis de emisiones para motocicletas y motocarros', 210000.00, 0.00, true),
    (gen_random_uuid(), 'LIVIANO', 'RTM & Emisiones - Vehículos Particulares Livianos', 'Revisión para automóviles, camionetas, camperos y vans de servicio particular', 320000.00, 0.00, true),
    (gen_random_uuid(), 'PUBLICO', 'RTM & Emisiones - Vehículos de Servicio Público', 'Revisión reglamentaria anual para taxis, colectivos y vans de servicio público', 350000.00, 0.00, true),
    (gen_random_uuid(), 'PESADO', 'RTM & Emisiones - Vehículos Pesados & Camiones', 'Revisión para camiones, tractomulas, buses y busetas (> 3.5 Ton)', 450000.00, 0.00, true)
ON CONFLICT (categoria) DO NOTHING;
