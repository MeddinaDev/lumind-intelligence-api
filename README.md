# Lumind Intelligence API

> Backend REST desarrollado con Spring Boot 3.5 para la plataforma de productividad Lumind.

> Arquitectura modular basada en features, autenticación JWT, migraciones Flyway, documentación OpenAPI y **154 tests automatizados** con ~91 % de cobertura JaCoCo.

## Características

- JWT Authentication (register, login, refresh)
- Refresh token rotation (SHA-256 en BD)
- Habit, Task y Pomodoro CRUD
- Productivity Statistics (read model)
- AI Productivity Analysis (Gemini encapsulado; stub en desarrollo)
- Swagger / OpenAPI
- Flyway (V1–V5)
- Bean Validation
- MapStruct
- Arquitectura Feature Based
- Tests unitarios e integración (MockMvc)
- JaCoCo

## Estado del repositorio

| Componente | Estado |
|------------|--------|
| Aplicación Spring Boot | ✅ Configurada y ejecutable |
| PostgreSQL + Flyway | ✅ V1–V5 (users, refresh_tokens, habits, tasks, pomodoro_sessions) |
| Spring Security | ✅ JWT Bearer stateless; rutas públicas auth, Swagger y Actuator |
| OpenAPI / Swagger UI | ✅ Todos los endpoints documentados |

| Feature | Estado |
|---------|--------|
| Authentication | ✅ |
| Habits | ✅ |
| Tasks | ✅ |
| Pomodoro | ✅ |
| Statistics | ✅ |
| AI Analysis | ✅ (stub Gemini; HTTP real pendiente) |
| User profile API | ⏳ Entidad/repository; sin endpoints |

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL 16+
- Flyway
- SpringDoc OpenAPI
- MapStruct · Lombok
- JJWT 0.13.0
- Maven · JaCoCo

## Roadmap

| Fase | Estado |
|------|--------|
| Setup + Security base | ✅ |
| Authentication (JWT) | ✅ |
| Habits | ✅ |
| Tasks | ✅ |
| Pomodoro | ✅ |
| Statistics | ✅ |
| AI Analysis | ✅ (stub) |
| Testing & Hardening | 🔄 Sprint 8 en curso |
| Documentation polish | ⏳ Sprint 9 |

## Calidad

- Arquitectura Feature-Based
- Clean Code · SOLID
- 154 tests · ~91 % cobertura JaCoCo (instrucciones)
- Bean Validation · MapStruct
- JUnit 5 · Mockito · MockMvc

## Configuración

Variables de entorno (valores por defecto entre paréntesis):

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto | `5432` |
| `DB_NAME` | Nombre de la base de datos | `lumind` |
| `DB_USERNAME` | Usuario | `lumind` |
| `DB_PASSWORD` | Contraseña | `lumind` |
| `SERVER_PORT` | Puerto de la aplicación | `8080` |
| `JWT_SECRET` | Secreto HMAC para firmar JWT (mín. 256 bits) | — (obligatorio) |
| `GEMINI_API_KEY` | API key de Gemini (producción AI) | — (opcional; stub en dev) |

Referencia completa en [`.env.example`](.env.example).

## Ejecución local

```bash
createdb lumind
export JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run
```

> **Nota:** `JWT_SECRET` es obligatorio. Copia [`.env.example`](.env.example) a `.env` o exporta las variables en tu shell.

```bash
mvn clean verify   # Compilar, tests (154) y reporte JaCoCo
mvn package        # Compilar y empaquetar
```

## Endpoints de API

Rutas públicas (sin JWT):

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/auth/register` | Registro |
| `POST` | `/api/v1/auth/login` | Login |
| `POST` | `/api/v1/auth/refresh` | Renovar tokens |
| — | `/swagger-ui.html`, `/v3/api-docs` | OpenAPI |
| — | `/actuator/health`, `/actuator/info` | Actuator |

Rutas protegidas (JWT Bearer):

| Método | Ruta | Descripción |
|--------|------|-------------|
| CRUD | `/api/v1/habits` | Hábitos |
| CRUD | `/api/v1/tasks` | Tareas |
| CRUD | `/api/v1/pomodoro-sessions` | Sesiones Pomodoro |
| `GET` | `/api/v1/statistics/overview` | Resumen de productividad |
| `GET` | `/api/v1/statistics/tasks` | Estadísticas de tareas |
| `GET` | `/api/v1/statistics/pomodoro-sessions` | Estadísticas Pomodoro |
| `GET` | `/api/v1/statistics/habits` | Estadísticas de hábitos |
| `POST` | `/api/v1/ai/productivity-analysis` | Análisis IA |

## Estructura del proyecto

```
src/main/java/com/lumind/api/
├── config/          # Security, JWT, OpenAPI, Gemini
├── auth/
├── user/
├── habit/
├── task/
├── pomodoro/
├── statistics/
├── ai/
└── common/
```

## Documentación

- [AGENTS.md](AGENTS.md) — documento maestro (arquitectura, estándares, workflow)
- [docs/](docs/) — contexto, roadmap, ADRs, specs, Development Log

## Licencia

[MIT](LICENSE) — Copyright (c) 2026 MeddinaDev
