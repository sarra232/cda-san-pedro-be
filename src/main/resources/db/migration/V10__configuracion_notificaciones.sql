-- Migración V10: Tabla de Configuraciones Generales y Parámetros de Notificaciones
CREATE TABLE IF NOT EXISTS configuraciones_sistema (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clave VARCHAR(100) NOT NULL UNIQUE,
    valor TEXT NOT NULL,
    descripcion VARCHAR(255),
    categoria VARCHAR(50) DEFAULT 'NOTIFICACIONES',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Valores iniciales para alertas de tesorería y cuentas por pagar
INSERT INTO configuraciones_sistema (clave, valor, descripcion, categoria)
VALUES 
    ('NOTIFICACIONES_TESORERIA_EMAILS', 'administracion@cdasanpedro.com, gerencia@cdasanpedro.com', 'Lista de correos para alertas de cuentas por pagar', 'TESORERIA'),
    ('NOTIFICACIONES_TESORERIA_TELEFONOS', '3000000000, 3113456789', 'Lista de números telefónicos para alertas SMS/WhatsApp de facturas', 'TESORERIA'),
    ('NOTIFICACIONES_TESORERIA_ACTIVO', 'true', 'Indica si el barrido automático diario de alertas está habilitado', 'TESORERIA'),
    ('NOTIFICACIONES_TESORERIA_HORA', '08:00', 'Hora del día para el barrido automático diario', 'TESORERIA')
ON CONFLICT (clave) DO NOTHING;
