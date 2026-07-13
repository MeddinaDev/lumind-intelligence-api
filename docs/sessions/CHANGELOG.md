# Development Log

## 2026-07-06

### Sprint
Sprint 1 - Project Setup

### Objetivo
Completar la base del proyecto para comenzar el desarrollo de funcionalidades.

### Cambios realizados
- Inicializado el proyecto con Spring Boot 3.5 y Java 21.
- Configurada la arquitectura Feature-Based.
- Configurado Spring Security (HTTP Basic temporal).
- Configurado PostgreSQL y Flyway.
- Configurado Swagger/OpenAPI.
- Creado AGENTS.md.
- Creado README provisional.
- Definida la política de idioma.
- Documentación inicial completada (Roadmap, Domain Model, ADRs, etc.).

### Decisiones tomadas
- Arquitectura Feature-Based.
- Documentación interna en español.
- Código en inglés.
- README definitivo en Sprint 9.
- AGENTS.md como fuente principal de contexto del proyecto.

### Estado del proyecto
✅ Sprint 1 completado.

### Próximo paso
Sprint 2 - Authentication.

---

## 2026-07-06 (continuación)

### Sprint
Sprint 2 - Authentication

### Objetivo
Diseñar completamente la autenticación antes de implementar código.

### Cambios realizados
- Creada especificación técnica en `docs/spec/authentication/SPECIFICATION.md`.
- Actualizado `AGENTS.md` con regla del Development Log.

### Decisiones tomadas
- Registro con auto-login (devuelve access token y refresh token).
- Refresh tokens almacenados hasheados (SHA-256) en base de datos.
- Librería JWT: JJWT.
- Política de contraseñas: 8–128 caracteres en Sprint 2.
- Entidad `User` mínima para autenticación; perfil de usuario en sprints futuros.

### Estado del proyecto
🔄 Sprint 2 en curso — especificación aprobada, implementación pendiente.

### Próximo paso
Implementar autenticación según plan de Sprint 2.

---

## 2026-07-06 — Fase 0

### Sprint
Sprint 2 - Authentication

### Objetivo
Preparar la infraestructura de documentación, dependencias y configuración JWT antes de implementar lógica de autenticación.

### Cambios realizados
- Completados ADR `003-security-strategy.md` y `004-jwt.md`.
- Añadidas dependencias JJWT 0.13.0 en `pom.xml`.
- Configuradas propiedades JWT en `application.yml`.
- Documentada variable `JWT_SECRET` en `.env.example` y `README.md`.

### Decisiones tomadas
- Estrategia de seguridad registrada en ADR 003 (JWT Bearer, BCrypt, rutas públicas, CSRF off, anti-enumeración).
- Detalle JWT registrado en ADR 004 (JJWT, HS256, TTL access 15 min / refresh 7 días, hash SHA-256 en BD).

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 0 completada; pendiente Fase 1 (capa común de excepciones).

### Próximo paso
Fase 1 — `ErrorResponse`, excepciones de dominio y `GlobalExceptionHandler`.

---

## 2026-07-06 — Fase 1

### Sprint
Sprint 2 - Authentication

### Objetivo
Implementar la capa común de manejo de errores antes de persistencia y lógica de autenticación.

### Cambios realizados
- Creado `ErrorResponse` y `FieldError` en `common.exception`.
- Creadas excepciones de dominio: `EmailAlreadyExistsException`, `InvalidCredentialsException`, `AccountDisabledException`, `InvalidRefreshTokenException`.
- Implementado `GlobalExceptionHandler` con `@RestControllerAdvice` y formato de error unificado.
- Handlers de infraestructura: validación Bean Validation, JSON malformado y errores no controlados (`500`).

### Decisiones tomadas
- Mensajes de error fijos como constantes privadas en `GlobalExceptionHandler` (no clase `ErrorMessages` separada).
- Excepciones marker sin mensaje; el handler controla el texto expuesto al cliente.
- `JwtException` diferido a Fase 6 (filtro JWT fuera del ciclo MVC).
- Tests del handler diferidos a Fase 8.

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 1 completada; pendiente Fase 2 (persistencia `User` y migraciones Flyway).

### Próximo paso
Fase 2 — entidad `User`, `UserRepository` y migración Flyway `V1__create_users_table.sql`.

---

## 2026-07-06 — Fase 2

### Sprint
Sprint 2 - Authentication

### Objetivo
Implementar la capa de persistencia de usuarios: migraciones Flyway, entidad JPA, repositorio, DTO de respuesta y mapper MapStruct.

### Cambios realizados
- Creada migración Flyway `V1__create_users_table.sql` (tabla `users`).
- Creada migración Flyway `V2__create_refresh_tokens_table.sql` (tabla `refresh_tokens`; esquema listo para Fase 4).
- Creada entidad JPA `User` en `user.entity` con UUID generado en aplicación y timestamps automáticos.
- Creado `UserRepository` con `existsByEmail` y `findByEmail`.
- Creado `UserSummaryResponse` (record) en `user.dto.response`.
- Creado `UserMapper` con MapStruct (`toSummaryResponse`).

### Decisiones tomadas
- UUID asignado en `@PrePersist` (D-05); sin `@GeneratedValue` ni DEFAULT en BD.
- Sin relación JPA `User` ↔ `refresh_tokens` hasta Fase 4 (`RefreshToken` en `auth/`).
- FK `refresh_tokens.user_id` con `ON DELETE CASCADE` a nivel SQL.
- `@PrePersist` / `@PreUpdate` para `created_at` y `updated_at` (sin service en esta fase).

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 2 completada; pendiente Fase 3 (infraestructura JWT: `JwtProperties`, `JwtService`).

### Próximo paso
Fase 3 — `JwtProperties` y `JwtService`.

---

## 2026-07-09 — Fase 3

### Sprint
Sprint 2 - Authentication

### Objetivo
Implementar la infraestructura criptográfica de autenticación: configuración JWT tipada, servicio de emisión/validación de tokens, codificador de contraseñas BCrypt y utilidad SHA-256 para refresh tokens.

### Cambios realizados
- Creado `JwtProperties` (`@ConfigurationProperties`) con validación de secreto Base64 (≥ 256 bits decodificados).
- Creado `JwtConfig` para registrar `JwtProperties`.
- Creado `JwtService` en `auth/`: generación de access/refresh tokens (HS256, JJWT 0.13), validación diferenciada por claim `type`, extracción de `sub`.
- Secreto JWT decodificado con `Decoders.BASE64.decode` antes de construir `SecretKey`.
- Creado `PasswordEncoderConfig` con bean `BCryptPasswordEncoder` (strength 10).
- Creado `Sha256Hasher` en `common/util/` para hash SHA-256 hex (64 caracteres) de refresh tokens.

### Decisiones tomadas
- `JWT_SECRET` almacenado como Base64 (`openssl rand -base64 32`); decodificación explícita en `JwtProperties` y `JwtService`.
- Constantes privadas en `JwtService` para claims (`email`, `type`, `refresh`); sin magic strings.
- `parseAndValidateRefreshToken()` incluido en Fase 3 para dejar `JwtService` completo antes de Fase 4–5.
- `PasswordEncoderConfig` separado de `SecurityConfig` (sin modificar seguridad HTTP Basic).

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 3 completada; pendiente Fase 4 (`RefreshToken`, repositorio, rotación).

### Próximo paso
Fase 4 — entidad `RefreshToken`, `RefreshTokenRepository` y lógica de rotación.

---

## 2026-07-09 — Fase 4

### Sprint
Sprint 2 - Authentication

### Objetivo
Implementar persistencia y ciclo de vida de refresh tokens: entidad JPA, repositorio, emisión hasheada y rotación transaccional.

### Cambios realizados
- Creada entidad `RefreshToken` en `auth/entity/` con relación `@ManyToOne` unidireccional a `User`.
- Creado `RefreshTokenRepository` con `findByToken` (búsqueda por hash SHA-256).
- Creado record `IssuedTokens` (`accessToken`, `refreshToken`) en `auth/model/`.
- Creado `RefreshTokenService` con `issueTokens()` y `rotate()`; hash SHA-256 del JWT completo antes de persistir.
- Rotación transaccional (`@Transactional` en `rotate()`): revocación del token anterior y persistencia del nuevo en la misma transacción (rollback si falla el guardado del nuevo token).
- Detección de reutilización de token revocado con log `WARN` y `user_id`.
- Verificación de `user.enabled` en `rotate()`; cuenta deshabilitada lanza `AccountDisabledException`.

### Decisiones tomadas
- `IssuedTokens` sin entidad `User`; el caller (Fase 5 `AuthService`) resuelve el usuario si lo necesita para el DTO de respuesta.
- `expires_at` en BD alineado con el claim `exp` del JWT al persistir.
- Sin actualización de AGENTS.md, README ni SPEC en esta fase (cierre documental al finalizar Sprint 2).

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 4 completada; pendiente Fase 5 (`AuthService`).

### Próximo paso
Fase 5 — `AuthService` (registro, login, refresh).

---

## 2026-07-13 — Fase 5

### Sprint
Sprint 2 - Authentication

### Objetivo
Implementar la lógica de negocio de autenticación: registro con auto-login y login, delegando la emisión de tokens a `RefreshTokenService`.

### Cambios realizados
- Creados DTOs `RegisterRequest`, `LoginRequest` y `AuthResponse` en `auth/dto/`.
- Creado `AuthMapper` (MapStruct) con mapeo `RegisterRequest` → `User` y `IssuedTokens` + `User` → `AuthResponse`.
- Creado `AuthService` con `register()` (`@Transactional`) y `login()`.
- Registro: unicidad de email, hash BCrypt, persistencia de usuario y emisión de tokens vía `RefreshTokenService.issueTokens()`.
- Login: anti-enumeración (`InvalidCredentialsException` genérica), verificación de cuenta activa tras credenciales válidas, emisión de tokens.
- Logging seguro: `INFO` en registro/login exitoso (`userId`); `WARN` en credenciales inválidas; sin passwords ni tokens.

### Decisiones tomadas
- Fase 5 limitada a `register()` y `login()`; `refresh()` diferido a fase posterior.
- Sin normalización de email ni handler de `DataIntegrityViolationException` en esta fase.
- Emisión de tokens delegada completamente a `RefreshTokenService`; `JwtService` usado solo para `expiresIn`.
- Sin controllers, filtros, tests ni cambios en `SecurityConfig`.

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 5 completada; pendiente Fase 6 (filtro JWT y `SecurityConfig`).

### Próximo paso
Fase 6 — `JwtAuthenticationFilter` y actualización de `SecurityConfig`.

---

## 2026-07-13 — Fase 7

### Sprint
Sprint 2 - Authentication

### Objetivo
Exponer la autenticación por HTTP: `AuthController` con endpoints register, login y refresh, documentación OpenAPI con Bearer JWT y validación Bean Validation.

### Cambios realizados
- Creado `AuthController` en `auth/` con `POST /api/v1/auth/register`, `/login` y `/refresh`.
- Creado `RefreshTokenRequest` con validación `@NotBlank` y anotaciones `@Schema`.
- Implementado `AuthService.refresh()` delegando en `RefreshTokenService.rotate()` y reutilizando `AuthMapper`.
- Actualizado `OpenApiConfig` con esquema `bearerAuth` (HTTP Bearer JWT) y `SecurityRequirement` global.
- Documentados con `@Schema` los DTOs `RegisterRequest`, `LoginRequest`, `RefreshTokenRequest`, `AuthResponse` y `UserSummaryResponse`.
- Endpoints públicos de auth marcados con `@SecurityRequirements()` para Swagger.

### Decisiones tomadas
- `AuthService.refresh()` incluido en Fase 7 como cierre del gap diferido en Fase 5.
- `AccountDisabledException` en refresh mantiene HTTP `403` (sin modificar `RefreshTokenService`).
- Respuestas HTTP mediante `ResponseEntity<AuthResponse>`: `201` (register), `200` (login, refresh).
- Sin tests, sin cambios en `JwtService`, `RefreshTokenService`, `SecurityConfig` ni entidades.
- Sin actualización de AGENTS.md, README.md ni SPECIFICATION.md en esta fase.

### Estado del proyecto
🔄 Sprint 2 en curso — Fase 7 completada; pendiente Fase 8 (tests).

### Próximo paso
Fase 8 — tests unitarios de `AuthService` e integración de endpoints auth (MockMvc).

---

## 2026-07-13 — Fase 8

### Sprint
Sprint 2 - Authentication

### Objetivo
Implementar la batería inicial de tests del módulo de autenticación para cerrar Sprint 2 con cobertura alta de las reglas de negocio.

### Cambios realizados
- Añadida dependencia H2 (test scope) y plugin JaCoCo 0.8.14 en `pom.xml`.
- Creado perfil `application-test.yml` con H2 en memoria, Flyway deshabilitado y JWT de test.
- Creada utilidad compartida `AuthTestData` para datos de prueba reutilizables.
- Tests unitarios: `AuthServiceTest` (11), `JwtServiceTest` (11), `RefreshTokenServiceTest` (9).
- Tests de integración MockMvc: `AuthControllerIntegrationTest` (9) sobre `POST /register`, `/login` y `/refresh`.
- Cobertura JaCoCo del paquete `com.lumind.api.auth` (servicios + controller): 100 % instrucciones.

### Decisiones tomadas
- Integración con `@SpringBootTest` + H2 en memoria; sin Testcontainers ni cambios en producción.
- `extractTokenId()` no existe en `JwtService`; cobertura del `jti` verificada vía claim `id` en refresh tokens.
- Sin tests de `JwtAuthenticationFilter` ni `SecurityConfig` (fuera de alcance de Fase 8).
- Sin cambios en lógica de negocio, entidades, DTOs, AGENTS, README ni SPEC.

### Estado del proyecto
✅ Sprint 2 completado — módulo auth con tests y cobertura de reglas de negocio.

### Próximo paso
Sprint 3 — siguiente feature según roadmap (usuarios / hábitos).

---

## 2026-07-13 — Fase 9

### Sprint
Sprint 3 - Habits

### Objetivo
Implementar el modelo de dominio Habit (entidad, persistencia, DTOs y mapper) sin capa de servicio ni API.

### Cambios realizados
- Creada entidad `Habit` en `habit/entity/` con relación `@ManyToOne` a `User`, UUID, `createdAt` y `updatedAt`.
- Creado `HabitRepository` (`JpaRepository<Habit, UUID>`).
- Creada migración Flyway `V3__create_habits_table.sql` con FK a `users` y `ON DELETE CASCADE`.
- Creados DTOs `CreateHabitRequest`, `UpdateHabitRequest` y `HabitResponse` con Bean Validation.
- Creado `HabitMapper` (MapStruct): `toEntity`, `toResponse` y `updateEntity` con estrategia `IGNORE` para campos nulos.

### Decisiones tomadas
- Modelo mínimo: `name` (obligatorio, 100 chars) y `description` (opcional, 500 chars); sin streaks, estadísticas ni recordatorios.
- `user_id` inmutable en entidad (`updatable = false`); asignación de usuario diferida a `HabitService`.
- Sin `@Schema` en DTOs (OpenAPI fuera de alcance de esta fase).
- Sin `HabitService`, `HabitController`, tests ni cambios de seguridad.

### Estado del proyecto
🔄 Sprint 3 en curso — Fase 9 (dominio Habit) completada; pendiente capa de servicio y API.

### Próximo paso
Fase 10 — `HabitService` y reglas de negocio CRUD.