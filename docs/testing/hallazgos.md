# Hallazgos para las demostraciones de testing

> ¿Qué? Defectos reales de este proyecto que se dejan **sin corregir a propósito**.
> ¿Para qué? Demostrar en clase cómo cada tipo de prueba encuentra lo que las otras no ven, en
> una app real y no en un ejemplo de juguete. Se usan en el
> [Bootcamp Testing ADSO](https://github.com/ergrato-dev/bc-testing-adso).
> ¿Impacto? Si corriges uno, actualiza esta tabla: la demostración de esa semana cambia.

Todos se reprodujeron el 26 de septiembre de 2026 con la BD de pruebas (`db-test`) y Mailpit.

| # | Hallazgo | Quién lo encuentra | Semana |
|:-:|---|---|:-:|
| 1 | JSON mal formado responde 500 en vez de 400 | API (MockMvc) | 4 |
| 2 | Un 401 sin token responde con cuerpo vacío, fuera del contrato ProblemDetail | API (MockMvc) | 4 |
| 3 | El token de verificación se puede usar dos veces con peticiones simultáneas | Integración / API concurrente | 6 y 7 |
| 4 | El login muestra "Ha ocurrido un error inesperado" en vez del mensaje del API | Componentes con MSW / E2E | 4 y 5 |
| 5 | `LoginPage.tsx` llama a `useState` después de un `return` condicional | Lint (ESLint) / revisión | 3 |

---

## 1. JSON mal formado → 500

**Qué pasa.** `GlobalExceptionHandler` no maneja `HttpMessageNotReadableException`, así que un cuerpo inválido cae en el manejador genérico y responde **500** "Error interno del servidor". Es un error del cliente (400).

```bash
curl -s -H 'content-type: application/json' -d '{"email":' http://localhost:8080/api/v1/auth/login
```

**Por qué los tests no lo ven.** Ningún test de `AuthControllerTest` envía un cuerpo mal formado.

## 2. 401 sin cuerpo

**Qué pasa.** `GET /api/v1/users/me` sin token responde `401` con `Content-Length: 0`: el punto de entrada de seguridad usa `response.sendError(...)` y no pasa por el ProblemDetail que usa el resto del API. El frontend no recibe un `detail` que mostrar.

```bash
curl -i http://localhost:8080/api/v1/users/me
```

**Por qué los tests no lo ven.** Los tests verifican el código 401, no el cuerpo.

## 3. Token de verificación reutilizable bajo concurrencia

**Qué pasa.** `AuthService.verifyEmail` lee el token, comprueba `isUsed()` y después lo marca como usado. Dos peticiones simultáneas con el mismo token pasan la comprobación antes de que alguna lo marque: **ambas responden 200** (5 de 5 intentos). En serie, la segunda sí responde 400.

**Por qué los tests no lo ven.** Los tests hacen una petición a la vez, dentro de una transacción con rollback.

**Pista.** Bloquear la fila (`@Lock(PESSIMISTIC_WRITE)` en la consulta del repositorio) o hacer un `UPDATE … WHERE used = false` y revisar cuántas filas cambió.

## 4. El mensaje del API no llega a la persona

**Qué pasa.** Con una contraseña incorrecta, el API responde `401` con `"detail": "Credenciales inválidas"`, pero la página muestra **"Ha ocurrido un error inesperado. Intenta de nuevo."**: el frontend no lee el `detail` del ProblemDetail.

**Cómo verlo.** Inicia sesión con una contraseña incorrecta en `e2e/` o en la app. Con MSW (semana 5), un handler que responda ese mismo ProblemDetail hace fallar un test que espere "Credenciales inválidas".

## 5. Hook condicional en `LoginPage.tsx`

**Qué pasa.** Si `isAuthenticated` es verdadero, el componente hace `return <Navigate …/>` **antes** de los `useState` del formulario (líneas 26 a 42). React exige que los hooks se llamen siempre en el mismo orden; `pnpm lint` lo marca con `react-hooks/rules-of-hooks`. En los E2E no produjo un fallo visible, pero es una regla que React da por supuesta.

**Cómo verlo.** `cd fe && pnpm lint`. Es un buen caso para la semana 3: qué detecta una herramienta estática que un test de componente no.

---

## Ya corregido en este repo (no son demostraciones)

- Los tests se conectaban al contenedor de desarrollo. Ahora usan `TEST_DATABASE_URL` (servicio `db-test`); sin ella el contexto no arranca.
- Se quitó la configuración de Testcontainers que ningún test usaba y se corrigió la documentación que decía lo contrario.
- JaCoCo exige cobertura en `./mvnw verify`, el frontend mide cobertura y el CI corre los E2E con PostgreSQL y Mailpit como servicios.
