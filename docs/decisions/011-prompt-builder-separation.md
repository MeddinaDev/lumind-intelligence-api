# ADR 011 — Prompt Builder Separation

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-30 |
| Sprint | 7 — Artificial Intelligence |

---

## Contexto

El prompt enviado al modelo de lenguaje debe construirse a partir de métricas agregadas, respetando reglas de no envío de PII y un schema JSON de respuesta esperado. Mezclar esta lógica en el Service dificulta pruebas y auditoría.

---

## Decisión

- `ProductivityAnalysisPromptBuilder` es un componente dedicado en `ai.prompt`.
- Recibe `ProductivityAnalysisPromptInput` (DTO interno con overview, tasks, pomodoro, habits).
- Responsabilidades del builder:
  - Serializar métricas agregadas (sin email, nombres de tareas ni hábitos).
  - Incluir instrucciones de formato JSON de respuesta (`summary`, `insights`, `recommendations`).
  - Registrar `JavaTimeModule` para serializar `Instant` correctamente.
- `ProductivityAnalysisService` delega la construcción del prompt al builder; no contiene plantillas ni lógica de serialización del prompt.

---

## Consecuencias

### Positivas

- Separación de responsabilidades (SRP): Service orquesta; builder construye prompt.
- Tests unitarios independientes del builder (`ProductivityAnalysisPromptBuilderTest`).
- Auditoría clara de qué datos se envían al modelo.

### Negativas

- Componente adicional en el paquete AI.
- Cambios en el contrato de respuesta del modelo requieren actualizar builder y parser en Service.

---

## Referencias

- [docs/spec/ai/SPECIFICATION.md](../spec/ai/SPECIFICATION.md)
- [ADR 009 — AI Depends on Statistics](009-ai-statistics-dependency.md)
- [ADR 012 — Stateless AI Analysis](012-stateless-ai-analysis.md)
