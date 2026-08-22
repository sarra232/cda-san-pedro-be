# ⚙️ CDA San Pedro Backend (REST API)

Servicio backend de alto rendimiento para **CDA San Pedro S.A.S.** construido con **Java 21**, **Spring Boot 3.3.3** y **Clean Architecture**.

---

## 🛠️ Stack Tecnológico

- **Lenguaje**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.3
- **Persistencia**: Spring Data JPA + Hibernate 6.5
- **Base de Datos**: PostgreSQL 16
- **Versionamiento de Base de Datos**: Flyway Migrations
- **Seguridad**: Spring Security 6 + JSON Web Tokens (JWT con algoritmo HMAC-SHA512)
- **Generación de Documentos**: iText PDF (Facturas y Comprobantes RTM)
- **Utilidades**: Lombok, MapStruct / DTO Mappers, Jakarta Validation

---

## 🏛️ Estructura del Código (Clean Architecture)

```
src/main/java/com/cdasanpedro/
├── core/                              # Capa de Dominio (Pura, sin frameworks)
│   ├── model/enums/                   # Enums: RolUsuario, EstadoOrden, CategoriaVehiculo, etc.
│   └── exception/                     # Jerarquía de excepciones de negocio
├── application/                       # Capa de Aplicación (Casos de uso)
│   ├── dto/                           # Data Transfer Objects para requests y responses
│   └── usecase/                       # Servicios de lógica de negocio (Ingreso, Factura, Tarifa, etc.)
├── infrastructure/                    # Capa de Infraestructura
│   ├── bootstrap/                     # DataInitializer (Seeds de usuarios y configuración)
│   ├── persistence/
│   │   ├── entity/                    # Entidades JPA (PostgreSQL)
│   │   └── repository/                # Interfaces Spring Data JPA
│   ├── pdf/                           # Generación de comprobantes PDF
│   └── security/                      # Filtros JWT, AuthenticationProvider y PasswordEncoder
└── web/                               # Capa de Presentación (REST Controllers)
    ├── controller/                    # Endpoints REST API
    ├── dto/                           # Wrapper genérico ApiResponse<T>
    └── exception/                     # GlobalExceptionHandler (@ControllerAdvice)
```

---

## 🗄️ Migraciones de Base de Datos (Flyway)

Las migraciones se ejecutan automáticamente en el inicio de la aplicación:

| Versión | Archivo | Descripción |
| :--- | :--- | :--- |
| **V1** | `V1__init.sql` | Esquema base: `usuarios`, `clientes`, `vehiculos`, `ordenes_ingreso`, `facturas`, `items_factura`, `notificaciones`. |
| **V2** | `V2__pruebas_inspeccion.sql` | Tabla `pruebas_inspeccion` para las 4 pruebas NTC 5375 con firma del técnico responsable. |
| **V3** | `V3__tarifas.sql` | Tabla `tarifas` inicial para precios dinámicos por categoría. |
| **V4** | `V4__servicios_catalogo.sql` | Catálogo ampliado de servicios con códigos únicos (`codigo`, `tipo_servicio`) y múltiples servicios por categoría. |

---

## 📡 Referencia de la API REST

Todos los endpoints (excepto login) requieren header `Authorization: Bearer <JWT_TOKEN>`.

### 1. Autenticación (`/api/auth`)
- `POST /api/auth/login` — Autenticación con tipo/número de documento y contraseña. Retorna JWT Token y datos del usuario.

### 2. Recepción & Órdenes de Ingreso (`/api/ingresos`)
- `GET /api/ingresos` — Listar órdenes (con filtros opcionales por fecha, estado y placa).
- `GET /api/ingresos/{id}` — Consultar orden por UUID.
- `POST /api/ingresos` — Registrar nuevo ingreso vehicular y generar consecutivo de turno.
- `PATCH /api/ingresos/{id}/estado` — Cambiar estado de la orden. **Nota**: Cambiar a `APROBADO` o `RECHAZADO` requiere rol `ADMINISTRADOR` o `DIRECTOR_TECNICO`.
- `GET /api/ingresos/listos-facturar` — Listar vehículos listos para cobro en ventanilla.
- `GET /api/ingresos/buscar-placa` — Búsqueda predictiva de vehículos y propietarios por placa.

### 3. Pista de Inspección Técnica (`/api/ingresos/{id}/pruebas`)
- `GET /api/ingresos/{id}/pruebas` — Obtener las 4 pruebas reglamentarias de una orden.
- `PATCH /api/ingresos/{id}/pruebas` — Registrar resultados, mediciones y observaciones de una prueba técnica.

### 4. Catálogo de Servicios & Tarifas (`/api/tarifas`)
- `GET /api/tarifas` — Listar catálogo de servicios (con filtros por categoría y estado).
- `GET /api/tarifas/{id}` — Obtener detalle de un servicio.
- `POST /api/tarifas` — Crear nuevo servicio *(Exclusivo Administrador)*.
- `PUT /api/tarifas/{id}` — Modificar precio, nombre o descripción *(Exclusivo Administrador)*.
- `DELETE /api/tarifas/{id}` — Eliminar servicio del catálogo *(Exclusivo Administrador)*.

### 5. Facturación & Caja (`/api/facturas`)
- `GET /api/facturas` — Listar facturas emitidas con filtros por rango de fechas.
- `GET /api/facturas/{id}` — Consultar factura por ID.
- `POST /api/facturas` — Emitir factura y liquidar financieramente con tarifa dinámica.
- `GET /api/facturas/{id}/pdf` — Descargar comprobante oficial en PDF.
- `POST /api/facturas/{id}/enviar` — Enviar comprobante al WhatsApp del pagador.

### 6. Personal & Usuarios (`/api/usuarios`)
- `GET /api/usuarios` — Listar empleados del CDA.
- `POST /api/usuarios` — Registrar nuevo usuario *(Exclusivo Administrador)*.
- `PUT /api/usuarios/{id}` — Modificar datos o rol *(Exclusivo Administrador)*.
- `PATCH /api/usuarios/{id}/estado` — Activar o inactivar usuario *(Exclusivo Administrador)*.

---

## 🔒 Segregación de Roles en Backend

```
                 [ ADMINISTRADOR ] ─────────> Control Total + Tarifas + Personal
                         │
                         ├───> [ DIRECTOR_TECNICO ] ──> Dictamen Final RTM (Aprobar/Rechazar)
                         │
                         ├───> [ TECNICO_PISTA ] ─────> Registro de 4 Pruebas NTC 5375
                         │
                         └───> [ RECEPCIONISTA ] ─────> Turnos, Clientes y Facturación
```

---

## 💻 Desarrollo Local

### Compilar y Ejecutar con Maven
```bash
# Compilar paquete
mvn clean package -DskipTests

# Ejecutar pruebas unitarias
mvn test

# Iniciar servidor localmente (requiere PostgreSQL en localhost:5432)
mvn spring-boot:run
```

### Variables de Entorno Principales
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cda_db
SPRING_DATASOURCE_USERNAME=cda_user
SPRING_DATASOURCE_PASSWORD=cda_password_2026
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```
