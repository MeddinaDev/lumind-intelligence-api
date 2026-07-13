# Lumind Intelligence API

Backend Spring Boot del proyecto Lumind. Repositorio en fase de configuración inicial: aplicación ejecutable, seguridad, documentación OpenAPI y estructura de paquetes por features.

> Actualmente se encuentra en desarrollo activo. Ya incluye autenticación completa mediante JWT, gestión de hábitos, documentación OpenAPI, migraciones con Flyway y una arquitectura modular organizada por features.


## Estado del repositorio

| Componente | Estado |
|------------|--------|
| Aplicación Spring Boot | Configurada y ejecutable |
| Conexión PostgreSQL + Flyway | Configurados; migraciones `V1` (users) y `V2` (refresh_tokens) |
| Spring Security | HTTP Basic; rutas públicas en Swagger y Actuator; JWT: dependencia y config (Fase 0), lógica pendiente |
| Feature `user` | Entidad `User`, `UserRepository`, `UserSummaryResponse`, `UserMapper` (Fase 2) |
| OpenAPI / Swagger UI | Configurado (sin operaciones de API) |
| Endpoints REST de negocio | No implementados |
| Tests | No implementados |

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- SpringDoc OpenAPI
- MapStruct · Lombok
- Maven

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 16+

## Roadmap

✅ Authentication

✅ JWT Security

✅ Habit CRUD

🚧 Tasks

🚧 Pomodoro

🚧 Statistics

🚧 AI Assistant

🚧 Notifications

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

Referencia completa en [`.env.example`](.env.example).

## Ejecución local

```bash
# Crear la base de datos y el usuario en PostgreSQL
createdb lumind

# Definir JWT_SECRET (obligatorio para arrancar la aplicación)
export JWT_SECRET=$(openssl rand -base64 32)

# Arrancar la aplicación
mvn spring-boot:run
```

> **Nota:** `JWT_SECRET` debe estar definido antes de ejecutar `mvn spring-boot:run`. Sin esta variable, Spring Boot no puede resolver la configuración JWT en `application.yml`. Puedes copiar [`.env.example`](.env.example) a `.env` y exportar las variables, o definirlas manualmente en tu shell.

Otros comandos útiles:

```bash
mvn test        # Ejecutar tests (cuando existan)
mvn package     # Compilar y empaquetar
```

## Endpoints disponibles

| Ruta | Descripción | Acceso |
|------|-------------|--------|
| `/swagger-ui.html` | Interfaz Swagger UI | Público |
| `/v3/api-docs` | Especificación OpenAPI | Público |
| `/actuator/health` | Estado de salud | Público |
| `/actuator/info` | Información de la aplicación | Público |

El resto de rutas requieren autenticación HTTP Basic.

## Estructura del proyecto

```
src/main/java/com/lumind/api/
├── config/          # Security, OpenAPI
├── auth/            
├── user/            # User, UserRepository, UserSummaryResponse, UserMapper
├── habit/
├── task/
├── pomodoro/
├── statistics/
├── ai/
└── common/
```

## Documentación

- [AGENTS.md](AGENTS.md) — documento maestro del proyecto (arquitectura, estándares, workflow)
- [docs/](docs/) — contexto, roadmap, decisiones arquitectónicas

## Licencia

[MIT](LICENSE) — Copyright (c) 2026 MeddinaDev
