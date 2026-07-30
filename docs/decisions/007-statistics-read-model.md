# ADR 007 — Statistics Read Model

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-30 |
| Sprint | 6 — Statistics |

---

## Contexto

El módulo Statistics debe exponer métricas de productividad sin duplicar datos ni crear tablas de agregación en Sprint 6. Las features Habit, Task y Pomodoro ya persisten los datos maestros.

Alternativas consideradas:

| Opción | Descripción | Descartada porque |
|--------|-------------|-------------------|
| Tablas materializadas / caché | Pre-calcular agregados en BD | Complejidad prematura; sincronización con datos maestros |
| Servicios cross-feature | Statistics invoca `HabitService`, `TaskService`, etc. | Acoplamiento entre features; viola aislamiento |
| **Read model en Statistics** | JPQL agregado sobre entidades existentes | — **Elegida** |

---

## Decisión

Statistics actúa como **read model transversal read-only**:

- No posee entidades JPA propias ni migraciones Flyway dedicadas en Sprint 6.
- `ProductivityStatisticsRepository` ejecuta consultas JPQL de agregación sobre `Task`, `Habit` y `PomodoroSession`.
- El acoplamiento de lectura al esquema interno de otras features queda **documentado y aceptado** como deuda técnica consciente.
- Ningún endpoint de Statistics modifica datos de negocio.

---

## Consecuencias

### Positivas

- Sin duplicación de datos ni tablas adicionales.
- Implementación rápida y determinista (mismas entradas → mismas métricas).
- API de statistics independiente de la lógica CRUD de cada feature.

### Negativas

- Statistics depende del modelo interno de Habit/Task/Pomodoro; cambios de esquema pueden requerir ajustes en queries.
- Consultas cross-entity pueden requerir índices compuestos adicionales en fases de hardening.

---

## Referencias

- [docs/spec/statistics/SPECIFICATION.md](../spec/statistics/SPECIFICATION.md)
- [ADR 001 — Feature-based architecture](001-feature-based-architecture.md)
- [ADR 008 — Temporal Semantics](008-temporal-semantics.md)
