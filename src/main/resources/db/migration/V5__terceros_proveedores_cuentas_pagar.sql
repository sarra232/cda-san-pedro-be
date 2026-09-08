-- Migración V5: Terceros Universales, Roles Polimórficos, Reinspecciones (15 días), Proveedores y Cuentas por Pagar
-- Compatible con PostgreSQL 16 y preparado para futuras integraciones sin migraciones destructivas

-- 1. Tabla: terceros (Entidad Maestra Universal de Personas y Empresas)
CREATE TABLE IF NOT EXISTS terceros (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_documento VARCHAR(15) NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(25) NOT NULL UNIQUE,
    digito_verificacion VARCHAR(2),
    tipo_persona VARCHAR(20) NOT NULL DEFAULT 'NATURAL',
    razon_social_o_nombre VARCHAR(200) NOT NULL,
    primer_nombre VARCHAR(60),
    otros_nombres VARCHAR(60),
    primer_apellido VARCHAR(60),
    segundo_apellido VARCHAR(60),
    celular_principal VARCHAR(25) NOT NULL,
    telefono_secundario VARCHAR(25),
    email_principal VARCHAR(120),
    email_facturacion VARCHAR(120),
    direccion VARCHAR(250),
    municipio_dane VARCHAR(10),
    departamento_dane VARCHAR(5),
    responsabilidad_fiscal VARCHAR(20) DEFAULT 'R-99-PN',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_terceros_documento ON terceros(numero_documento);
CREATE INDEX IF NOT EXISTS idx_terceros_celular ON terceros(celular_principal);
CREATE INDEX IF NOT EXISTS idx_terceros_email ON terceros(email_principal);
CREATE INDEX IF NOT EXISTS idx_terceros_metadata ON terceros USING gin (metadata);

-- 2. Tabla: terceros_roles (Roles Polimórficos: CLIENTE, PROVEEDOR, EMPLEADO, PRESTADOR_SERVICIOS, etc.)
CREATE TABLE IF NOT EXISTS terceros_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tercero_id UUID NOT NULL REFERENCES terceros(id) ON DELETE CASCADE,
    tipo_rol VARCHAR(35) NOT NULL,
    metadata_rol JSONB NOT NULL DEFAULT '{}'::jsonb,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tercero_rol UNIQUE (tercero_id, tipo_rol)
);

CREATE INDEX IF NOT EXISTS idx_terceros_roles_tipo ON terceros_roles(tipo_rol);
CREATE INDEX IF NOT EXISTS idx_terceros_roles_tercero ON terceros_roles(tercero_id);

-- Poblar terceros y roles desde clientes existentes
INSERT INTO terceros (id, tipo_documento, numero_documento, razon_social_o_nombre, celular_principal, email_principal, direccion, created_at, updated_at)
SELECT id, tipo_documento, numero_documento, nombres_razon_social, celular, email, direccion, created_at, updated_at
FROM clientes
ON CONFLICT (numero_documento) DO UPDATE
SET razon_social_o_nombre = EXCLUDED.razon_social_o_nombre,
    celular_principal = EXCLUDED.celular_principal;

INSERT INTO terceros_roles (tercero_id, tipo_rol, activo)
SELECT id, 'CLIENTE', TRUE
FROM clientes
ON CONFLICT (tercero_id, tipo_rol) DO NOTHING;

-- Poblar terceros y roles desde usuarios existentes
INSERT INTO terceros (tipo_documento, numero_documento, razon_social_o_nombre, celular_principal, created_at, updated_at)
SELECT tipo_documento, numero_documento, nombres_apellidos, '3000000000', created_at, updated_at
FROM usuarios
ON CONFLICT (numero_documento) DO NOTHING;

INSERT INTO terceros_roles (tercero_id, tipo_rol, metadata_rol, activo)
SELECT t.id, 'EMPLEADO', jsonb_build_object('rol_usuario', u.rol), TRUE
FROM usuarios u
JOIN terceros t ON t.numero_documento = u.numero_documento
ON CONFLICT (tercero_id, tipo_rol) DO NOTHING;

-- 3. Extender tablas nucleares con metadata JSONB y soporte de Reinspección
ALTER TABLE ordenes_ingreso ADD COLUMN IF NOT EXISTS orden_padre_id UUID REFERENCES ordenes_ingreso(id) ON DELETE SET NULL;
ALTER TABLE ordenes_ingreso ADD COLUMN IF NOT EXISTS es_reinspeccion BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ordenes_ingreso ADD COLUMN IF NOT EXISTS dias_transcurridos_rechazo INTEGER;
ALTER TABLE ordenes_ingreso ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE facturas ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE vehiculos ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

-- 4. Tabla: reinspeccion_seguimiento (Control estricto de los 15 días calendario de gracia para 2da revisión)
CREATE TABLE IF NOT EXISTS reinspeccion_seguimiento (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orden_rechazada_id UUID NOT NULL REFERENCES ordenes_ingreso(id) ON DELETE CASCADE UNIQUE,
    vehiculo_id UUID NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    cliente_id UUID REFERENCES clientes(id) ON DELETE RESTRICT,
    fecha_rechazo TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_limite_15_dias TIMESTAMP WITH TIME ZONE NOT NULL,
    estado_seguimiento VARCHAR(30) NOT NULL DEFAULT 'EN_PLAZO',
    reinspeccion_completada BOOLEAN NOT NULL DEFAULT FALSE,
    orden_reinspeccion_id UUID REFERENCES ordenes_ingreso(id) ON DELETE SET NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reinspeccion_vehiculo ON reinspeccion_seguimiento(vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_reinspeccion_limite ON reinspeccion_seguimiento(fecha_limite_15_dias);
CREATE INDEX IF NOT EXISTS idx_reinspeccion_estado ON reinspeccion_seguimiento(estado_seguimiento);

-- 5. Tabla: cuentas_por_pagar (Obligaciones, Facturas de Proveedores, Membresías y Responsabilidades)
CREATE TABLE IF NOT EXISTS cuentas_por_pagar (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    acreedor_tercero_id UUID NOT NULL REFERENCES terceros(id) ON DELETE RESTRICT,
    numero_referencia VARCHAR(60),
    concepto VARCHAR(250) NOT NULL,
    monto_total NUMERIC(14,2) NOT NULL,
    saldo_pendiente NUMERIC(14,2) NOT NULL,
    tipo_obligacion VARCHAR(40) NOT NULL DEFAULT 'FACTURA_PROVEEDOR',
    periodicidad VARCHAR(25) NOT NULL DEFAULT 'PAGO_UNICO',
    fecha_emision DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento DATE NOT NULL,
    dias_aviso_anticipado INTEGER NOT NULL DEFAULT 5,
    estado VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE',
    observaciones TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cuentas_pagar_acreedor ON cuentas_por_pagar(acreedor_tercero_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_pagar_vencimiento ON cuentas_por_pagar(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_cuentas_pagar_estado ON cuentas_por_pagar(estado);

-- 6. Tabla: pagos_proveedor (Comprobantes y abonos a cuentas por pagar)
CREATE TABLE IF NOT EXISTS pagos_proveedor (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cuenta_por_pagar_id UUID NOT NULL REFERENCES cuentas_por_pagar(id) ON DELETE CASCADE,
    monto_pagado NUMERIC(14,2) NOT NULL,
    fecha_pago TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo_pago VARCHAR(30) NOT NULL DEFAULT 'TRANSFERENCIA',
    numero_comprobante VARCHAR(80),
    soporte_url_archivo TEXT,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    observaciones TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pagos_prov_cuenta ON pagos_proveedor(cuenta_por_pagar_id);
CREATE INDEX IF NOT EXISTS idx_pagos_prov_fecha ON pagos_proveedor(fecha_pago);

-- 7. Seed Inicial de Proveedores y Responsabilidades Clave del CDA
INSERT INTO terceros (id, tipo_documento, numero_documento, digito_verificacion, tipo_persona, razon_social_o_nombre, celular_principal, email_principal, direccion, responsabilidad_fiscal, metadata)
VALUES 
(
    '00000000-0000-0000-0000-000000000101',
    'NIT',
    '900123456',
    '1',
    'JURIDICA',
    'ONAC - Organismo Nacional de Acreditación',
    '3105551001',
    'contacto@onac.org.co',
    'Av. Calle 26 # 69D-91, Bogotá',
    'O-23',
    '{"categoria_proveedor": "CALIBRACION_EQUIPOS", "datos_bancarios": "Bancolombia Cta Cte 102-998877-01"}'::jsonb
),
(
    '00000000-0000-0000-0000-000000000102',
    'NIT',
    '830098765',
    '4',
    'JURIDICA',
    'SICOV & RUNT Concesión Nacional',
    '3105551002',
    'facturacion@runt.com.co',
    'Calle 100 # 19-61, Bogotá',
    'O-13',
    '{"categoria_proveedor": "SOFTWARE_LICENCIAS", "datos_bancarios": "Davivienda Cta Ahorros 045-887711-22"}'::jsonb
),
(
    '00000000-0000-0000-0000-000000000103',
    'NIT',
    '901456789',
    '8',
    'JURIDICA',
    'Metrología y Calibraciones de Pista S.A.S.',
    '3124445566',
    'soporte@metrologiacda.com',
    'Carrera 15 # 45-20, Medellín',
    'R-99-PN',
    '{"categoria_proveedor": "CALIBRACION_EQUIPOS", "datos_bancarios": "BBVA Cta Cte 980-123456-78"}'::jsonb
)
ON CONFLICT (numero_documento) DO NOTHING;

INSERT INTO terceros_roles (tercero_id, tipo_rol, activo)
SELECT id, 'PROVEEDOR', TRUE FROM terceros WHERE numero_documento IN ('900123456', '830098765', '901456789')
ON CONFLICT (tercero_id, tipo_rol) DO NOTHING;

-- Seed inicial de Cuentas por Pagar de demostración
INSERT INTO cuentas_por_pagar (id, acreedor_tercero_id, numero_referencia, concepto, monto_total, saldo_pendiente, tipo_obligacion, periodicidad, fecha_emision, fecha_vencimiento, dias_aviso_anticipado, estado)
VALUES
(
    '00000000-0000-0000-0000-000000000201',
    '00000000-0000-0000-0000-000000000101',
    'ONAC-2026-08',
    'Auditoría y Mantenimiento de Acreditación ISO/IEC 17020',
    2500000.00,
    2500000.00,
    'MEMBRESIA_LICENCIA',
    'ANUAL',
    CURRENT_DATE - INTERVAL '10 days',
    CURRENT_DATE + INTERVAL '5 days',
    7,
    'PENDIENTE'
),
(
    '00000000-0000-0000-0000-000000000102',
    '00000000-0000-0000-0000-000000000102',
    'SICOV-PINS-778',
    'Paquete Mensual de Pines de Transmisión SICOV',
    1200000.00,
    1200000.00,
    'SOFTWARE_LICENCIAS',
    'MENSUAL',
    CURRENT_DATE - INTERVAL '20 days',
    CURRENT_DATE + INTERVAL '2 days',
    5,
    'PENDIENTE'
),
(
    '00000000-0000-0000-0000-000000000103',
    '00000000-0000-0000-0000-000000000103',
    'FAC-CALIB-455',
    'Calibración de Frenómetro de Rodillos y Luxómetro Pista 1',
    850000.00,
    850000.00,
    'CALIBRACION_EQUIPOS',
    'TRIMESTRAL',
    CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE - INTERVAL '2 days',
    5,
    'VENCIDA'
)
ON CONFLICT (id) DO NOTHING;
