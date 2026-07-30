# ADR 012 — Stateless AI Analysis

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-30 |
| Sprint | 7 — Artificial Intelligence |

---

## Contexto

El análisis de productividad con IA puede invitar a persistir historial, caché de respuestas o prompts para reutilización. Sprint 7 debe entregar valor con alcance acotado.

---

## Decisión

- Cada petición a `POST /api/v1/ai/productivity-analysis` genera un análisis **stateless**:
  - No se persisten prompts enviados al modelo.
  - No se persisten respuestas del modelo en base de datos.
  - No hay historial de análisis ni caché de resultados en Sprint 7.
- La respuesta incluye `generatedAt` (timestamp de generación) pero no un identificador persistido.
- Re-análisis del mismo periodo invoca de nuevo al proveedor LLM.

### Evolución futura (fuera de Sprint 7)

- Historial de análisis, caché por `(userId, from, to)` y rate limiting server-side.

---

## Consecuencias

### Positivas

- Implementación simple; sin migraciones Flyway adicionales para IA.
- Sin gestión de consistencia entre caché y datos maestros.
- Privacidad: no se almacenan respuestas del modelo en BD.

### Negativas

- Coste de API repetido en consultas idénticas.
- Cliente no puede recuperar un análisis anterior sin regenerarlo.

---

## Referencias

- [docs/spec/ai/SPECIFICATION.md](../spec/ai/SPECIFICATION.md)
- [ADR 009 — AI Depends on Statistics](009-ai-statistics-dependency.md)
- [ADR 010 — AI Provider Encapsulation](010-ai-provider-encapsulation.md)
