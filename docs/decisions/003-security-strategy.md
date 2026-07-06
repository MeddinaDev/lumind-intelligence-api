# ADR 003 — Estrategia de seguridad

| Campo | Valor |
|-------|-------|
| Estado | Aceptada |
| Fecha | 2026-07-06 |
| Sprint | 2 — Authentication |

---

## Contexto

En Sprint 1 se configuró Spring Security con **HTTP Basic** como mecanismo temporal. Todas las rutas excepto Swagger y Actuator requieren autenticación básica.

En Sprint 2 (Fase 2 del roadmap) la API debe ofrecer autenticación propia mediante registro y login, con tokens JWT para acceder a recursos protegidos. Lumind es una API REST stateless orientada a clientes (web/móvil).

La especificación técnica de autenticación está aprobada en [docs/spec/authentication/SPECIFICATION.md](../spec/authentication/SPECIFICATION.md).

---

## Decisión

Se adopta la siguiente estrategia de seguridad para Sprint 2:

### Autenticación

- Mecanismo principal: **JWT Bearer** en header `Authorization`.
- HTTP Basic se sustituirá por JWT en la Fase 6 de implementación (filtro y `SecurityConfig`).
- Sesión Spring Security: **STATELESS** (sin sesión HTTP server-side).

### Contraseñas

- Algoritmo: **BCrypt** mediante `BCryptPasswordEncoder`.
- Las contraseñas nunca se almacenan, loguean ni devuelven en texto plano.

### Refresh tokens

- Los refresh tokens JWT se persisten en base de datos como **hash SHA-256** (detalle en [ADR 004](004-jwt.md)).
- Rotación obligatoria en cada uso del endpoint `/api/v1/auth/refresh`.

### Rutas públicas (sin JWT)

| Ruta | Motivo |
|------|--------|
| `/api/v1/auth/**` | Registro, login y refresh |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Documentación OpenAPI |
| `/actuator/health`, `/actuator/info` | Infraestructura |

Todas las demás rutas requieren access token JWT válido.

### CSRF

- **Deshabilitado.** Coherente con API stateless que usa JWT en header, no cookies.

### Anti-enumeración

- En login fallido, respuesta genérica: `"Invalid email or password"` (`401`), sin revelar si el email existe.

### Transporte

- En producción, la API debe servirse exclusivamente sobre **HTTPS**.
- Los tokens viajan en header `Authorization`, nunca en query params ni cookies en Sprint 2.

### Principio de mínimo privilegio

- Endpoints de auth son públicos solo los estrictamente necesarios (register, login, refresh).
- Actuator expone únicamente `health` e `info`.

---

## Fuera de alcance (Sprint 2)

- Logout server-side / blacklist de access tokens.
- Rate limiting en `/login` y `/register`.
- OAuth2 / login social.
- MFA (autenticación multifactor).
- Recuperación de contraseña.

Estas mejoras quedan documentadas como evolución futura en la spec de autenticación.

---

## Consecuencias

### Positivas

- Modelo de autenticación estándar en APIs REST.
- Desacoplamiento entre cliente y servidor (stateless).
- Alineación con prácticas de producción presentables en entrevista técnica.

### Negativas

- Revocación inmediata de access tokens no es posible sin blacklist (TTL corto de 15 min mitiga el riesgo).
- Logout es responsabilidad del cliente hasta implementar revocación server-side de refresh tokens.

---

## Referencias

- [docs/spec/authentication/SPECIFICATION.md](../spec/authentication/SPECIFICATION.md) — §9, §10
- [ADR 004 — JWT](004-jwt.md)
- [AGENTS.md](../../AGENTS.md) — sección Seguridad
