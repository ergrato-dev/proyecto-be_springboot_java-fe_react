# Referencia de Endpoints — NN Auth System (Spring Boot)

> Stack: Spring Boot 3.2+ (Java 21) | Puerto: **8080** | Swagger UI: `/swagger-ui.html`
> Referencia funcional: [proyecto-be_fastapi-fe_react](https://github.com/ergrato-dev/proyecto-be_fastapi-fe_react) (FastAPI, puerto 8000)

---

## Índice

1. [Información Base](#1-información-base)
2. [Resumen de Endpoints](#2-resumen-de-endpoints)
3. [Códigos de Estado HTTP](#3-códigos-de-estado-http)
4. [Detalle de Endpoints — Autenticación](#4-detalle-de-endpoints--autenticación)
   - [POST /api/v1/auth/register](#41-post-apiv1authregister)
   - [POST /api/v1/auth/login](#42-post-apiv1authlogin)
   - [POST /api/v1/auth/refresh](#43-post-apiv1authrefresh)
   - [POST /api/v1/auth/change-password](#44-post-apiv1authchange-password)
   - [POST /api/v1/auth/forgot-password](#45-post-apiv1authforgot-password)
   - [POST /api/v1/auth/reset-password](#46-post-apiv1authreset-password)
   - [POST /api/v1/auth/verify-email](#47-post-apiv1authverify-email)
5. [Detalle de Endpoints — Usuario](#5-detalle-de-endpoints--usuario)
   - [GET /api/v1/users/me](#51-get-apiv1usersme)
6. [Rate Limiting — Bucket4j](#6-rate-limiting--bucket4j)
7. [Autenticación JWT — Flujo de Headers](#7-autenticación-jwt--flujo-de-headers)
8. [Manejo de Errores Globales](#8-manejo-de-errores-globales)
9. [Swagger UI](#9-swagger-ui)
10. [Diferencias vs Referencia FastAPI](#10-diferencias-vs-referencia-fastapi)

---

## 1. Información Base

| Propiedad             | Valor                                     |
|-----------------------|-------------------------------------------|
| Base URL (desarrollo) | `http://localhost:8080`                   |
| Prefijo API           | `/api/v1/`                                |
| Formato de datos      | JSON (application/json)                   |
| Autenticación         | JWT — Bearer Token en header              |
| Header de auth        | `Authorization: Bearer <access_token>`    |
| Swagger UI            | `http://localhost:8080/swagger-ui.html`   |
| OpenAPI spec JSON     | `http://localhost:8080/v3/api-docs`       |

> **Nota importante:** El backend corre en Tomcat embebido en el puerto **8080** (Spring Boot por defecto).
> La referencia FastAPI usaba el puerto **8000** (Uvicorn). El frontend apunta a `VITE_API_URL=http://localhost:8080`.

---

## 2. Resumen de Endpoints

| Método | Ruta                              | Descripción                              | Auth JWT | Rate Limit  |
|--------|-----------------------------------|------------------------------------------|----------|-------------|
| POST   | `/api/v1/auth/register`           | Registrar nuevo usuario                  | No       | 5 req/min   |
| POST   | `/api/v1/auth/login`              | Iniciar sesión, obtener tokens           | No       | 10 req/min  |
| POST   | `/api/v1/auth/refresh`            | Renovar access token con refresh token   | No (*)   | 20 req/min  |
| POST   | `/api/v1/auth/change-password`    | Cambiar contraseña (usuario autenticado) | Sí       | 5 req/min   |
| POST   | `/api/v1/auth/forgot-password`    | Solicitar email de recuperación          | No       | 3 req/min   |
| POST   | `/api/v1/auth/reset-password`     | Restablecer contraseña con token         | No (*)   | 5 req/min   |
| POST   | `/api/v1/auth/verify-email`       | Verificar dirección de email             | No (*)   | 5 req/min   |
| GET    | `/api/v1/users/me`                | Obtener perfil del usuario autenticado   | Sí       | 60 req/min  |

(*) No requiere Access Token JWT estándar, pero sí un token específico (refresh, reset o verificación).

---

## 3. Códigos de Estado HTTP

| Código | Nombre                | Cuándo se usa en este sistema                                     |
|--------|-----------------------|-------------------------------------------------------------------|
| 200    | OK                    | Solicitud exitosa (login, refresh, me, cambio de contraseña, etc.)|
| 201    | Created               | Recurso creado exitosamente (registro de usuario)                 |
| 400    | Bad Request           | Datos de entrada inválidos (bean validation fallida)              |
| 401    | Unauthorized          | Token inválido, expirado, o credenciales incorrectas              |
| 403    | Forbidden             | Usuario autenticado pero sin permisos (ej: email no verificado)   |
| 404    | Not Found             | Recurso no encontrado                                             |
| 422    | Unprocessable Entity  | Error de validación semántica (contraseña débil, email duplicado) |
| 429    | Too Many Requests     | Rate limit excedido (Bucket4j)                                    |
| 500    | Internal Server Error | Error inesperado del servidor                                     |

> **Diferencia vs referencia FastAPI:** FastAPI devolvía 422 para errores de validación Pydantic de forma automática.
> Spring Boot devuelve 400 para errores de `@Valid` Bean Validation. El `GlobalExceptionHandler` puede estandarizar esto.

---

## 4. Detalle de Endpoints — Autenticación

---

### 4.1 POST `/api/v1/auth/register`

**Descripción:** Registra un nuevo usuario en el sistema. El email debe ser único. La contraseña se almacena como hash BCrypt. Se envía un email de verificación al usuario.

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.registerUser(request);
}
```

**DTO de Request (`RegisterRequest.java`):**
```java
public record RegisterRequest(
    @NotBlank @Email
    String email,

    @NotBlank
    @Size(min = 2, max = 255)
    String fullName,

    @NotBlank
    @Size(min = 8, message = "Mínimo 8 caracteres")
    @Pattern(regexp = ".*[A-Z].*", message = "Debe tener al menos una mayúscula")
    @Pattern(regexp = ".*[a-z].*", message = "Debe tener al menos una minúscula")
    @Pattern(regexp = ".*\\d.*",   message = "Debe tener al menos un número")
    String password
) {}
```

**Body de la petición:**
```json
{
  "email": "usuario@ejemplo.com",
  "fullName": "María García",
  "password": "MiContraseña123"
}
```

**Rate Limit:** 5 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 201 Created:**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "usuario@ejemplo.com",
  "fullName": "María García",
  "isActive": true,
  "isEmailVerified": false,
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Errores posibles:**

| Código | Condición                                    | Mensaje                                    |
|--------|----------------------------------------------|--------------------------------------------|
| 400    | Campo `email` vacío o ausente                | `"El email es requerido"`                  |
| 400    | Formato de email inválido                    | `"Formato de email inválido"`              |
| 400    | Contraseña sin mayúscula, minúscula o número | Mensaje de `@Pattern` correspondiente      |
| 400    | Contraseña menor a 8 caracteres              | `"Mínimo 8 caracteres"`                    |
| 422    | Email ya registrado                          | `"El email ya está registrado"`            |
| 429    | Rate limit excedido                          | `"Demasiadas solicitudes. Intenta más tarde"` |

**Nota de seguridad:** La contraseña se hashea inmediatamente con BCrypt antes de almacenarla. Nunca se almacena ni se loggea la contraseña en texto plano.

---

### 4.2 POST `/api/v1/auth/login`

**Descripción:** Autentica un usuario con email y contraseña. Verifica que el email esté verificado y la cuenta esté activa. Genera un par de tokens JWT (access + refresh).

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/login")
public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
}
```

**DTO de Request (`LoginRequest.java`):**
```java
public record LoginRequest(
    @NotBlank @Email
    String email,

    @NotBlank
    String password
) {}
```

**Body de la petición:**
```json
{
  "email": "usuario@ejemplo.com",
  "password": "MiContraseña123"
}
```

**Rate Limit:** 10 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 200 OK:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

**Estructura del payload JWT (access token):**
```json
{
  "sub": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "usuario@ejemplo.com",
  "type": "access",
  "iat": 1705312200,
  "exp": 1705313100
}
```

> `sub` = user ID (UUID), `iat` = issued at, `exp` = expiration (access: +15 min, refresh: +7 días)

**Errores posibles:**

| Código | Condición                                | Mensaje                                         |
|--------|------------------------------------------|-------------------------------------------------|
| 400    | Campo vacío o ausente                    | Mensaje de Bean Validation correspondiente      |
| 401    | Email no existe o contraseña incorrecta  | `"Credenciales inválidas"` (mensaje genérico)   |
| 403    | Email no verificado                      | `"Debes verificar tu email antes de ingresar"`  |
| 403    | Cuenta desactivada                       | `"Tu cuenta está desactivada"`                  |
| 429    | Rate limit excedido                      | `"Demasiadas solicitudes. Intenta más tarde"`   |

**Nota de seguridad:** El mensaje de error para credenciales inválidas es SIEMPRE el mismo, independientemente de si el email no existe o si la contraseña es incorrecta. Esto previene enumeración de usuarios (user enumeration attack).

---

### 4.3 POST `/api/v1/auth/refresh`

**Descripción:** Renueva el access token usando el refresh token. Implementa rotación de tokens (el refresh token antiguo se invalida y se emite uno nuevo).

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/refresh")
public TokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refreshToken(request.refreshToken());
}
```

**Body de la petición:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Rate Limit:** 20 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 200 OK:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...<nuevo>",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...<nuevo>",
  "tokenType": "Bearer"
}
```

**Errores posibles:**

| Código | Condición                            | Mensaje                              |
|--------|--------------------------------------|--------------------------------------|
| 401    | Refresh token inválido               | `"Token inválido"`                   |
| 401    | Refresh token expirado (> 7 días)    | `"Token expirado"`                   |
| 401    | Campo `refreshToken` vacío           | `"El refresh token es requerido"`    |

---

### 4.4 POST `/api/v1/auth/change-password`

**Descripción:** Cambia la contraseña del usuario autenticado. Requiere la contraseña actual para verificar la identidad.

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/change-password")
public MessageResponse changePassword(
    @Valid @RequestBody ChangePasswordRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    return authService.changePassword(request, userDetails.getUsername());
}
```

> `@AuthenticationPrincipal` extrae el usuario del SecurityContext, que fue poblado por `JwtAuthenticationFilter`.
> Spring Security equivale al `Depends(get_current_user)` de FastAPI.

**Header requerido:**
```
Authorization: Bearer <access_token>
```

**DTO de Request (`ChangePasswordRequest.java`):**
```java
public record ChangePasswordRequest(
    @NotBlank
    String currentPassword,

    @NotBlank
    @Size(min = 8)
    @Pattern(regexp = ".*[A-Z].*")
    @Pattern(regexp = ".*[a-z].*")
    @Pattern(regexp = ".*\\d.*")
    String newPassword
) {}
```

**Body de la petición:**
```json
{
  "currentPassword": "MiContraseñaAnterior123",
  "newPassword": "NuevaContraseña456"
}
```

**Rate Limit:** 5 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 200 OK:**
```json
{
  "message": "Contraseña actualizada exitosamente"
}
```

**Errores posibles:**

| Código | Condición                                | Mensaje                                    |
|--------|------------------------------------------|--------------------------------------------|
| 400    | Campo vacío o contraseña nueva débil     | Mensaje de Bean Validation correspondiente |
| 401    | Access token ausente o inválido          | `"No autenticado"`                         |
| 401    | Contraseña actual incorrecta             | `"La contraseña actual es incorrecta"`     |

---

### 4.5 POST `/api/v1/auth/forgot-password`

**Descripción:** Solicita el envío de un email con enlace para restablecer la contraseña. La respuesta es SIEMPRE la misma, independientemente de si el email existe o no, para no revelar qué emails están registrados.

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/forgot-password")
public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request.email());
    // La respuesta es siempre idéntica — no revela si el email existe
    return new MessageResponse("Si el email está registrado, recibirás instrucciones en breve");
}
```

**Body de la petición:**
```json
{
  "email": "usuario@ejemplo.com"
}
```

**Rate Limit:** 3 peticiones / minuto por IP (Bucket4j) — el más estricto para prevenir abuso

**Respuesta exitosa — 200 OK (siempre igual):**
```json
{
  "message": "Si el email está registrado, recibirás instrucciones en breve"
}
```

**Errores posibles:**

| Código | Condición                   | Mensaje                         |
|--------|-----------------------------|---------------------------------|
| 400    | Email vacío o formato malo  | `"Formato de email inválido"`   |
| 429    | Rate limit excedido         | `"Demasiadas solicitudes..."`   |

**Nota de seguridad:** Si el email NO existe en la base de datos, el endpoint NO devuelve ningún error. Siempre devuelve el mismo mensaje 200. Esto es por diseño para prevenir el ataque de enumeración de usuarios (saber qué emails están registrados bombeando el endpoint).

**Enlace generado en el email:**
```
{FRONTEND_URL}/reset-password?token={UUID_TOKEN}
```
El token expira en **1 hora**.

---

### 4.6 POST `/api/v1/auth/reset-password`

**Descripción:** Restablece la contraseña usando el token recibido por email. El token es de un solo uso y expira en 1 hora.

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/reset-password")
public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return authService.resetPassword(request);
}
```

**DTO de Request (`ResetPasswordRequest.java`):**
```java
public record ResetPasswordRequest(
    @NotBlank
    String token,

    @NotBlank
    @Size(min = 8)
    @Pattern(regexp = ".*[A-Z].*")
    @Pattern(regexp = ".*[a-z].*")
    @Pattern(regexp = ".*\\d.*")
    String newPassword
) {}
```

**Body de la petición:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NuevaContraseñaSegura789"
}
```

**Rate Limit:** 5 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 200 OK:**
```json
{
  "message": "Contraseña restablecida exitosamente"
}
```

**Errores posibles:**

| Código | Condición                            | Mensaje                                    |
|--------|--------------------------------------|--------------------------------------------|
| 400    | Campo vacío o contraseña débil       | Mensaje de Bean Validation correspondiente |
| 400    | Token no encontrado en la BD         | `"Token inválido"`                         |
| 400    | Token expirado (> 1 hora)            | `"El token ha expirado"`                   |
| 400    | Token ya utilizado (`used = true`)   | `"El token ya fue utilizado"`              |

---

### 4.7 POST `/api/v1/auth/verify-email`

**Descripción:** Marca el email del usuario como verificado usando el token enviado al registrarse. El token expira en 24 horas.

**Implementación Java:**
```java
// AuthController.java
@PostMapping("/verify-email")
public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    return authService.verifyEmail(request.token());
}
```

**Body de la petición:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Rate Limit:** 5 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 200 OK:**
```json
{
  "message": "Email verificado exitosamente. Ya puedes iniciar sesión."
}
```

**Errores posibles:**

| Código | Condición                               | Mensaje                          |
|--------|-----------------------------------------|----------------------------------|
| 400    | Token vacío o ausente                   | `"El token es requerido"`        |
| 400    | Token no encontrado en la BD            | `"Token inválido"`               |
| 400    | Token expirado (> 24 horas)             | `"El token ha expirado"`         |
| 400    | Token ya utilizado                      | `"El token ya fue utilizado"`    |

**Enlace típico en el email de verificación:**
```
{FRONTEND_URL}/verify-email?token={UUID_TOKEN}
```
El frontend extrae el token del query param y hace POST a este endpoint automáticamente.

---

## 5. Detalle de Endpoints — Usuario

---

### 5.1 GET `/api/v1/users/me`

**Descripción:** Devuelve el perfil del usuario actualmente autenticado. No acepta body.

**Implementación Java:**
```java
// UserController.java
@GetMapping("/me")
public UserResponse getMe(@AuthenticationPrincipal UserDetails userDetails) {
    return userService.findByEmail(userDetails.getUsername());
}
```

**Header requerido:**
```
Authorization: Bearer <access_token>
```

**Rate Limit:** 60 peticiones / minuto por IP (Bucket4j)

**Respuesta exitosa — 200 OK:**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "usuario@ejemplo.com",
  "fullName": "María García",
  "isActive": true,
  "isEmailVerified": true,
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Errores posibles:**

| Código | Condición                          | Mensaje                  |
|--------|------------------------------------|--------------------------|
| 401    | Access token ausente               | `"No autenticado"`       |
| 401    | Access token inválido o expirado   | `"Token inválido"`       |

---

## 6. Rate Limiting — Bucket4j

### 6.1 ¿Qué es Bucket4j?

Bucket4j es la librería Java que reemplaza a `slowapi` del proyecto de referencia FastAPI. Implementa el algoritmo **token bucket** para controlar la tasa de peticiones por IP.

### 6.2 Configuración en Spring Boot

```yaml
# application.yml
bucket4j:
  enabled: true
  filters:
    - cache-name: rate-limit-buckets
      url: /api/v1/auth/register
      rate-limits:
        - bandwidths:
            - capacity: 5
              time: 1
              unit: minutes
    - url: /api/v1/auth/login
      rate-limits:
        - bandwidths:
            - capacity: 10
              time: 1
              unit: minutes
```

### 6.3 Respuesta cuando se excede el límite — 429 Too Many Requests

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Has excedido el límite de solicitudes. Intenta de nuevo en 60 segundos.",
  "path": "/api/v1/auth/login"
}
```

**Header adicional en la respuesta:**
```
X-Rate-Limit-Retry-After-Seconds: 45
```

---

## 7. Autenticación JWT — Flujo de Headers

### 7.1 Peticiones que requieren autenticación

```http
GET /api/v1/users/me HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

### 7.2 Cómo funciona `JwtAuthenticationFilter`

```
Petición HTTP
    ↓
JwtAuthenticationFilter extends OncePerRequestFilter
    ↓
1. Leer header: Authorization: Bearer <token>
2. Verificar firma HMAC-SHA256 con JWT_SECRET (JwtUtil.java)
3. Verificar no expirado
4. Extraer userId/email del payload
5. Crear UsernamePasswordAuthenticationToken
6. Poblar SecurityContextHolder
    ↓
Controller procesa la petición
    ↓
@AuthenticationPrincipal → UserDetails del token
```

### 7.3 Renovación de tokens (flujo recomendado en el frontend)

```
1. Hacer petición normal con Access Token (15 min)
2. Si respuesta es 401 (token expirado):
   a. Hacer POST /api/v1/auth/refresh con Refresh Token (7 días)
   b. Si respuesta es 200: guardar nuevos tokens, reintentar petición original
   c. Si respuesta es 401: redirigir al usuario a /login
3. Si Refresh Token también expira: redirigir a /login
```

**Implementación en el frontend (Axios interceptor):**
```typescript
// api/axios.ts
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      const newTokens = await refreshToken(getRefreshToken());
      setTokens(newTokens);
      error.config.headers.Authorization = `Bearer ${newTokens.accessToken}`;
      return axiosInstance(error.config);
    }
    return Promise.reject(error);
  }
);
```

---

## 8. Manejo de Errores Globales

### 8.1 `GlobalExceptionHandler.java`

Spring Boot usa `@ControllerAdvice` para centralizar el manejo de errores. Esto equivale a los HTTP exception handlers de FastAPI.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja errores de @Valid (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        // Agrupa todos los errores de validación en un solo response
    }
}
```

### 8.2 Estructura estándar de error

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación",
  "details": {
    "email": "Formato de email inválido",
    "password": "Debe contener al menos una mayúscula"
  },
  "path": "/api/v1/auth/register"
}
```

### 8.3 Error de validación múltiple (Bean Validation)

Cuando múltiples campos fallan la validación `@Valid`, el response agrupa todos los errores:

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "details": {
    "email": "Formato de email inválido",
    "password": "Mínimo 8 caracteres",
    "fullName": "El nombre es requerido"
  }
}
```

> **Diferencia vs FastAPI:** FastAPI devolvía 422 con un array `detail` de objetos con `loc`, `msg`, `type`.
> Spring Boot con nuestro `GlobalExceptionHandler` devuelve 400 con un objeto `details` más legible.

---

## 9. Swagger UI

### 9.1 Acceso

| Ambiente    | URL                                         | Disponible |
|-------------|---------------------------------------------|------------|
| Desarrollo  | `http://localhost:8080/swagger-ui.html`     | ✅ Sí      |
| Producción  | `http://tu-dominio.com/swagger-ui.html`     | ❌ No (404)|

### 9.2 Configuración condicional (`OpenApiConfig.java`)

```java
@Configuration
@ConditionalOnProperty(name = "app.swagger.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NN Auth System API")
                .version("1.0")
                .description("Sistema de autenticación para aplicaciones NN")
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT"))
            );
    }
}
```

```yaml
# application.yml (desarrollo)
app:
  swagger:
    enabled: true

# application-production.yml (producción — deshabilitar Swagger)
app:
  swagger:
    enabled: false
```

### 9.3 Prueba de un endpoint desde Swagger UI

1. Abrir `http://localhost:8080/swagger-ui.html`
2. Expandir `POST /api/v1/auth/login`
3. Click en **Try it out**
4. Ingresar el body JSON con email y password
5. Click en **Execute**
6. Copiar el `accessToken` de la respuesta
7. Click en **Authorize** (candado en la parte superior)
8. Ingresar: `Bearer <accessToken>` en el campo
9. Ahora puedes ejecutar endpoints protegidos como `GET /api/v1/users/me`

---

## 10. Diferencias vs Referencia FastAPI

Esta tabla documenta las diferencias de comportamiento o implementación respecto al proyecto de referencia `proyecto-be_fastapi-fe_react`.

| Aspecto                       | FastAPI (proyecto-be_fastapi-fe_react)              | Spring Boot (este proyecto)                  |
|-------------------------------|---------------------------------------|----------------------------------------------|
| Puerto del backend            | 8000 (Uvicorn)                        | 8080 (Tomcat embebido)                       |
| Swagger UI                    | `/docs` (automático)                  | `/swagger-ui.html` (SpringDoc OpenAPI)      |
| OpenAPI JSON                  | `/openapi.json`                       | `/v3/api-docs`                               |
| Validación de entrada         | Pydantic (422 automático)             | Bean Validation `@Valid` (400 por defecto)   |
| Autenticación                 | `Depends(get_current_user)`           | `JwtAuthenticationFilter` + `@AuthPrincipal`|
| Rate Limiting                 | `slowapi` (decorador)                 | `Bucket4j` (configuración YAML o filtro)    |
| Manejo de errores             | `HTTPException` (Python)              | `@ControllerAdvice` + excepciones Java       |
| Migraciones BD                | Alembic (`alembic upgrade head`)      | Flyway (auto en startup, `V1__*.sql`)        |
| Front VITE_API_URL            | `http://localhost:8000`               | `http://localhost:8080`                      |
| Testing                       | pytest + httpx + TestClient           | JUnit 5 + MockMvc + PostgreSQL de pruebas    |
| Contenedor de tests           | pytest (sin Docker)                   | Servicio db-test (docker compose, 5433)      |

### Compatibilidad de contratos API

La funcionalidad es **idéntica** entre ambas implementaciones. La estructura del JSON de request/response es la misma. El frontend React puede conectarse a cualquiera de los dos backends sin modificaciones (solo cambia `VITE_API_URL`).

---

*Documentación generada para el proyecto NN Auth System. Stack: Spring Boot 3.2+ (Java 21). Ver [architecture.md](architecture.md) para la arquitectura del sistema.*
