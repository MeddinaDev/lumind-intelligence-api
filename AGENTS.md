# AGENTS.md

Documento maestro del proyecto Lumind Intelligence API. Es la fuente de verdad para agentes IA y desarrolladores: define misión, arquitectura, estándares, workflow y criterios de calidad. El resto de la documentación deriva de este archivo y no debe contradecirlo.

---

## Rol y jerarquía documental

| Documento | Función |
|-----------|---------|
| **AGENTS.md** (este archivo) | Reglas, principios, arquitectura, workflow, quality gate |
| **/docs** | Contexto de negocio, roadmap, ADRs, domain model, specs |
| **README.md** | Onboarding rápido: setup, estado, enlaces |
| **docs/LANGUAGE_POLICY.md** | Política oficial de idiomas |

Antes de implementar cualquier cambio, lee este archivo. Consulta `/docs` para el detalle de negocio y decisiones arquitectónicas.

---

## Política de idioma

Consulta [docs/LANGUAGE_POLICY.md](docs/LANGUAGE_POLICY.md) para el detalle completo.

- **Código** (clases, métodos, variables, endpoints, DTOs): inglés.
- **Documentación interna** (`/docs`, este archivo): español.
- **README público**: inglés en Sprint 9. Hasta entonces, README provisional en español.

Los documentos en `/docs` que aún estén en inglés se traducirán progresivamente cuando se modifiquen. No traducir masivamente sin cambios de contenido.

---

## Misión

Estás contribuyendo a Lumind Intelligence API, un backend de estilo producción diseñado para demostrar prácticas profesionales de ingeniería de software.

No es un tutorial ni un proyecto experimental. Cada cambio debe mejorar mantenibilidad, legibilidad, escalabilidad y calidad a largo plazo. Prioriza siempre la arquitectura sobre la velocidad de implementación.

---

## Visión del proyecto

Lumind es una plataforma de productividad impulsada por IA. El objetivo es construir un backend que pueda presentarse con confianza en una entrevista técnica.

### Módulos principales

- Autenticación
- Usuarios
- Hábitos
- Tareas
- Sesiones Pomodoro
- Estadísticas de productividad
- Análisis de productividad con IA

---

## Estado actual del proyecto

**Fase 1 / Sprint 1** — scaffolding inicial. Ten en cuenta el estado real del código:

| Área | Estado |
|------|--------|
| Endpoints de negocio | No implementados (sin `@RestController`) |
| Seguridad | HTTP Basic (`SecurityConfig`); JWT planificado (Fase 2) |
| Flyway | Configurado; sin migraciones aún |
| Docker | Planificado; sin `Dockerfile` ni `docker-compose` |
| Gemini API | Planificado (Fase 7); sin integración |
| Tests | Dependencias presentes; sin tests escritos |

No asumas que JWT, Docker o Gemini están disponibles hasta que existan en `pom.xml` y en el código.

---

## Tech stack

### Implementado

- Java 21
- Spring Boot 3.5
- Spring Security (HTTP Basic)
- Spring Data JPA
- PostgreSQL
- Flyway
- SpringDoc / Swagger (OpenAPI)
- MapStruct
- Lombok
- JUnit + Mockito (dependencias; tests pendientes)
- Spring Boot Actuator

### Planificado

- JWT (autenticación)
- Docker
- Gemini API (análisis de productividad)

Nunca introduzcas nuevas tecnologías o dependencias sin aprobación explícita.

---

## Arquitectura

El proyecto sigue una **arquitectura basada en features**. Cada feature posee su propio:

- Controller
- Service
- Repository
- Entity
- DTO
- Mapper

El código compartido vive únicamente en el paquete `common`. Nunca introduzcas capas o paquetes que rompan esta arquitectura.

Detalle completo: [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md)
Decisión registrada: [docs/decisions/001-feature-based-architecture.md](docs/decisions/001-feature-based-architecture.md)

### Estructura de paquetes

```
src/main/java/com/lumind/api/
├── LumindIntelligenceApiApplication.java
├── config/
├── auth/
├── user/
├── habit/
├── task/
├── pomodoro/
├── statistics/
├── ai/
└── common/
    ├── constants/
    ├── exception/
    └── util/
```

---

## Principios de ingeniería

Prioridades:

1. Corrección
2. Seguridad
3. Mantenibilidad
4. Legibilidad
5. Rendimiento

Cuando tengas dudas:

- Prefiere código explícito.
- Prefiere simplicidad.
- Prefiere convenciones de Spring Boot.
- Evita abstracciones innecesarias.
- Evita optimización prematura.

---

## Workflow de desarrollo

Antes de implementar cualquier feature:

1. Lee la documentación relevante en `/docs` (ver mapa más abajo).
2. Revisa el sprint actual: [docs/SPRINTS.md](docs/SPRINTS.md)
3. Revisa el roadmap: [docs/ROADMAP.md](docs/ROADMAP.md)
4. Revisa el domain model: [docs/domain/DOMAIN_MODEL.md](docs/domain/DOMAIN_MODEL.md)
5. Revisa ADRs si aplica: [docs/decisions/](docs/decisions/)
6. Revisa la especificación de la feature: [docs/spec/](docs/spec/)
7. Produce un plan de implementación cuando se solicite.

Nunca empieces a implementar features grandes sin un plan aprobado.

---

## Estándares de código

### Inyección y diseño

- Usa únicamente inyección por constructor. Nunca inyección por campo.
- Mantén métodos pequeños y nombres significativos.
- Evita lógica duplicada.
- Sigue SOLID cuando sea apropiado.
- No crees clases utilitarias salvo que sean realmente reutilizables.

### Paquetes

- Respeta la estructura de paquetes existente.
- Nunca muevas clases entre features sin justificación.
- Evita acoplamiento entre features.

### Entidades

- Representan conceptos de negocio.
- Nunca expongas entidades fuera de la capa Service.
- Mantén las entidades centradas en datos de negocio.
- Evita lógica de negocio en controllers.

### DTOs

- Los controllers se comunican únicamente mediante DTOs.
- Cada request usa Request DTOs.
- Cada response usa Response DTOs.
- Nunca expongas entidades JPA a través de la API.

### Mappers

- Usa MapStruct.
- Nunca dupliques lógica de mapeo manualmente.
- Mantén la responsabilidad de mapeo dentro de las clases mapper.

### Repositories

- Solo acceden a persistencia.
- Nunca coloques lógica de negocio en repositories.

### Services

- Las reglas de negocio pertenecen a Services.
- Los services coordinan repositories, mappers y validación.

### Controllers

Los controllers deben ser delgados. Responsabilidades:

- Recibir requests
- Validar input
- Delegar a services
- Devolver responses

Nada más.

---

## Validación y excepciones

### Validación

- Usa Bean Validation para input externo.
- La validación de negocio pertenece a Services.
- Nunca dupliques reglas de validación.

### Excepciones

- Usa manejo global de excepciones.
- Devuelve respuestas de error consistentes.
- Nunca expongas excepciones internas.

---

## Seguridad

- La seguridad es prioritaria.
- Las contraseñas deben codificarse siempre.
- Nunca expongas información sensible.
- Usa el principio de mínimo privilegio.
- Sigue las mejores prácticas de Spring Security.

Estrategia de seguridad: [docs/decisions/003-security-strategy.md](docs/decisions/003-security-strategy.md)
JWT (planificado): [docs/decisions/004-jwt.md](docs/decisions/004-jwt.md)

---

## Base de datos

- Todos los cambios de esquema deben usar Flyway.
- Nunca modifiques la estructura de la base de datos manualmente.
- Los archivos de migración deben estar versionados.

Decisión Flyway: [docs/decisions/002-flyway.md](docs/decisions/002-flyway.md)

---

## Documentación de API

- Cada endpoint debe aparecer en Swagger.
- Documenta request y response DTOs.
- Mantén la documentación de API actualizada.

---

## Testing

- Cada regla de negocio importante debe ser testeable.
- Escribe tests limpios y mantenibles.
- Prefiere tests significativos sobre alta cobertura.

---

## Documentación como parte de la feature

- Si cambia la arquitectura: actualiza la documentación.
- Si cambian reglas de negocio: actualiza el Domain Model si es necesario.
- Si se toman decisiones: crea o actualiza un ADR.

Al modificar un documento en `/docs`, tradúcelo al español si aún está en inglés.

---

## Git workflow

- Mantén commits pequeños.
- Cada commit debe representar un cambio lógico.
- Evita mezclar modificaciones no relacionadas.

---

## Quality gate

Una feature solo está terminada si cumple **todos** estos criterios:

- El proyecto compila correctamente.
- Los tests pasan.
- Swagger está actualizado.
- DTOs completos (request y response).
- Mapper creado.
- Validaciones implementadas.
- Excepciones manejadas.
- Logs adecuados.
- Migración Flyway creada si aplica.
- Documentación actualizada.
- Sin warnings.
- Sigue las convenciones de este archivo.

Checklist detallado: [docs/  DEFINITION_OF_DONE.md](docs/  DEFINITION_OF_DONE.md)

---

## Acciones prohibidas

Nunca:

- Introduzcas dependencias innecesarias.
- Rompas la arquitectura del proyecto.
- Generes código sin usar.
- Añadas abstracciones especulativas.
- Ignores la documentación existente.
- Omitas validación.
- Omitas manejo de errores.
- Omitas actualizaciones de documentación.

Pregunta siempre antes de tomar decisiones arquitectónicas.

---

## Mapa de documentación

| Documento | Contenido |
|-----------|-----------|
| [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | Visión, features, stack |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Fases del proyecto (1–9) |
| [docs/SPRINTS.md](docs/SPRINTS.md) | Planificación por sprints |
| [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) | Arquitectura basada en features |
| [docs/domain/DOMAIN_MODEL.md](docs/domain/DOMAIN_MODEL.md) | Modelo de dominio |
| [docs/DEFINITION_OF_DONE.md](docs/  DEFINITION_OF_DONE.md) | Checklist de feature terminada |
| [docs/LANGUAGE_POLICY.md](docs/LANGUAGE_POLICY.md) | Política de idiomas |
| [docs/decisions/](docs/decisions/) | Architecture Decision Records |
| [docs/spec/](docs/spec/) | Especificaciones y tareas de features |
| [docs/sessions/](docs/sessions/) | Notas de sesiones de desarrollo |

## Filosofía de revisión

Al revisar código o documentación:

- No propongas cambios por preferencia personal.
- Justifica técnicamente cada sugerencia.
- Explica beneficios y posibles inconvenientes.
- Si la solución actual es válida, indícalo explícitamente.
- No reescribas código completo si basta con un cambio localizado.

## Modo de trabajo

Plan Mode:
- Analiza.
- Haz preguntas si falta contexto.
- Propón un plan.
- Espera aprobación.

Build Mode:
- Ejecuta únicamente el plan aprobado.
- No introduzcas cambios adicionales.

## Antes de responder:

- Revisa el estado real del proyecto.
- No asumas funcionalidades no implementadas.
- Si existe documentación relacionada, consúltala antes de responder.
- Cuando detectes una decisión arquitectónica relevante, propón crear o actualizar un ADR antes de implementarla.
