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
- Las fechas se modelan como instantes absolutos mediante **`Instant`** / **`TIMESTAMPTZ`**.
- Las conexiones PostgreSQL (Hikari) se inicializan con **`SET TIME ZONE 'UTC'`**, junto con `hibernate.jdbc.time_zone=UTC`, para que operaciones como `CAST(timestamptz AS date)` en agrupaciones calendariales sean deterministas y coherentes con la semántica UTC de la API.

### Semántica por entidad

| Entidad | Campo(s) usados | Semántica |
|---------|-----------------|-----------|
| **Task** | `createdAt` | Tareas creadas en el periodo |
| **Task** | `completedAt` + `completed = true` | Momento UTC en que la tarea pasó a completada; no cambia al editar metadatos |
| **PomodoroSession** | `startedAt` | Sesiones iniciadas en el periodo |
| **PomodoroSession** | `finishedAt` + `completed = true` | Sesiones completadas |
| **PomodoroSession** | `completedMinutes` | Minutos de foco acumulados |
| **Habit** | `createdAt` | Hábitos creados en el periodo; inventario total del usuario |

### Agrupación diaria

- Tendencias diarias usan `CAST(timestamp AS localdate)` en JPQL; PostgreSQL aplica la zona de sesión al convertir `timestamptz` a fecha, por lo que la sesión se fija a **UTC** en el pool de conexiones.

---

## Consecuencias

### Positivas

- Reglas temporales documentadas y consistentes entre endpoints de statistics.
- Validación centralizada de periodos en `ProductivityStatisticsService`.

### Negativas

- Tareas completadas antes de F45 conservan `completed_at` backfill desde `updated_at` (aproximación histórica).

---

## Referencias

- [docs/spec/statistics/SPECIFICATION.md](../spec/statistics/SPECIFICATION.md)
- [ADR 007 — Statistics Read Model](007-statistics-read-model.md)
