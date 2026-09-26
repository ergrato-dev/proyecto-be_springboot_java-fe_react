# Arquitectura del Sistema — NN Auth System

> Proyecto: NN Auth System
> Stack: Spring Boot 3 (Java 21) + React 19 (TypeScript) + PostgreSQL 17 + Docker
> Tests: ✅ completados — BE: 29 tests (JUnit 5 + MockMvc + PostgreSQL de pruebas) | FE: 42 tests (Vitest + Testing Library)
>
> **Referencia:** Arquitectura equivalente a [proyecto-be_fastapi-fe_react](https://github.com/ergrato-dev/proyecto-be_fastapi-fe_react)
> (FastAPI + React). Misma funcionalidad, diferente implementación de backend.

---

## Vista General del Sistema

El sistema sigue una arquitectura **Cliente–Servidor de 3 capas**, donde cada capa tiene una
responsabilidad única y se comunica solo con la capa adyacente:

```
┌──────────────────────────────────────────────────────────────────┐
│  CAPA 3 — CLIENTE (Navegador Web)                                │
│                                                                  │
│  React 19.2.4 + TypeScript 6 + TailwindCSS 4 + React Router 7   │
│  http://localhost:5173                                           │
│                                                                  │
│  ┌─────────────┐  ┌─────────────────┐  ┌──────────────────────┐ │
│  │   Pages     │  │   Components    │  │  Context / Hooks     │ │
│  │  (vistas)   │  │  (UI + Layout)  │  │  (estado de auth)    │ │
│  └──────┬──────┘  └────────┬────────┘  └──────────────────────┘ │
│         │                  │                                     │
│         └──────────────────┤                                     │
│                            ▼                                     │
│  ┌───────────────────────────────────┐                           │
│  │   api/auth.ts + api/axios.ts      │  (HTTP + JWT)             │
│  └───────────────────────────────────┘                           │
└─────────────────────────────█████████████████████────────────────┘
                               ↕ JSON / HTTPS
┌─────────────────────────────█████████████████████────────────────┐
│  CAPA 2 — SERVIDOR (Backend API)                                 │
│                                                                  │
│  Spring Boot 3 (Java 21) + Tomcat Embebido                       │
│  http://localhost:8080                                           │
│                                                                  │
│  ┌────────────┐   ┌─────────────┐   ┌────────────┐              │
│  │ Controllers│ → │  Services   │ → │   Utils    │              │
│  │ (endpoints)│   │  (lógica)   │   │(jwt/email) │              │
│  └────────────┘   └─────────────┘   └────────────┘              │
│         │                 │                 │                    │
│         ▼                 ▼                 ▼                    │
│  ┌───────────────────────────────────────────────┐               │
│  │  DTOs (Bean Validation) + Entities (JPA ORM)  │               │
│  └───────────────────────────────────────────────┘               │
└─────────────────────────────█████████████████████────────────────┘
                               ↕ SQL (HikariCP + Hibernate)
┌─────────────────────────────█████████████████████────────────────┐
│  CAPA 1 — DATOS (Base de Datos)                                  │
│                                                                  │
│  PostgreSQL 17 (Docker Container o instalación local)            │
│  localhost:5432                                                  │
│                                                                  │
│  ┌──────┐  ┌──────────────────────┐  ┌──────────────────────┐   │
│  │users │  │password_reset_tokens │  │email_verification_   │   │
│  │      │  │                      │  │tokens                │   │
│  └──────┘  └──────────────────────┘  └──────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Arquitectura del Backend (`be/`)

### Comparación con el proyecto de referencia (FastAPI)

| Concepto         | proyecto-be_fastapi-fe_react (FastAPI)        | proyecto-be_springboot_java-fe_react (Spring Boot)          |
|------------------|----------------------------------|------------------------------------------|
| Servidor         | Uvicorn (ASGI, async)           | Tomcat embebido (Servlet, sync/async)    |
| ORM              | SQLAlchemy 2.0                  | Spring Data JPA + Hibernate 6            |
| Migraciones      | Alembic                         | Flyway                                   |
| Validación       | Pydantic (schemas)              | Bean Validation — Jakarta (@Valid, @NotBlank, ...) en DTOs |
| JWT              | python-jose[cryptography]       | JJWT (io.jsonwebtoken)                   |
| Hashing          | passlib[bcrypt]                 | Spring Security BCryptPasswordEncoder    |
| Rate Limiting    | slowapi                         | Bucket4j + Spring AOP                   |
| Swagger          | FastAPI auto-genera             | SpringDoc OpenAPI 2.x                   |
| Inyección deps.  | `Depends()` de FastAPI          | `@Autowired` / `@RequiredArgsConstructor` de Spring |
| Config           | Pydantic Settings               | `@ConfigurationProperties` + `application.yml` |
| Logging          | Python logging (structlog)      | SLF4J + Logback (JSON estructurado)     |
| Testing          | pytest + httpx                  | JUnit 5 + MockMvc + BD de pruebas       |

### Estructura de capas

```
be/src/main/java/com/nn/auth/
│
├── NnAuthApplication.java      ← Punto de entrada: @SpringBootApplication
│                                  Carga el contexto de Spring, inicia Tomcat
│
├── config/                     ← CONFIGURACIÓN DE LA APLICACIÓN
│   ├── ApplicationConfig.java  ← Beans globales: BCryptPasswordEncoder, etc.
│   ├── SecurityConfig.java     ← SecurityFilterChain, CORS, JWT filter, rutas públicas
│   └── OpenApiConfig.java      ← Swagger UI condicional (deshabilitado en producción)
│
├── controller/                 ← CAPA DE PRESENTACIÓN (HTTP)
│   ├── AuthController.java     ← POST /register, /login, /refresh, /change-password,
│   │                              /forgot-password, /reset-password, /verify-email
│   └── UserController.java     ← GET /api/v1/users/me
│
├── service/                    ← CAPA DE LÓGICA DE NEGOCIO
│   └── AuthService.java        ← registerUser, loginUser, refreshAccessToken,
│                                  changePassword, requestPasswordReset,
│                                  resetPassword, verifyEmail
│
├── repository/                 ← CAPA DE DATOS (Spring Data JPA interfaces)
│   ├── UserRepository.java
│   ├── PasswordResetTokenRepository.java
│   └── EmailVerificationTokenRepository.java
│
├── entity/                     ← MODELOS ORM (mapean tablas de la BD)
│   ├── User.java               ← @Entity — tabla users
│   ├── PasswordResetToken.java ← @Entity — tabla password_reset_tokens
│   └── EmailVerificationToken.java ← @Entity — tabla email_verification_tokens
│
├── dto/                        ← DATA TRANSFER OBJECTS (validación y serialización)
│   ├── request/                ← Datos que entran (con @Valid y Bean Validation)
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   └── VerifyEmailRequest.java
│   └── response/               ← Datos que salen (nunca incluyen hashed_password)
│       ├── UserResponse.java
│       ├── TokenResponse.java
│       └── MessageResponse.java
│
├── security/                   ← MECANISMOS DE SEGURIDAD JWT
│   ├── JwtUtil.java            ← Crear y verificar tokens con JJWT
│   └── JwtAuthenticationFilter.java ← OncePerRequestFilter: extrae JWT del header
│                                        y carga el usuario en el SecurityContext
│
├── exception/                  ← MANEJO CENTRALIZADO DE ERRORES
│   └── GlobalExceptionHandler.java ← @ControllerAdvice + @ExceptionHandler
│                                       Traduce excepciones a respuestas HTTP
│
└── util/                       ← UTILIDADES TRANSVERSALES
    ├── EmailService.java       ← Envío de emails (JavaMailSender)
    └── AuditLogger.java        ← Logging estructurado JSON (SLF4J)
```

### Flujo de una petición HTTP

```
1. Cliente envía:   POST /api/v1/auth/login { email, password }
                    ↓
2. JwtAuthentication
   Filter:          Detecta este endpoint como público (no requiere JWT)
                    → Deja pasar la petición sin verificar token
                    ↓
3. Bucket4j         @RateLimiting verifica que la IP no superó 10/minuto
   Rate Limiter:    Si sí → 429 Too Many Requests
                    ↓
4. Controller:      AuthController.login() recibe el body
                    @Valid valida LoginRequest (Bean Validation)
                    Si hay errores de validación → 400/422 automático
                    ↓
5. Service:         authService.loginUser():
                    - Busca user por email (userRepository.findByEmailIgnoreCase)
                    - Verifica password: passwordEncoder.matches(plain, hashed)
                    - Verifica isEmailVerified y isActive
                    - Genera access_token y refresh_token (JwtUtil)
                    - Registra en auditLogger.logLoginSuccess(...)
                    ↓
6. Response:        Controller retorna TokenResponse(accessToken, refreshToken, tokenType)
                    Spring serializa a JSON con Jackson
```

### Seguridad en el backend

```
┌────────────────────────────────────────────────────────────┐
│  Capas de seguridad (de afuera hacia adentro)              │
│                                                            │
│  1. CORS Configuration       → Solo FRONTEND_URL           │
│  2. Security Headers Middleware → X-Frame-Options, nosniff │
│  3. Bucket4j Rate Limiter    → Por IP, por endpoint        │
│  4. Bean Validation (@Valid) → Tipos, formato, fortaleza   │
│  5. JwtAuthenticationFilter  → OncePerRequestFilter        │
│  6. Spring Security Context  → Usuario cargado del token   │
│  7. Business Logic Checks    → isActive, isEmailVerified   │
│  8. JPA/Hibernate ORM        → No raw SQL, no injection    │
│  9. BCrypt Hashing           → Contraseñas nunca en plano  │
│ 10. AuditLogger              → Trazabilidad de eventos     │
└────────────────────────────────────────────────────────────┘
```

---

## Arquitectura del Frontend (`fe/`)

El frontend apunta al backend en puerto **8080** (Spring Boot) en lugar de 8000 (FastAPI/Uvicorn).
Usa React 19.2.4, Vite 8, TypeScript 6 (strict) y TailwindCSS 4 con tokens `accent-*` amber.

### Estructura de capas

```
fe/src/
│
├── main.tsx         ← Punto de entrada: renderiza <App /> en el DOM
├── App.tsx          ← Rutas de la aplicación (React Router)
├── index.css        ← Estilos globales + imports de TailwindCSS
│
├── context/         ← ESTADO GLOBAL
│   └── AuthContext.tsx     ← Provider: usuario actual, tokens, loading
│
├── hooks/           ← LÓGICA REUTILIZABLE
│   └── useAuth.ts   ← Acceso al contexto de auth desde cualquier componente
│
├── api/             ← COMUNICACIÓN HTTP
│   ├── auth.ts      ← Funciones: register, login, refresh, changePassword, etc.
│   └── axios.ts     ← Instancia de Axios con interceptores JWT automáticos
│                       (apunta a http://localhost:8080)
│
├── components/      ← COMPONENTES REUTILIZABLES
│   ├── ProtectedRoute.tsx  ← Guarda de rutas autenticadas
│   ├── layout/
│   │   ├── AuthLayout.tsx  ← Layout para páginas de autenticación
│   │   └── Navbar.tsx      ← Barra de navegación con tema y logout
│   └── ui/
│       ├── Button.tsx      ← Botón con estado loading (aria-busy)
│       ├── InputField.tsx  ← Input con label, ícono, validación y a11y
│       └── Alert.tsx       ← Mensajes de éxito/error/info
│
├── pages/           ← VISTAS (una por ruta)
│   ├── LandingPage.tsx         ← Página pública principal
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── ChangePasswordPage.tsx
│   ├── ForgotPasswordPage.tsx
│   ├── ResetPasswordPage.tsx
│   ├── TerminosDeUsoPage.tsx
│   ├── PoliticaPrivacidadPage.tsx
│   ├── PoliticaCookiesPage.tsx
│   └── ContactPage.tsx
│
└── types/           ← TIPOS TYPESCRIPT
    └── auth.ts      ← LoginRequest, RegisterRequest, UserResponse, TokenResponse, etc.
```

### Rutas de la aplicación

```
/                 → LandingPage (pública)
/login            → LoginPage (pública)
/register         → RegisterPage (pública)
/forgot-password  → ForgotPasswordPage (pública)
/reset-password   → ResetPasswordPage (pública, requiere ?token=...)
/verify-email     → (manejado con ?token=...) → llama al backend
/dashboard        → DashboardPage (PROTEGIDA — requiere auth)
/change-password  → ChangePasswordPage (PROTEGIDA — requiere auth)
/terminos-de-uso  → TerminosDeUsoPage (pública)
/privacidad       → PoliticaPrivacidadPage (pública)
/cookies          → PoliticaCookiesPage (pública)
/contacto         → ContactPage (pública)
```

### Flujo de autenticación en el frontend

```
Arranque de la app:
1. AuthContext se monta → intenta cargar usuario desde memoria
2. Si hay access_token en memoria → llama GET /api/v1/users/me (puerto 8080)
3. Si 200 → usuario autenticado, redirige a /dashboard si está en /login
4. Si 401 → intenta refresh con POST /api/v1/auth/refresh
5. Si refresh falla → usuario va a /login

Login exitoso:
1. LoginPage → api/auth.ts::login(email, password) → TokenResponse
2. AuthContext guarda tokens en memoria (no localStorage por seguridad)
3. GET /api/v1/users/me → guarda perfil en estado
4. React Router navega a /dashboard

Acceso a ruta protegida sin token:
1. <ProtectedRoute> detecta que no hay usuario autenticado
2. Muestra spinner (role="status") mientras verifica
3. Si no autenticado → <Navigate to="/login" />

Expiración del access_token (15 min):
1. Axios interceptor detecta 401 en respuesta
2. Automáticamente llama POST /api/v1/auth/refresh
3. Si refresh exitoso → reintenta la petición original
4. Si refresh falla → logout + redirect a /login
```

---

## Flujos de Autenticación de Extremo a Extremo

### Flujo 1 — Registro y Verificación de Email

```
Usuario                  Frontend (React)            Backend (Spring Boot)     Email (Mailpit/SMTP)
   │                           │                             │                      │
   │ Rellena formulario        │                             │                      │
   │ ─────────────────────────►│                             │                      │
   │                           │ POST /api/v1/auth/register  │                      │
   │                           │────────────────────────────►│                      │
   │                           │                             │ 1. @Valid Bean Validation
   │                           │                             │ 2. Verifica email ∄  │
   │                           │                             │ 3. BCrypt.encode()   │
   │                           │                             │ 4. Crea User (isEmailVerified=false)
   │                           │                             │ 5. Crea EmailVerificationToken
   │                           │                             │ 6. emailService.sendVerification()
   │                           │                             │─────────────────────►│
   │                           │◄────────────────────────────│                      │
   │ Ve mensaje "Verifica email"│  201 UserResponse           │  Envía email         │
   │◄──────────────────────────│                             │                      │
   │                           │                             │                      │
   │ Hace clic en el enlace    │                             │                      │
   │ ─────────────────────────►│                             │                      │
   │                           │ POST /api/v1/auth/verify-email { token }           │
   │                           │────────────────────────────►│                      │
   │                           │                             │ 1. Busca token en BD │
   │                           │                             │ 2. Valida no expirado│
   │                           │                             │ 3. Marca used=true   │
   │                           │                             │ 4. isEmailVerified=true
   │                           │◄────────────────────────────│                      │
   │ "Verificado, puedes login"│  200 MessageResponse        │                      │
   │◄──────────────────────────│                             │                      │
```

### Flujo 2 — Login y Acceso a Dashboard

```
Usuario           Frontend                Backend (Spring Boot)  PostgreSQL
   │                 │                          │                    │
   │ email+password  │                          │                    │
   │────────────────►│                          │                    │
   │                 │ POST /api/v1/auth/login  │                    │
   │                 │─────────────────────────►│                    │
   │                 │                          │ JPA: SELECT FROM users
   │                 │                          │──────────────────►│
   │                 │                          │◄──────────────────│
   │                 │                          │ passwordEncoder.matches()
   │                 │                          │ jwtUtil.generateAccessToken()
   │                 │                          │ jwtUtil.generateRefreshToken()
   │                 │◄─────────────────────────│                    │
   │                 │ 200 TokenResponse        │                    │
   │                 │ (accessToken+refreshToken)│                   │
   │                 │                          │                    │
   │                 │ GET /api/v1/users/me     │                    │
   │                 │ Authorization: Bearer ..  │                    │
   │                 │─────────────────────────►│                    │
   │                 │                          │ JwtFilter verifica token
   │                 │                          │ Carga User en SecurityContext
   │                 │                          │ JPA: SELECT user by id ──►│
   │                 │◄─────────────────────────│                    │
   │ Dashboard carga │ 200 UserResponse         │                    │
   │◄────────────────│                          │                    │
```

### Flujo 3 — Recuperación de Contraseña

```
Usuario           Frontend                Backend (Spring Boot)  PostgreSQL    Email
   │                 │                          │                    │           │
   │ Ingresa email   │                          │                    │           │
   │────────────────►│ POST /auth/forgot-password                    │           │
   │                 │─────────────────────────►│                    │           │
   │                 │                          │ JPA: busca user ──►│           │
   │                 │                          │ (si no existe, respuesta genérica)
   │                 │                          │ Crea PasswordResetToken ────────────────►│
   │ "Revisa tu email"│◄─────────────────────────│                   │  Envía mail│
   │◄────────────────│ 200 (siempre igual)      │                    │           │
   │                 │                          │                    │           │
   │ Clic en enlace  │                          │                    │           │
   │────────────────►│ POST /auth/reset-password { token, newPassword }
   │                 │─────────────────────────►│                    │           │
   │                 │                          │ Valida token ─────►│           │
   │                 │                          │ BCrypt.encode(newPassword)      │
   │                 │                          │ JPA: UPDATE users password      │
   │                 │                          │ JPA: UPDATE token used=true     │
   │ "Contraseña restablecida"◄─────────────────│                    │           │
   │◄────────────────│ 200 MessageResponse      │                    │           │
```

---

## Decisiones Técnicas Clave

### ¿Por qué Spring Boot y no Quarkus o Micronaut?

| Criterio               | Spring Boot 3 | Quarkus       | Micronaut     |
|------------------------|---------------|---------------|---------------|
| Madurez y ecosistema   | ✅ Muy maduro  | ⚠️ Relativo   | ⚠️ Relativo   |
| Comunidad educativa    | ✅ Enorme      | ❌ Pequeña    | ❌ Pequeña    |
| Documentación          | ✅ Extensa     | ✅ Buena      | ✅ Buena      |
| Curva de aprendizaje   | ⚠️ Moderada   | Alta          | Alta          |
| JDK 21 (Virtual Threads)| ✅ Soportado  | ✅ Soportado  | ✅ Soportado  |
| Spring Security (BCrypt)| ✅ Incluido   | Diferente     | Diferente     |

Spring Boot fue elegido por su ecosistema maduro, comunidad enorme y ser el estándar
de facto en el mundo empresarial Java.

### ¿Por qué Maven y no Gradle?

Maven usa un formato XML declarativo (`pom.xml`) que es más explícito y predecible para
proyectos educativos. El **Maven Wrapper** (`./mvnw`) garantiza que todos usen exactamente
la misma versión de Maven sin necesidad de instalación global.

### ¿Por qué Flyway y no Liquibase?

| Criterio         | Flyway         | Liquibase          |
|------------------|----------------|--------------------|
| Simplicidad      | ✅ Scripts SQL | ⚠️ XML/YAML/JSON   |
| Legibilidad      | ✅ SQL puro    | ❌ Abstracción extra |
| Curva didáctica  | ✅ Baja        | ❌ Media-alta       |
| Integración SB   | ✅ Automática  | ✅ Automática      |

Flyway usa scripts SQL numerados (`V1__descripcion.sql`) que se ejecutan automáticamente
al iniciar la aplicación. El SQL es directamente legible y enseñable.

### ¿Por qué JWT stateless y no sesiones en servidor?

| Criterio           | JWT (stateless)           | Sesiones en servidor         |
|--------------------|---------------------------|------------------------------|
| Escalabilidad      | ✅ Horizontal fácil       | ❌ Requiere sesión compartida / Redis |
| Estado en servidor | ✅ Ninguno                | ❌ Almacenamiento de sesiones  |
| Revocación         | ❌ Requiere blacklist     | ✅ Borrar sesión               |
| Apropiado para SPA | ✅ Diseñado para esto     | ⚠️ Problemas con CORS         |

Para este proyecto educativo, la arquitectura stateless con JWT es la más apropiada —
permite escalar horizontalmente sin coordinación entre servidores.

### ¿Por qué Bean Validation y no validación manual?

Bean Validation (Jakarta) permite declarar las reglas de validación directamente en los
DTOs usando anotaciones (`@NotBlank`, `@Email`, `@Size`, `@Pattern`). Esto es el
equivalente exacto de Pydantic en Python: **declarativo, auto-documentado y reutilizable**.
Con `@Valid` en el Controller, Spring valida automáticamente antes de llegar al Service.

```java
// ✅ Validación declarativa — equivalente a Pydantic en FastAPI
public record RegisterRequest(
    @NotBlank @Email                        String email,
    @NotBlank                               String fullName,
    @NotBlank @Size(min = 8)
    @Pattern(regexp = ".*[A-Z].*")
    @Pattern(regexp = ".*[a-z].*")
    @Pattern(regexp = ".*\\d.*")            String password
) {}

// En el Controller — @Valid activa la validación automáticamente
public UserResponse register(@Valid @RequestBody RegisterRequest request) { ... }
// Si falla → MethodArgumentNotValidException → GlobalExceptionHandler → 400
```

### ¿Por qué JJWT y no Spring Security OAuth2?

Spring Security OAuth2 es un framework completo para flujos OAuth/OIDC (Google Login, etc.).
Para este proyecto educativo con JWT manual, JJWT (io.jsonwebtoken) es la librería correcta:
explícita, liviana y directamente enseñable. **Aprender JJWT enseña cómo funcionan los JWT**;
OAuth2 lo abstrae detrás de muchas capas.

### ¿Por qué React + Vite y no Next.js?

Este es un proyecto de SPA (Single Page Application) — toda la navegación ocurre en el cliente.
Next.js agrega SSR (Server-Side Rendering) que no es necesario para una app de auth. Vite es
más rápido en desarrollo y la configuración es más simple para aprendizaje.

---

## Configuración de Entornos

Spring Boot usa perfiles (`spring.profiles.active`) para diferenciar ambientes:

| Variable         | `development`                        | `production`                                    |
|------------------|--------------------------------------|--------------------------------------------------|
| `ENVIRONMENT`    | development                          | production                                       |
| Swagger UI       | ✅ `/swagger-ui.html`               | ❌ Deshabilitado (404)                           |
| `DB_HOST`        | localhost (o container Docker)       | Servidor de BD en cloud                          |
| `FRONTEND_URL`   | http://localhost:5173               | https://tu-dominio.com                           |
| `JWT_SECRET`     | Clave de desarrollo (≥32 chars)      | Clave aleatoria larga (`openssl rand -hex 64`)   |
| Email            | Mailpit (SMTP local) o MockSender   | SMTP real (Resend, SendGrid, etc.)               |
| Puerto           | 8080 (default)                       | Variable según despliegue                        |

> Ver `be/.env.example` para la lista completa de variables de entorno.

---

## Operación Sin Docker

Una de las ventajas de Spring Boot sobre FastAPI/Python es que el backend compila a
un **JAR autónomo** que puede ejecutarse en cualquier máquina con JDK 21, sin necesidad
de entorno virtual ni gestores de dependencias en tiempo de ejecución.

### Opciones de ejecución sin Docker

```bash
# Opción 1 — Maven Wrapper (desarrollo, con hot reload)
cd be
./mvnw spring-boot:run -Dspring-boot.run.profiles=noDocker

# Opción 2 — JAR compilado (más parecido a producción)
./mvnw package -DskipTests
java -jar target/nn-auth-system-*.jar --spring.profiles.active=noDocker

# Opción 3 — Variables de entorno inline (útil en CI/CD)
DB_HOST=localhost DB_PORT=5432 ./mvnw spring-boot:run
```

El perfil `noDocker` configura un **MockMailSender** que en lugar de enviar emails reales,
los imprime en la consola con el formato:

```
[MOCK EMAIL] To: usuario@ejemplo.com
[MOCK EMAIL] Subject: Verifica tu email — NN Auth System
[MOCK EMAIL] Body:
  Enlace de verificación: http://localhost:5173/verify-email?token=abc123...
```

---

## Sistema de Temas Diferenciales — Amber (Spring Boot Java)

Este proyecto forma parte de una **serie educativa** donde cada variante de backend tiene
una identidad visual única definida por su color de acento. Esto permite distinguir visualmente
qué stack está corriendo al mayor de un vistazo.

### Tabla de identidades por stack

| Stack                  | Proyecto               | Acento Tailwind | Hex referencia |
|------------------------|------------------------|-----------------|----------------|
| FastAPI (Python)       | `proyecto-be_fastapi-fe_react`       | `emerald`       | `#059669`      |
| Express.js (Node)      | `proyecto-beex-fe`     | `blue`          | `#2563eb`      |
| Next.js (fullstack)    | `proyecto-be-fe-next`  | `violet`        | `#7c3aed`      |
| **Spring Boot (Java)** | **`proyecto-be_springboot_java-fe_react`** | **`amber`**     | **`#d97706`**  |
| Spring Boot (Kotlin)   | `proyecto-besbk-fe`    | `fuchsia`       | `#c026d3`      |
| Go REST API            | `proyecto-bego-fe`     | `cyan`          | `#0891b2`      |

### Implementación del token `accent-*`

El color de acento se define en **11 líneas** dentro de `fe/src/index.css` usando
el sistema `@theme inline` de TailwindCSS v4. Los componentes **nunca** usan colores
hardcodeados (`amber-600`) — siempre usan el token neutro (`accent-600`):

```css
/* fe/src/index.css — Spring Boot Java → amber */
@theme inline {
  --color-accent-50:  var(--color-amber-50);
  --color-accent-100: var(--color-amber-100);
  --color-accent-200: var(--color-amber-200);
  --color-accent-300: var(--color-amber-300);
  --color-accent-400: var(--color-amber-400);
  --color-accent-500: var(--color-amber-500);
  --color-accent-600: var(--color-amber-600);  /* botones primarios */
  --color-accent-700: var(--color-amber-700);  /* hover de botones */
  --color-accent-800: var(--color-amber-800);
  --color-accent-900: var(--color-amber-900);
  --color-accent-950: var(--color-amber-950);
}
```

### Colores semánticos (NO cambian entre proyectos)

| Semántica | Color Tailwind | Cuándo usar                    |
|-----------|---------------|--------------------------------|
| Éxito     | `green-*`     | Operación completada           |
| Error     | `red-*`       | Validación fallida, API error  |
| Advertencia | `yellow-*`  | Avisos, tokens próximos a expirar |
| Info      | `blue-*`      | Mensajes informativos neutros  |

### Colores del logo SVG — Amber

El componente `Logo.tsx` usa colores SVG hardcodeados que deben coincidir con el acento:

| Elemento           | Atributo SVG           | Color           |
|--------------------|------------------------|-----------------|
| Borde del badge    | `stroke="#d97706"`     | amber-600       |
| Trazos de la letra | `stroke="#fbbf24"`     | amber-400       |

> Ver `docs/referencia-tecnica/design-system.md` para instrucciones completas de clonación,
> verificación visual y convenciones de uso del sistema de tokens.
