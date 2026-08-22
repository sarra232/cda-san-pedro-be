package com.cdasanpedro.core.model.enums;

public enum TipoDocumento {
    CC("Cédula de Ciudadanía"),
    CE("Cédula de Extranjería"),
    NIT("Número de Identificación Tributaria"),
    TI("Tarjeta de Identidad"),
    PASAPORTE("Pasaporte"),
    PPT("Permiso por Protección Temporal"),
    PEP("Permiso Especial de Permanencia"),
    RC("Registro Civil");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
