-- Migración Inicial de Base de Datos - CDA San Pedro
-- Version: V1
-- PostgreSQL 16 compatible

-- Habilitar extensión para generación de UUIDs si no está activa
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tabla: usuarios (Personal con acceso al sistema)
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_documento VARCHAR(15) NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(25) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombres_apellidos VARCHAR(150) NOT NULL,
    rol VARCHAR(25) NOT NULL DEFAULT 'RECEPCIONISTA',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuarios_documento ON usuarios(numero_documento);
CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios(rol);

-- 2. Tabla: clientes (Propietarios, conductores y pagadores)
CREATE TABLE IF NOT EXISTS clientes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_documento VARCHAR(15) NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(25) NOT NULL UNIQUE,
    nombres_razon_social VARCHAR(150) NOT NULL,
    direccion VARCHAR(200),
    celular VARCHAR(20) NOT NULL,
    email VARCHAR(120),
    fecha_nacimiento DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clientes_documento ON clientes(numero_documento);
CREATE INDEX IF NOT EXISTS idx_clientes_celular ON clientes(celular);

-- 3. Tabla: vehiculos (Parque automotor y propietario oficial)
CREATE TABLE IF NOT EXISTS vehiculos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    placa VARCHAR(10) NOT NULL UNIQUE,
    categoria VARCHAR(20) NOT NULL DEFAULT 'LIVIANO',
    marca VARCHAR(60) NOT NULL,
    linea VARCHAR(60) NOT NULL,
    modelo INTEGER NOT NULL,
    chasis_vin VARCHAR(50),
    fecha_vencimiento_soat DATE,
    fecha_vencimiento_rtm DATE,
    propietario_id UUID REFERENCES clientes(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehiculos_placa ON vehiculos(placa);
CREATE INDEX IF NOT EXISTS idx_vehiculos_soat ON vehiculos(fecha_vencimiento_soat);
CREATE INDEX IF NOT EXISTS idx_vehiculos_rtm ON vehiculos(fecha_vencimiento_rtm);
CREATE INDEX IF NOT EXISTS idx_vehiculos_categoria ON vehiculos(categoria);

-- 4. Tabla: ordenes_ingreso (Recepción del vehículo en ventanilla)
CREATE TABLE IF NOT EXISTS ordenes_ingreso (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    consecutivo BIGSERIAL NOT NULL UNIQUE,
    fecha_ingreso TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kilometraje INTEGER NOT NULL,
    tipo_servicio VARCHAR(50) NOT NULL DEFAULT 'RTM_LEGAL',
    estado VARCHAR(25) NOT NULL DEFAULT 'INGRESADO',
    conductor_es_propietario BOOLEAN NOT NULL DEFAULT TRUE,
    conductor_id UUID REFERENCES clientes(id) ON DELETE RESTRICT,
    vehiculo_id UUID NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    observaciones TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ingresos_fecha ON ordenes_ingreso(fecha_ingreso);
CREATE INDEX IF NOT EXISTS idx_ingresos_estado ON ordenes_ingreso(estado);
CREATE INDEX IF NOT EXISTS idx_ingresos_vehiculo ON ordenes_ingreso(vehiculo_id);

-- 5. Tabla: facturas (Liquidación y cobro inmutable)
CREATE TABLE IF NOT EXISTS facturas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero_factura VARCHAR(30) NOT NULL UNIQUE,
    consecutivo BIGSERIAL NOT NULL UNIQUE,
    fecha_emision TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(14,2) NOT NULL,
    iva NUMERIC(14,2) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    metodo_pago VARCHAR(25) NOT NULL DEFAULT 'EFECTIVO',
    estado VARCHAR(20) NOT NULL DEFAULT 'PAGADA',
    orden_ingreso_id UUID NOT NULL REFERENCES ordenes_ingreso(id) ON DELETE RESTRICT,
    cliente_factura_id UUID NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facturas_fecha ON facturas(fecha_emision);
CREATE INDEX IF NOT EXISTS idx_facturas_cliente ON facturas(cliente_factura_id);
CREATE INDEX IF NOT EXISTS idx_facturas_orden ON facturas(orden_ingreso_id);

-- 6. Tabla: items_factura (Desglose de cobro)
CREATE TABLE IF NOT EXISTS items_factura (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    factura_id UUID NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
    descripcion VARCHAR(200) NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    valor_unitario NUMERIC(14,2) NOT NULL,
    total_item NUMERIC(14,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_factura_id ON items_factura(factura_id);

-- 7. Tabla: notificaciones_cola (Control de envíos asíncronos)
CREATE TABLE IF NOT EXISTS notificaciones_cola (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cliente_id UUID REFERENCES clientes(id) ON DELETE SET NULL,
    tipo VARCHAR(30) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    destinatario VARCHAR(120) NOT NULL,
    asunto VARCHAR(200),
    cuerpo_payload JSONB NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER NOT NULL DEFAULT 0,
    fecha_programada TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_enviado TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_estado ON notificaciones_cola(estado, fecha_programada);

-- Seed inicial de usuario Administrador (Password: Admin123*)
-- Hash BCrypt: $2a$10$Qj/tZ0B9a2M43x5r2d45b.fA2zGzU5Gz8sZ.5s2c8s2y6A3.8c7b. (Ejemplo hash estándar)
INSERT INTO usuarios (id, tipo_documento, numero_documento, password_hash, nombres_apellidos, rol, activo)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'CC',
    '123456789',
    '$2a$10$w8B7T0YqT9o8xNmv5u8XNu4dK8n0vN5eL4N0vN5eL4N0vN5eL4N0v',
    'Administrador Principal CDA',
    'ADMINISTRADOR',
    TRUE
) ON CONFLICT (numero_documento) DO NOTHING;
