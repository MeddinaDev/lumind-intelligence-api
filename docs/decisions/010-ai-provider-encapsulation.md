# ADR 010 — AI Provider Encapsulation

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-30 |
| Sprint | 7 — Artificial Intelligence |

---

## Contexto

La integración con Gemini implica HTTP, configuración de API key, timeouts y traducción de errores del proveedor. El resto del sistema no debe depender de detalles de Gemini.

---

## Decisión

- Se define la abstracción `AiLanguageModelClient` con método `generateCompletion(String prompt)`.
- La implementación concreta `GeminiClient` vive en `ai.client.gemini` y encapsula:
  - Configuración (`GeminiProperties`, `GeminiConfig`)
  - Comunicación HTTP (`RestClient`)
  - Traducción de excepciones de infraestructura a excepciones de dominio IA
- Controller y Service dependen únicamente de `AiLanguageModelClient`, no de Gemini directamente.
- Sustitución de proveedor futuro: nueva implementación de la interfaz sin cambiar capa de negocio.

### Estado de implementación

- Stub operativo en desarrollo; integración HTTP real pendiente (deuda técnica aceptada).
- Métodos de traducción y validación preparados para la fase de integración HTTP.

---

## Consecuencias

### Positivas

- Testabilidad: tests unitarios e integración mockean `AiLanguageModelClient`.
- Aislamiento de SDK/HTTP y secretos del proveedor.
- Evolución a multi-proveedor posible sin refactor del Service.

### Negativas

- Capa de abstracción adicional para un único proveedor en Sprint 7.
- Stub puede ocultar fallos de integración hasta despliegue con API key real.

---

## Referencias

- [docs/spec/ai/SPECIFICATION.md](../spec/ai/SPECIFICATION.md)
- [ADR 009 — AI Depends on Statistics](009-ai-statistics-dependency.md)
