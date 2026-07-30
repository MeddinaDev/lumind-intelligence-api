# Contexto del proyecto — Lumind Intelligence API

## Visión

Lumind Intelligence API es el backend de Lumind, una plataforma de productividad orientada a mejorar la gestión del tiempo, los hábitos y la concentración mediante inteligencia artificial.

El proyecto sigue una arquitectura profesional basada en features, priorizando mantenibilidad, escalabilidad y código limpio frente a la introducción innecesaria de tecnologías.

El objetivo es simular el desarrollo de un backend de producción presentable en una entrevista técnica.

---

## Features principales

| Feature | Estado | Descripción |
|---------|--------|-------------|
| Autenticación JWT | ✅ | Registro, login, refresh con rotación de tokens |
| Usuarios | 🔄 | Entidad y persistencia; perfil sin API pública aún |
| Hábitos | ✅ | CRUD REST con ownership por usuario |
| Tareas | ✅ | CRUD REST con estado completado |
| Sesiones Pomodoro | ✅ | CRUD REST con seguimiento de foco |
| Estadísticas de productividad | ✅ | Read model read-only (JPQL agregado) |
| Análisis IA | ✅ | Informe vía Gemini (stub HTTP; integración real pendiente) |
| API REST documentada | ✅ | OpenAPI / Swagger UI |

---

## Stack tecnológico

- Java 21
- Spring Boot 3.5
- Spring Security (JWT Bearer)
- Spring Data JPA
- PostgreSQL + Flyway
- SpringDoc OpenAPI
- MapStruct · Lombok
- JJWT 0.13.0
- Gemini API (encapsulada; stub en desarrollo)
- JUnit 5 · Mockito · MockMvc · JaCoCo

**Planificado:** Docker, CI/CD, Testcontainers en tests de integración.

---

## Principios de desarrollo

- Arquitectura basada en features.
- Código explícito y legible.
- Commits pequeños y lógicos.
- Toda decisión arquitectónica relevante queda registrada en un ADR.
- Sin copy-paste; reutilizar patrones existentes.
- La IA se usa como asistente de desarrollo, no como sustituto del criterio técnico.

---

## Estado actual

- **Sprint actual:** 8 — Testing & Hardening
- **Sprints completados:** 0–7 (setup, auth, habits, tasks, pomodoro, statistics, AI)
- **Tests:** 154 automatizados; ~91 % cobertura JaCoCo
- **Migraciones Flyway:** V1–V5

---

## Objetivo

Construir un backend que pueda presentarse con confianza en una entrevista técnica, demostrando prácticas de ingeniería de software en entornos reales.

---

## Referencias

- [AGENTS.md](../AGENTS.md) — documento maestro
- [ROADMAP.md](ROADMAP.md) — fases del proyecto
- [SPRINTS.md](SPRINTS.md) — planificación por sprints
- [sessions/CHANGELOG.md](sessions/CHANGELOG.md) — Development Log
