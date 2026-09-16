-- ==============================================================================
-- MIGRACIÓN V12: Tabla de Tokens de Autenticación (Invitación & Recuperación de Contraseña)
-- CDA San Pedro S.A.S.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS tokens_autenticacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    empleado_id UUID REFERENCES empleados(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL, -- 'INVITACION', 'RECUPERACION_PASSWORD'
    email_destinatario VARCHAR(150) NOT NULL,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_uso TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens_autenticacion(token);
CREATE INDEX IF NOT EXISTS idx_tokens_usuario ON tokens_autenticacion(usuario_id);
CREATE INDEX IF NOT EXISTS idx_tokens_empleado ON tokens_autenticacion(empleado_id);
CREATE INDEX IF NOT EXISTS idx_tokens_tipo_usado ON tokens_autenticacion(tipo, usado);
