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