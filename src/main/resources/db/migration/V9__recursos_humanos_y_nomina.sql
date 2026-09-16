-- ==============================================================================
-- MIGRACIÓN V9: Módulo de Recursos Humanos (RRHH), Gestión de Inspectores ONAC y Motor de Nómina
-- CDA San Pedro S.A.S.
-- ==============================================================================

-- 1. Tabla de Empleados (vinculada con la entidad maestra Terceros)
CREATE TABLE IF NOT EXISTS empleados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tercero_id UUID NOT NULL REFERENCES terceros(id) ON DELETE RESTRICT,
    cargo VARCHAR(100) NOT NULL,
    departamento VARCHAR(100) NOT NULL DEFAULT 'OPERACIONES_PISTA',
    tipo_contrato VARCHAR(50) NOT NULL DEFAULT 'TERMINO_INDEFINIDO',
    salario_base NUMERIC(14, 2) NOT NULL DEFAULT 1600000.00 CHECK (salario_base >= 0),
    auxilio_transporte_aplica BOOLEAN NOT NULL DEFAULT TRUE,
    banco VARCHAR(100),
    tipo_cuenta VARCHAR(30) DEFAULT 'AHORROS',
    numero_cuenta VARCHAR(60),
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_retiro DATE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_empleado_tercero UNIQUE (tercero_id)
);

CREATE INDEX IF NOT EXISTS idx_empleados_cargo ON empleados(cargo);
CREATE INDEX IF NOT EXISTS idx_empleados_estado ON empleados(estado);
CREATE INDEX IF NOT EXISTS idx_empleados_tercero ON empleados(tercero_id);

-- 2. Tabla de Certificaciones Técnicas de Empleados (Control Acreditación ONAC / ISO 17020 / MinTransporte)
CREATE TABLE IF NOT EXISTS certificaciones_empleado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empleado_id UUID NOT NULL REFERENCES empleados(id) ON DELETE CASCADE,
    tipo_certificacion VARCHAR(100) NOT NULL,
    codigo_certificado VARCHAR(100) NOT NULL,
    entidad_emisora VARCHAR(150) NOT NULL DEFAULT 'SENA / Organismo Acreditador',
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    soporte_url TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    observaciones TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_certificaciones_empleado ON certificaciones_empleado(empleado_id);
CREATE INDEX IF NOT EXISTS idx_certificaciones_vencimiento ON certificaciones_empleado(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_certificaciones_estado ON certificaciones_empleado(estado);

-- 3. Tabla Maestra de Nóminas (Periodos Quincenales / Mensuales)
CREATE TABLE IF NOT EXISTS nominas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    periodo_anio INT NOT NULL,
    periodo_mes INT NOT NULL,
    periodo_quincena INT NOT NULL DEFAULT 2,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    total_devengado NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_deducciones NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_neto NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_aportes_patronales NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_provisiones NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    cuenta_por_pagar_id UUID REFERENCES cuentas_por_pagar(id) ON DELETE SET NULL,
    observaciones TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_nomina_periodo UNIQUE (periodo_anio, periodo_mes, periodo_quincena)
);

CREATE INDEX IF NOT EXISTS idx_nominas_periodo ON nominas(periodo_anio, periodo_mes, periodo_quincena);
CREATE INDEX IF NOT EXISTS idx_nominas_estado ON nominas(estado);

-- 4. Tabla de Detalles de Nómina por Empleado
CREATE TABLE IF NOT EXISTS nomina_detalles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nomina_id UUID NOT NULL REFERENCES nominas(id) ON DELETE CASCADE,
    empleado_id UUID NOT NULL REFERENCES empleados(id) ON DELETE RESTRICT,
    dias_trabajados INT NOT NULL DEFAULT 15,
    salario_base NUMERIC(14, 2) NOT NULL,
    sueldo_devengado NUMERIC(14, 2) NOT NULL,
    auxilio_transporte NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    horas_extras NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    bonificaciones NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_devengado NUMERIC(14, 2) NOT NULL,
    deduccion_salud NUMERIC(14, 2) NOT NULL,
    deduccion_pension NUMERIC(14, 2) NOT NULL,
    otras_deducciones NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_deducciones NUMERIC(14, 2) NOT NULL,
    neto_pagar NUMERIC(14, 2) NOT NULL,
    aporte_salud_patronal NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    aporte_pension_patronal NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    aporte_arl NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    parafiscales_caja NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    provision_prima NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    provision_cesantias NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    provision_intereses_cesantias NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    provision_vacaciones NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_nomina_empleado UNIQUE (nomina_id, empleado_id)
);

CREATE INDEX IF NOT EXISTS idx_nomina_detalles_nomina ON nomina_detalles(nomina_id);
CREATE INDEX IF NOT EXISTS idx_nomina_detalles_empleado ON nomina_detalles(empleado_id);

-- 5. Semilla Inicial de Empleados del CDA (Director Técnico, Inspectores de Pista y Cajera)
INSERT INTO terceros (id, tipo_documento, numero_documento, digito_verificacion, tipo_persona, razon_social_o_nombre, primer_nombre, primer_apellido, celular_principal, email_principal, direccion, responsabilidad_fiscal, metadata)
VALUES
(
    '00000000-0000-0000-0000-000000000301',
    'CC',
    '1037654321',
    '0',
    'NATURAL',
    'Carlos Mario Henao Restrepo',
    'Carlos',
    'Henao',
    '3104567890',
    'carlos.henao@cdasanpedro.com',
    'Calle 49 # 50-12, San Pedro de los Milagros',
    'R-99-PN',
    '{"cargo_sugerido": "DIRECTOR_TECNICO"}'::jsonb
),
(
    '00000000-0000-0000-0000-000000000302',
    'CC',
    '1020304050',
    '0',
    'NATURAL',
    'Andrés Felipe Gómez Zapata',
    'Andrés',
    'Gómez',
    '3128899001',
    'andres.gomez@cdasanpedro.com',
    'Carrera 48 # 47-30, San Pedro de los Milagros',
    'R-99-PN',
    '{"cargo_sugerido": "INSPECTOR_LINEA_LIVIANOS"}'::jsonb
),
(
    '00000000-0000-0000-0000-000000000303',
    'CC',
    '43890123',
    '0',
    'NATURAL',
    'Mariana Vélez Londoño',
    'Mariana',
    'Vélez',
    '3157778899',
    'mariana.velez@cdasanpedro.com',
    'Calle 50 # 49-15, San Pedro de los Milagros',
    'R-99-PN',
    '{"cargo_sugerido": "CAJERO_RECEPCIONISTA"}'::jsonb
)
ON CONFLICT (numero_documento) DO UPDATE SET
    razon_social_o_nombre = EXCLUDED.razon_social_o_nombre,
    celular_principal = EXCLUDED.celular_principal,
    email_principal = EXCLUDED.email_principal;

INSERT INTO terceros_roles (tercero_id, tipo_rol, activo)
SELECT id, 'EMPLEADO', TRUE FROM terceros WHERE numero_documento IN ('1037654321', '1020304050', '43890123')
ON CONFLICT (tercero_id, tipo_rol) DO NOTHING;

-- Insertar Empleados usando el ID real del tercero
INSERT INTO empleados (tercero_id, cargo, departamento, tipo_contrato, salario_base, auxilio_transporte_aplica, banco, tipo_cuenta, numero_cuenta, fecha_ingreso, estado)
SELECT id, 'DIRECTOR_TECNICO', 'DIRECCION_TECNICA', 'TERMINO_INDEFINIDO', 3800000.00, FALSE, 'Bancolombia', 'AHORROS', '102-334455-66', '2024-01-15'::date, 'ACTIVO'
FROM terceros WHERE numero_documento = '1037654321'
ON CONFLICT (tercero_id) DO NOTHING;

INSERT INTO empleados (tercero_id, cargo, departamento, tipo_contrato, salario_base, auxilio_transporte_aplica, banco, tipo_cuenta, numero_cuenta, fecha_ingreso, estado)
SELECT id, 'INSPECTOR_LINEA_LIVIANOS', 'OPERACIONES_PISTA', 'TERMINO_INDEFINIDO', 1850000.00, TRUE, 'Bancolombia', 'AHORROS', '102-778899-00', '2024-03-01'::date, 'ACTIVO'
FROM terceros WHERE numero_documento = '1020304050'
ON CONFLICT (tercero_id) DO NOTHING;

INSERT INTO empleados (tercero_id, cargo, departamento, tipo_contrato, salario_base, auxilio_transporte_aplica, banco, tipo_cuenta, numero_cuenta, fecha_ingreso, estado)
SELECT id, 'CAJERO_RECEPCIONISTA', 'ADMINISTRACION', 'TERMINO_INDEFINIDO', 1600000.00, TRUE, 'Davivienda', 'AHORROS', '045-112233-44', '2024-02-10'::date, 'ACTIVO'
FROM terceros WHERE numero_documento = '43890123'
ON CONFLICT (tercero_id) DO NOTHING;

-- Semilla de Certificaciones Técnicas ONAC de Inspectores
INSERT INTO certificaciones_empleado (empleado_id, tipo_certificacion, codigo_certificado, entidad_emisora, fecha_emision, fecha_vencimiento, estado, observaciones)
SELECT e.id, 'DIRECTOR_TECNICO', 'CERT-DIR-2025-99', 'Organismo Nacional de Acreditación ONAC', '2025-01-10'::date, '2027-01-10'::date, 'VIGENTE', 'Director Técnico Titular de Línea Mixta'
FROM empleados e
JOIN terceros t ON e.tercero_id = t.id
WHERE t.numero_documento = '1037654321'
ON CONFLICT DO NOTHING;

INSERT INTO certificaciones_empleado (empleado_id, tipo_certificacion, codigo_certificado, entidad_emisora, fecha_emision, fecha_vencimiento, estado, observaciones)
SELECT e.id, 'INSPECTOR_LINEA_LIVIANOS', 'CERT-SENA-RTM-458', 'SENA Centro de Tecnologías del Transporte', '2025-06-15'::date, '2026-11-30'::date, 'VIGENTE', 'Inspector Certificado en Pista Livianos y Gases'
FROM empleados e
JOIN terceros t ON e.tercero_id = t.id
WHERE t.numero_documento = '1020304050'
ON CONFLICT DO NOTHING;

