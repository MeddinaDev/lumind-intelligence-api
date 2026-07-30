# ADR 008 — Temporal Semantics (Statistics)

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-30 |
| Sprint | 6 — Statistics |

---

## Contexto

Las métricas de Statistics agrupan datos por periodos temporales. Cada entidad de dominio expone timestamps distintos y el modelo de Habit no registra cumplimiento diario.

---

## Decisión

### Zona horaria

- Sprint 6 opera en **UTC** para resolución de periodos y agrupación diaria.
- Periodo por defecto: últimos **30 días** UTC cuando `from`/`to` no se envían.
- Periodo máximo: **366 días**; rangos inválidos devuelven `400`.

### Semántica por entidad

| Entidad | Campo(s) usados | Semántica |
|---------|-----------------|-----------|
| **Task** | `createdAt` | Tareas creadas en el periodo |
| **Task** | `updatedAt` + `completed = true` | **Proxy** de fecha de completado (deuda técnica: no existe `completedAt`) |
| **PomodoroSession** | `startedAt` | Sesiones iniciadas en el periodo |
| **PomodoroSession** | `finishedAt` + `completed = true` | Sesiones completadas |
| **PomodoroSession** | `completedMinutes` | Minutos de foco acumulados |
| **Habit** | `createdAt` | Hábitos creados en el periodo; inventario total del usuario |

### Agrupación diaria

- Tendencias diarias usan `CAST(timestamp AS localdate)` en JPQL, dependiente del timezone JVM/Hibernate.
- Evolución futura: normalización explícita a UTC en PostgreSQL.

---

## Consecuencias

### Positivas

- Reglas temporales documentadas y consistentes entre endpoints de statistics.
- Validación centralizada de periodos en `ProductivityStatisticsService`.

### Negativas

- `updatedAt` como proxy de completado puede distorsionar métricas si se edita una tarea tras marcarla completada.
- Agrupación diaria sensible a timezone del entorno de ejecución.

---

## Referencias

- [docs/spec/statistics/SPECIFICATION.md](../spec/statistics/SPECIFICATION.md)
- [ADR 007 — Statistics Read Model](007-statistics-read-model.md)
