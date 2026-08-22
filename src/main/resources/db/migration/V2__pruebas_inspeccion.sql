-- Migración V2: Control de 4 Pruebas Técnicas en Pista y Trazabilidad de Responsables - CDA San Pedro

CREATE TABLE IF NOT EXISTS pruebas_inspeccion (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orden_ingreso_id UUID NOT NULL REFERENCES ordenes_ingreso(id) ON DELETE CASCADE,
    tipo_prueba VARCHAR(50) NOT NULL,
    estado VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE',
    observaciones TEXT,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    fecha_ejecucion TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_orden_tipo_prueba UNIQUE (orden_ingreso_id, tipo_prueba)
);

CREATE INDEX IF NOT EXISTS idx_pruebas_orden ON pruebas_inspeccion(orden_ingreso_id);
CREATE INDEX IF NOT EXISTS idx_pruebas_tipo ON pruebas_inspeccion(tipo_prueba);
CREATE INDEX IF NOT EXISTS idx_pruebas_estado ON pruebas_inspeccion(estado);
CREATE INDEX IF NOT EXISTS idx_pruebas_usuario ON pruebas_inspeccion(usuario_id);
