-- Migración V6: Módulo de Integración de Facturación Electrónica DIAN con SIIGO API Cloud
-- Compatible con PostgreSQL 16 y Clean Architecture

-- 1. Tabla: mapeo_catalogo_siigo (Homologación de servicios/categorías del CDA a códigos de producto SIIGO)
CREATE TABLE IF NOT EXISTS mapeo_catalogo_siigo (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    categoria_cda VARCHAR(30) NOT NULL UNIQUE,
    codigo_producto_siigo VARCHAR(50) NOT NULL,
    descripcion_siigo VARCHAR(200) NOT NULL,
    tarifa_iva NUMERIC(5,2) NOT NULL DEFAULT 19.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mapeo_siigo_categoria ON mapeo_catalogo_siigo(categoria_cda);

-- Semilla inicial de mapeo por defecto para CDA San Pedro
INSERT INTO mapeo_catalogo_siigo (categoria_cda, codigo_producto_siigo, descripcion_siigo, tarifa_iva)
VALUES 
    ('MOTO', 'RTM-MOTO-01', 'Revisión Técnico-Mecánica y Emisiones Contaminantes - Motocicletas', 19.00),
    ('LIVIANO', 'RTM-LIV-01', 'Revisión Técnico-Mecánica y Emisiones Contaminantes - Vehículos Livianos', 19.00),
    ('PESADO', 'RTM-PES-01', 'Revisión Técnico-Mecánica y Emisiones Contaminantes - Vehículos Pesados', 19.00),
    ('PUBLICO', 'RTM-PUB-01', 'Revisión Técnico-Mecánica y Emisiones Contaminantes - Servicio Público', 19.00),
    ('PREVENTIVA', 'REV-PREV-01', 'Revisión Preventiva y Peritaje CDA San Pedro', 19.00),
    ('REINSPECCION_GRATUITA', 'RTM-REINSP-01', '2da Revisión / Reinspección RTM Gratuita (15 Días)', 0.00)
ON CONFLICT (categoria_cda) DO NOTHING;

-- 2. Tabla: facturas_electronicas_dian (Trazabilidad 1-a-1 de Facturas Emitidas ante la DIAN vía SIIGO)
CREATE TABLE IF NOT EXISTS facturas_electronicas_dian (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    factura_id UUID NOT NULL REFERENCES facturas(id) ON DELETE CASCADE UNIQUE,
    ambiente VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    siigo_invoice_id VARCHAR(100),
    numero_factura_siigo VARCHAR(50),
    cufe VARCHAR(255),
    qr_dian TEXT,
    pdf_siigo_url VARCHAR(500),
    estado_dian VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_respuesta TEXT,
    intentos INTEGER NOT NULL DEFAULT 0,
    payload_enviado JSONB NOT NULL DEFAULT '{}'::jsonb,
    respuesta_siigo JSONB NOT NULL DEFAULT '{}'::jsonb,
    fecha_emision_dian TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facturas_dian_factura ON facturas_electronicas_dian(factura_id);
CREATE INDEX IF NOT EXISTS idx_facturas_dian_cufe ON facturas_electronicas_dian(cufe);
CREATE INDEX IF NOT EXISTS idx_facturas_dian_estado ON facturas_electronicas_dian(estado_dian);
