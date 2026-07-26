# Especificación técnica — Productivity Statistics (Sprint 6)

Documento de diseño — **Fase 25**. Pendiente de aprobación e implementación.

| Campo | Valor |
|-------|-------|
| Sprint | 6 — Statistics |
| Fase roadmap | Fase 6 — Productivity Statistics |
| Estado | Diseño (2026-07-26) |
| Versión API | `v1` |
| Base path | `/api/v1/statistics` |

---

## 1. Objetivos

### 1.1 Objetivo general

Exponer métricas de productividad **calculadas en tiempo de consulta** a partir de los datos ya persistidos en los módulos Habit, Task y Pomodoro, sin duplicar información ni crear tablas de agregación en Sprint 6.

### 1.2 Objetivos funcionales

| ID | Objetivo |
|----|----------|
| STAT-01 | Ofrecer un resumen consolidado de productividad del usuario autenticado en un periodo configurable. |
| STAT-02 | Exponer métricas de tareas (creación, completado y tasa de cierre). |
| STAT-03 | Exponer métricas de sesiones Pomodoro (sesiones, minutos de foco y tasa de completado). |
| STAT-04 | Exponer métricas de hábitos disponibles con el modelo actual (inventario y altas en periodo). |
| STAT-05 | Restringir todas las consultas al usuario autenticado (JWT Bearer). |

### 1.3 Objetivos no funcionales

| ID | Objetivo |
|----|----------|
| STAT-NF-01 | Solo lectura; ningún endpoint de statistics modifica datos de negocio. |
| STAT-NF-02 | Respuestas consistentes con DTOs tipados y documentación OpenAPI. |
| STAT-NF-03 | Periodos acotados (máximo 366 días) para evitar consultas costosas. |
| STAT-NF-04 | Cálculos deterministas: mismas entradas → mismas métricas. |

### 1.4 Alcance Sprint 6

**Incluido:**

- Feature `statistics`: controller, service, DTOs, queries de agregación.
- Endpoints GET protegidos bajo `/api/v1/statistics`.
- Métricas derivadas de `habits`, `tasks` y `pomodoro_sessions`.

**Excluido:**

- Entidades JPA, migraciones Flyway y tablas de caché/materialización.
- Métricas de hábitos basadas en completados o rachas (el modelo `Habit` no registra cumplimiento).
- Análisis con IA (Sprint 7).
- Exportación CSV/PDF, comparativas entre usuarios o rankings globales.
- Zona horaria por usuario (Sprint 6 opera en **UTC**; ver ADR propuesto).

### 1.5 Aprovechamiento de datos existentes (sin duplicación)

| Módulo | Datos disponibles | Uso en Statistics |
|--------|-------------------|-------------------|
| **Authentication** | Sesiones JWT, refresh tokens | Solo autenticación; no aporta métricas de productividad. |
| **User** | Identidad, `createdAt` | Filtro por `userId`; opcionalmente contexto de antigüedad de cuenta (fuera de alcance inicial). |
| **Habit** | `name`, `description`, `createdAt` | Conteo total de hábitos del usuario e hábitos creados en el periodo. **No** hay campo de completado. |
| **Task** | `completed`, `createdAt`, `updatedAt` | Tareas creadas/completadas en periodo; tasa de completado. `updatedAt` actúa como **proxy** de fecha de completado. |
| **Pomodoro** | `durationMinutes`, `completedMinutes`, `completed`, `startedAt`, `finishedAt` | Sesiones iniciadas/completadas; minutos de foco (`completedMinutes`); tasas de completado. |

**Principio:** Statistics es un **read model** transversal. Los datos maestros siguen viviendo en sus features de origen.

---

## 2. Casos de uso

| ID | Caso de uso | Actor | Descripción |
|----|-------------|-------|-------------|
| UC-STAT-01 | Consultar resumen de productividad | Usuario autenticado | Obtiene KPIs consolidados de tareas, Pomodoro y hábitos para un periodo. |
| UC-STAT-02 | Consultar estadísticas de tareas | Usuario autenticado | Obtiene métricas y tendencia diaria de tareas en un periodo. |
| UC-STAT-03 | Consultar estadísticas de Pomodoro | Usuario autenticado | Obtiene métricas y tendencia diaria de sesiones y minutos de foco. |
| UC-STAT-04 | Consultar estadísticas de hábitos | Usuario autenticado | Obtiene inventario de hábitos y altas en el periodo (limitado al modelo actual). |

---

## 3. Endpoints

Todos requieren `Authorization: Bearer <access_token>`.

Parámetros de consulta compartidos (ver `StatisticsPeriodQuery`):

| Parámetro | Tipo | Obligatorio | Default | Descripción |
|-----------|------|-------------|---------|-------------|
| `from` | `Instant` (ISO-8601) | No | `now() - 30 días` (inicio del día UTC) | Inicio del periodo (inclusivo). |
| `to` | `Instant` (ISO-8601) | No | `now()` | Fin del periodo (inclusivo). |

**Validaciones comunes:**

- `from` ≤ `to`.
- Duración máxima del periodo: **366 días**.
- Si la validación falla → `400 Bad Request` (`ErrorResponse`).

### 3.1 GET `/api/v1/statistics/overview`

| Aspecto | Detalle |
|---------|---------|
| **Finalidad** | Dashboard resumido con los KPIs principales del periodo. |
| **Request** | Query: `StatisticsPeriodQuery` (`from`, `to`). |
| **Response** | `200 OK` — `ProductivityOverviewResponse`. |
| **Origen de datos** | Agregaciones sobre `tasks`, `pomodoro_sessions`, `habits` filtradas por `userId`. |
| **Reglas de negocio** | Métricas de tareas/Pomodoro según semántica de periodo (§5). Hábitos: total actual + creados en periodo. Sin tendencia diaria (evita solapamiento con endpoints de detalle). |

### 3.2 GET `/api/v1/statistics/tasks`

| Aspecto | Detalle |
|---------|---------|
| **Finalidad** | Detalle de productividad en gestión de tareas, incluida tendencia diaria para gráficos. |
| **Request** | Query: `StatisticsPeriodQuery`. |
| **Response** | `200 OK` — `TaskStatisticsResponse`. |
| **Origen de datos** | Tabla `tasks` (`createdAt`, `updatedAt`, `completed`, `user_id`). |
| **Reglas de negocio** | Ver §5.1. Tendencia diaria: tareas completadas por día UTC. |

### 3.3 GET `/api/v1/statistics/pomodoro-sessions`

| Aspecto | Detalle |
|---------|---------|
| **Finalidad** | Detalle de foco y uso del temporizador Pomodoro, con tendencia diaria de minutos. |
| **Request** | Query: `StatisticsPeriodQuery`. |
| **Response** | `200 OK` — `PomodoroStatisticsResponse`. |
| **Origen de datos** | Tabla `pomodoro_sessions` (`startedAt`, `finishedAt`, `completed`, `completedMinutes`, `user_id`). |
| **Reglas de negocio** | Ver §5.2. Minutos de foco = suma de `completedMinutes` de sesiones completadas en periodo. |

### 3.4 GET `/api/v1/statistics/habits`

| Aspecto | Detalle |
|---------|---------|
| **Finalidad** | Visibilidad del inventario de hábitos y crecimiento en el periodo. |
| **Request** | Query: `StatisticsPeriodQuery`. |
| **Response** | `200 OK` — `HabitStatisticsResponse`. |
| **Origen de datos** | Tabla `habits` (`createdAt`, `user_id`). |
| **Reglas de negocio** | Ver §5.3. No se infieren completados ni rachas. |

---

## 4. DTOs

Convenciones alineadas con Habit, Task y Pomodoro: **records**, campos en inglés, fechas como `Instant` / `LocalDate` (UTC).

### 4.1 Request DTOs (query)

```java
// statistics/dto/request/StatisticsPeriodQuery.java
public record StatisticsPeriodQuery(
        Instant from,
        Instant to
) {}
```

Binding vía `@ParameterObject` + Bean Validation en controller:

- `@NotNull` en ambos campos si se envían; defaults aplicados en service cuando omitidos.
- Validación de rango en service (regla de negocio).

### 4.2 Response DTOs

```java
// statistics/dto/response/DailyCountResponse.java
public record DailyCountResponse(
        LocalDate date,
        long count
) {}

// statistics/dto/response/DailyMinutesResponse.java
public record DailyMinutesResponse(
        LocalDate date,
        long minutes
) {}

// statistics/dto/response/ProductivityOverviewResponse.java
public record ProductivityOverviewResponse(
        Instant from,
        Instant to,
        TaskOverviewMetrics tasks,
        PomodoroOverviewMetrics pomodoroSessions,
        HabitOverviewMetrics habits
) {
    public record TaskOverviewMetrics(
            long created,
            long completed,
            double completionRate
    ) {}

    public record PomodoroOverviewMetrics(
            long sessionsStarted,
            long sessionsCompleted,
            long totalFocusMinutes,
            double completionRate
    ) {}

    public record HabitOverviewMetrics(
            long totalHabits,
            long createdInPeriod
    ) {}
}

// statistics/dto/response/TaskStatisticsResponse.java
public record TaskStatisticsResponse(
        Instant from,
        Instant to,
        long created,
        long completed,
        long pendingCreatedInPeriod,
        double completionRate,
        List<DailyCountResponse> completedByDay
) {}

// statistics/dto/response/PomodoroStatisticsResponse.java
public record PomodoroStatisticsResponse(
        Instant from,
        Instant to,
        long sessionsStarted,
        long sessionsCompleted,
        long totalFocusMinutes,
        long averageFocusMinutesPerCompletedSession,
        double completionRate,
        List<DailyMinutesResponse> focusMinutesByDay
) {}

// statistics/dto/response/HabitStatisticsResponse.java
public record HabitStatisticsResponse(
        Instant from,
        Instant to,
        long totalHabits,
        long createdInPeriod
) {}
```

**Notas:**

- `completionRate`: valor entre `0.0` y `1.0` (double); `0.0` si el denominador es 0.
- `pendingCreatedInPeriod`: tareas con `completed = false` y `createdAt` dentro del periodo.
- No se requiere MapStruct para statistics en Sprint 6 (sin entidades propias); mappers solo si la proyección lo justifica.

---

## 5. Métricas y reglas de negocio

### 5.1 Tareas

| Métrica | Definición | Justificación |
|---------|------------|---------------|
| `created` | `COUNT` tareas con `createdAt ∈ [from, to]` | Mide volumen de planificación en el periodo. |
| `completed` | `COUNT` tareas con `completed = true` y `updatedAt ∈ [from, to]` | Aproxima completados; no existe `completedAt`. Documentado como limitación. |
| `pendingCreatedInPeriod` | `COUNT` tareas con `completed = false` y `createdAt ∈ [from, to]` | Identifica backlog generado en el periodo. |
| `completionRate` | `completed / created` si `created > 0`, else `0.0` | KPI simple de cierre sobre lo planificado en el periodo. |
| `completedByDay` | Agrupación UTC por día de `updatedAt` donde `completed = true` | Alimenta gráficos de actividad sin duplicar overview. |

### 5.2 Sesiones Pomodoro

| Métrica | Definición | Justificación |
|---------|------------|---------------|
| `sessionsStarted` | `COUNT` con `startedAt ∈ [from, to]` | Mide intención de foco. |
| `sessionsCompleted` | `COUNT` con `completed = true` y `finishedAt ∈ [from, to]` | Sesiones cerradas correctamente. |
| `totalFocusMinutes` | `SUM(completedMinutes)` con `completed = true` y `finishedAt ∈ [from, to]` | Tiempo efectivo de concentración. |
| `averageFocusMinutesPerCompletedSession` | `totalFocusMinutes / sessionsCompleted` (0 si no hay completadas) | Calidad media de las sesiones. |
| `completionRate` | `sessionsCompleted / sessionsStarted` si `sessionsStarted > 0`, else `0.0` | Eficiencia del uso del temporizador. |
| `focusMinutesByDay` | Suma diaria UTC de `completedMinutes` por `finishedAt` | Tendencia de foco para visualizaciones. |

### 5.3 Hábitos

| Métrica | Definición | Justificación |
|---------|------------|---------------|
| `totalHabits` | `COUNT` total de hábitos del usuario (estado actual) | Inventario activo; no depende del periodo. |
| `createdInPeriod` | `COUNT` con `createdAt ∈ [from, to]` | Crecimiento del sistema de hábitos. |

**Métricas explícitamente excluidas (modelo actual):**

- Rachas, tasa de cumplimiento de hábitos, hábitos completados por día.
- Tiempo total en tareas (no hay tracking de duración).
- Métricas derivadas de refresh tokens o datos de auth.

### 5.4 Métricas del overview (sin redundancia)

El overview expone **únicamente** subconjuntos resumidos (`TaskOverviewMetrics`, `PomodoroOverviewMetrics`, `HabitOverviewMetrics`). Las tendencias diarias permanecen en los endpoints de detalle para no duplicar payload ni lógica.

---

## 6. Arquitectura del módulo

```
statistics/
├── StatisticsController.java
├── service/
│   └── ProductivityStatisticsService.java
├── repository/
│   └── ProductivityStatisticsRepository.java   // @Query JPQL; sin entidad propia
├── dto/
│   ├── request/
│   │   └── StatisticsPeriodQuery.java
│   └── response/
│       ├── ProductivityOverviewResponse.java
│       ├── TaskStatisticsResponse.java
│       ├── PomodoroStatisticsResponse.java
│       ├── HabitStatisticsResponse.java
│       ├── DailyCountResponse.java
│       └── DailyMinutesResponse.java
└── exception/
    └── InvalidStatisticsPeriodException.java     // 400 si from > to o periodo > 366 días
```

**Dependencias permitidas (Sprint 6):**

- Repositorio de consultas en `statistics` con JPQL sobre entidades `Task`, `PomodoroSession`, `Habit` (acoplamiento de lectura documentado en ADR).
- `AuthenticatedUser` desde JWT (patrón Habit/Task/Pomodoro).
- `common.exception` para errores.

**Dependencias prohibidas:**

- Llamadas a services de otras features.
- Exposición de entidades JPA en la API.
- Escritura en tablas de otras features.

---

## 7. Decisiones arquitectónicas — ADR propuestos (no creados en Fase 25)

| ADR propuesto | Tema | Motivo |
|---------------|------|--------|
| **007-statistics-read-model.md** | Statistics como agregación read-only sin persistencia propia | Evitar duplicación de datos; fijar que Sprint 6 calcula en consulta vía JPQL en `statistics.repository`. |
| **008-statistics-period-semantics.md** | Semántica temporal UTC y campos por entidad | Clarificar uso de `updatedAt` como proxy de completado en Task; límites de periodo; días UTC en tendencias. |

---

## 8. Plan de implementación sugerido (fases posteriores)

| Fase | Contenido |
|------|-----------|
| 26 | `ProductivityStatisticsRepository` + queries JPQL |
| 27 | `ProductivityStatisticsService` + reglas de periodo |
| 28 | `StatisticsController` + DTOs + OpenAPI |
| 29 | Excepciones + integración `GlobalExceptionHandler` |
| 30 | Tests unitarios e integración MockMvc |

---

## 9. Referencias

- [docs/ROADMAP.md](../../ROADMAP.md) — Fase 6
- [docs/SPRINTS.md](../../SPRINTS.md) — Sprint 6
- [docs/decisions/001-feature-based-architecture.md](../../decisions/001-feature-based-architecture.md)
- Módulos existentes: `habit`, `task`, `pomodoro`, `user`, `auth`
