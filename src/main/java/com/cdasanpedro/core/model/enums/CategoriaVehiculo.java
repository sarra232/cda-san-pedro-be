package com.cdasanpedro.core.model.enums;

public enum CategoriaVehiculo {
    MOTO("Motocicletas y similares"),
    LIVIANO("Automóviles, camionetas y camperos"),
    PESADO("Camiones, furgones y tractocamiones"),
    PUBLICO("Taxis, microbuses y transporte especial");

    private final String descripcion;

    CategoriaVehiculo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
