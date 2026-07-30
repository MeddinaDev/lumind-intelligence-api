# ADR 009 — AI Depends on Statistics

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-30 |
| Sprint | 7 — Artificial Intelligence |

---

## Contexto

El módulo AI genera análisis de productividad en lenguaje natural. Necesita métricas agregadas sin acoplarse a los datos maestros de Habit, Task o Pomodoro.

Alternativas consideradas:

| Opción | Descripción | Descartada porque |
|--------|-------------|-------------------|
| Acceso directo a repositories de dominio | AI consulta Task/Habit/Pomodoro | Rompe desacoplamiento; duplica lógica de agregación |
| Duplicar queries en AI | Copiar JPQL de Statistics en AI | Mantenimiento doble; inconsistencia de métricas |
| **Consumir Statistics** | AI usa `ProductivityStatisticsService` | — **Elegida** |

---

## Decisión

- AI es un **consumidor downstream** del read model Statistics.
- `ProductivityAnalysisService` obtiene métricas exclusivamente vía `ProductivityStatisticsService` (`getOverview`, `getTaskStatistics`, `getPomodoroStatistics`, `getHabitStatistics`).
- AI **no** accede a `HabitRepository`, `TaskRepository`, `PomodoroSessionRepository` ni a services de esas features.
- El `userId` llega desde JWT (`@AuthenticationPrincipal`); no hay dependencia de `AuthService`.

---

## Consecuencias

### Positivas

- Desacoplamiento claro entre features de dominio e IA.
- Cambios internos en Task/Habit/Pomodoro no afectan AI mientras Statistics mantenga su contrato.
- Métricas consistentes entre endpoints de statistics y análisis IA.

### Negativas

- AI hereda limitaciones del read model Statistics (proxies temporales, acoplamiento JPQL).
- Latencia acumulada: AI invoca múltiples métodos de statistics antes de llamar al LLM.

---

## Referencias

- [docs/spec/ai/SPECIFICATION.md](../spec/ai/SPECIFICATION.md)
- [ADR 007 — Statistics Read Model](007-statistics-read-model.md)
- [docs/spec/statistics/SPECIFICATION.md](../spec/statistics/SPECIFICATION.md)
