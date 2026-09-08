-- Migración V7: Homologación exacta con el catálogo de producción real de SIIGO Cloud API
-- CDA San Pedro S.A.S.

ALTER TABLE mapeo_catalogo_siigo ADD COLUMN IF NOT EXISTS siigo_tax_id INTEGER;

-- Actualización de categorías con los códigos reales de SIIGO
UPDATE mapeo_catalogo_siigo SET codigo_producto_siigo = '005', descripcion_siigo = 'REVISION TECNO MOTOS', siigo_tax_id = 20358, tarifa_iva = 19.00 WHERE categoria_cda = 'MOTO';
UPDATE mapeo_catalogo_siigo SET codigo_producto_siigo = '002', descripcion_siigo = 'REVISION TECNO AUTO LIVIANO', siigo_tax_id = 18668, tarifa_iva = 19.00 WHERE categoria_cda = 'LIVIANO';
UPDATE mapeo_catalogo_siigo SET codigo_producto_siigo = '001', descripcion_siigo = 'REVISION TECNO PESADOS', siigo_tax_id = 18668, tarifa_iva = 19.00 WHERE categoria_cda = 'PESADO';
UPDATE mapeo_catalogo_siigo SET codigo_producto_siigo = '002', descripcion_siigo = 'REVISION TECNO AUTO LIVIANO', siigo_tax_id = 18668, tarifa_iva = 19.00 WHERE categoria_cda = 'PUBLICO';
UPDATE mapeo_catalogo_siigo SET codigo_producto_siigo = '014', descripcion_siigo = 'REVISIONES PREVENTIVAS', siigo_tax_id = 18668, tarifa_iva = 19.00 WHERE categoria_cda = 'PREVENTIVA';
UPDATE mapeo_catalogo_siigo SET codigo_producto_siigo = '0015', descripcion_siigo = 'ANTICIPO PAGO REVISION TECNICOMECANICA', siigo_tax_id = NULL, tarifa_iva = 0.00 WHERE categoria_cda = 'REINSPECCION_GRATUITA';

-- Insertar subcategorías adicionales y conceptos de terceros en SIIGO
INSERT INTO mapeo_catalogo_siigo (categoria_cda, codigo_producto_siigo, descripcion_siigo, tarifa_iva, siigo_tax_id)
VALUES 
    ('MOTOCARRO', '003', 'REVISION TECNO MOTOCARRO', 19.00, 18668),
    ('CUATRIMOTO', '004', 'REVISION TECNO CUATRIMOTO', 19.00, 18668),
    ('RUNT', '006', 'DERECHOS DE RUNT', 0.00, NULL),
    ('SICOV', '007', 'INGRESO SICOV', 0.00, NULL),
    ('IVA_SICOV', '008', 'IVA SICOV', 0.00, NULL),
    ('ANSV', '009', 'SEGURIDAD VIAL', 0.00, NULL),
    ('BANCARIZACION', '010', 'BANCARIZACION', 0.00, NULL)
ON CONFLICT (categoria_cda) DO UPDATE 
SET codigo_producto_siigo = EXCLUDED.codigo_producto_siigo,
    descripcion_siigo = EXCLUDED.descripcion_siigo,
    tarifa_iva = EXCLUDED.tarifa_iva,
    siigo_tax_id = EXCLUDED.siigo_tax_id,
    activo = TRUE;
