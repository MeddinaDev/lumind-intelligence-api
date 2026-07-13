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