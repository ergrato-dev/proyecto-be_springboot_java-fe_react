# 🎓 Instrucciones del Proyecto — NN Auth System (Spring Boot)

> Proyecto: NN Auth System
> Stack: Spring Boot 3 (Java 21) + React (TypeScript) + PostgreSQL 17 + Docker
> Referencia funcional: [proyecto-be_fastapi-fe_react](https://github.com/ergrato-dev/proyecto-be_fastapi-fe_react) (FastAPI + React)

---

## 1. Identidad del Proyecto

| Campo        | Valor                                                                                                                   |
| ------------ | ----------------------------------------------------------------------------------------------------------------------- |
| Nombre       | NN Auth System                                                                                                          |
| Tipo         | Proyecto educativo — SENA                                                                                               |
| Propósito    | Sistema de autenticación completo (registro, login, cambio y recuperación de contraseña) para una empresa genérica "NN" |
| Enfoque      | Aprendizaje guiado: cada línea de código y documentación debe enseñar                                                   |
| Stack BE     | Java 21 + Spring Boot 3.2+ (diferente al repo de referencia que usa Python/FastAPI)                                     |
| Fecha inicio | Marzo 2026                                                                                                              |

---

## 2. Stack Tecnológico

### 2.1 Backend (`be/`)

| Tecnología                     | Versión    | Rol                                                   |
| ------------------------------ | ---------- | ----------------------------------------------------- |
| Java                           | 21 (LTS)   | Lenguaje principal del backend                        |
| Spring Boot                    | 3.2+       | Framework web, auto-configuración, embedded Tomcat    |
| Spring Data JPA                | (incluido) | ORM para interactuar con la base de datos             |
| Spring Security                | (incluido) | Solo para BCryptPasswordEncoder y SecurityFilterChain |
| Hibernate                      | 6.x        | Implementación JPA                                    |
| Flyway                         | 9+         | Migraciones de base de datos versionadas              |
| Bean Validation (Jakarta)      | 3.x        | Validación de datos en DTOs (equivalente a Pydantic)  |
| JJWT (io.jsonwebtoken)         | 0.12+      | Creación y verificación de tokens JWT                 |
| Bucket4j                       | 8+         | Rate limiting por IP en endpoints sensibles           |
| SpringDoc OpenAPI              | 2.x        | Swagger UI auto-generado en `/swagger-ui.html`        |
| JavaMailSender (Spring Mail)   | (incluido) | Envío de emails transaccionales                       |
| Lombok                         | latest     | Reducir código boilerplate (getters, builders, etc.)  |
| PostgreSQL Driver (pgjdbc)     | latest     | Driver JDBC para PostgreSQL                           |
| JUnit 5                        | (incluido) | Framework de testing                                  |
| Mockito                        | (incluido) | Mocks para tests unitarios                            |
| Spring MockMvc / WebTestClient | (incluido) | Tests de integración de controllers                   |
| PostgreSQL de pruebas (db-test)| 17-alpine  | BD desechable para tests (docker compose, puerto 5433) |
| JaCoCo                         | latest     | Medición de cobertura de código                       |
| Checkstyle                     | latest     | Linter de estilo de código Java                       |

### 2.2 Frontend (`fe/`)

| Tecnología      | Versión | Rol                                          |
| --------------- | ------- | -------------------------------------------- |
| Node.js         | 20 LTS+ | Runtime de JavaScript                        |
| React           | 18+     | Biblioteca para interfaces de usuario        |
| Vite            | 6+      | Bundler y dev server ultrarrápido            |
| TypeScript      | 5.0+    | Superset tipado de JavaScript                |
| TailwindCSS     | 4+      | Framework CSS utility-first                  |
| React Router    | 7+      | Enrutamiento del lado del cliente            |
| Axios           | latest  | Cliente HTTP para comunicación con la API    |
| Vitest          | latest  | Framework de testing compatible con Vite     |
| Testing Library | latest  | Utilidades de testing para componentes React |
| ESLint          | latest  | Linter para TypeScript/React                 |
| Prettier        | latest  | Formateador de código                        |

### 2.3 Base de Datos

| Tecnología     | Versión | Rol                                                       |
| -------------- | ------- | --------------------------------------------------------- |
| PostgreSQL     | 17+     | Base de datos relacional principal                        |
| Docker Compose | 2.20+   | Orquestación de contenedores (BD + Mailpit en desarrollo) |

### 2.4 Autenticación

| Aspecto       | Detalle                                                                              |
| ------------- | ------------------------------------------------------------------------------------ |
| Método        | JWT (JSON Web Tokens) — stateless                                                    |
| Access Token  | Duración: 15 minutos                                                                 |
| Refresh Token | Duración: 7 días                                                                     |
| Hashing       | BCrypt vía Spring Security BCryptPasswordEncoder                                     |
| Firma JWT     | HMAC-SHA256 (HS256) vía JJWT                                                         |
| Flujos        | Registro, Login, Cambio de contraseña, Recuperación por email, Verificación de email |

---

## 3. Reglas de Lenguaje — OBLIGATORIAS

### 3.1 Nomenclatura técnica → INGLÉS

Todo lo que sea código debe estar en inglés:

- Variables, parámetros, constantes, métodos, clases, interfaces
- Nombres de paquetes y archivos de código
- Endpoints y rutas de la API
- Nombres de tablas y columnas en la base de datos
- Nombres de componentes React y hooks
- Mensajes de commits
- Ramas de git
- Scripts de migración Flyway (`V1__create_users_table.sql`)

```java
// ✅ CORRECTO
public User findUserByEmail(String email) { ... }

// ❌ INCORRECTO
public Usuario buscarUsuarioPorEmail(String correo) { ... }
```

```typescript
// ✅ CORRECTO
function getUserProfile(): Promise<UserResponse> { ... }

// ❌ INCORRECTO
function obtenerPerfilUsuario(): Promise<RespuestaUsuario> { ... }
```

### 3.2 Comentarios y documentación → ESPAÑOL

Todo lo que sea documentación o comentarios debe estar en español:

- Comentarios en el código (`//`, `/* */`)
- Javadoc de clases y métodos (`/** ... */`)
- Comentarios JSDoc en TypeScript
- Archivos de documentación (`.md`)
- README.md
- Descripciones en `application.yml`

### 3.3 Regla del comentario pedagógico — ¿QUÉ? ¿PARA QUÉ? ¿IMPACTO?

Cada comentario significativo debe responder tres preguntas:

```java
/**
 * ¿Qué? Método que convierte una contraseña en texto plano a un hash BCrypt seguro.
 * ¿Para qué? Almacenar contraseñas de forma segura en la BD, nunca en texto plano.
 * ¿Impacto? Si se omite el hashing, las contraseñas quedan expuestas ante
 *            una filtración de la BD — compromete a todos los usuarios.
 */
public String hashPassword(String plainPassword) {
    return passwordEncoder.encode(plainPassword);
}
```

```typescript
/**
 * ¿Qué? Hook personalizado que provee el estado de autenticación y sus acciones.
 * ¿Para qué? Centralizar la lógica de auth para que cualquier componente pueda consumirla.
 * ¿Impacto? Sin este hook, cada componente tendría que reimplementar la lógica de auth,
 *           causando duplicación de código y posibles inconsistencias.
 */
export function useAuth(): AuthContextType { ... }
```

### 3.4 Cabecera de archivo obligatoria

Cada archivo nuevo debe incluir un comentario de cabecera al inicio:

```java
/**
 * Archivo: JwtUtil.java
 * Descripción: Utilidades para crear y verificar tokens JWT usando la librería JJWT.
 * ¿Para qué? Proveer funciones reutilizables de manejo de JWT en todo el sistema de auth.
 * ¿Impacto? Es la base de la seguridad del sistema. Un error aquí compromete
 *            toda la autenticación stateless.
 */
package com.nn.auth.security;
```

```typescript
/**
 * Archivo: AuthContext.tsx
 * Descripción: Contexto de React que gestiona el estado de autenticación global.
 * ¿Para qué? Proveer a toda la aplicación acceso al usuario autenticado, tokens y acciones de auth.
 * ¿Impacto? Sin este contexto, no habría forma de saber si el usuario está logueado
 *           ni de proteger rutas que requieren autenticación.
 */
```

---

## 4. Reglas de Entorno y Herramientas — OBLIGATORIAS

### 4.1 Java — SIEMPRE usar Maven Wrapper

```bash
# ✅ CORRECTO — Usar el wrapper incluido en el proyecto
cd be
./mvnw spring-boot:run          # Linux / macOS / Git Bash
./mvnw test                     # Ejecutar tests
./mvnw verify                   # Tests + cobertura JaCoCo
./mvnw package -DskipTests      # Compilar JAR sin tests

# ❌ INCORRECTO — No usar Maven global si la versión difiere
mvn spring-boot:run             # Puede fallar si la versión local difiere del wrapper

# ❌ INCORRECTO — No mezclar con Gradle a menos que se apruebe explícitamente
gradle bootRun
```

### 4.2 Node.js — SIEMPRE usar `pnpm`

```bash
# ✅ CORRECTO
pnpm install
pnpm add axios
pnpm add -D vitest
pnpm dev
pnpm test
pnpm build

# ❌ INCORRECTO — NUNCA usar npm
npm install        # ← PROHIBIDO
npm run dev        # ← PROHIBIDO
npx some-tool      # ← Usar pnpm dlx en su lugar

# ❌ INCORRECTO — NUNCA usar yarn
yarn install       # ← PROHIBIDO
```

Si algún tutorial o documentación sugiere `npm`, reemplazar por el equivalente `pnpm`.

### 4.3 Variables de entorno

- **NUNCA** hardcodear credenciales, URLs de bases de datos, secrets, o configuración sensible
- Usar archivos `.env` (no versionados en git) para cargar variables al entorno
- Spring Boot lee las variables de entorno automáticamente si se configuran en `application.yml`
  con la sintaxis `${VARIABLE_NAME}` o `${VARIABLE_NAME:valor_default}`
- Proveer siempre un `.env.example` con las variables necesarias y valores de ejemplo
- Validar las variables de entorno al inicio (`@ConfigurationProperties` con `@Validated`)

```bash
# be/.env.example
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nn_auth_db
DB_USERNAME=nn_user
DB_PASSWORD=nn_password
JWT_SECRET=your-super-secret-key-min-32-chars-change-in-production
JWT_ACCESS_TOKEN_EXPIRATION_MINUTES=15
JWT_REFRESH_TOKEN_EXPIRATION_DAYS=7
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
FRONTEND_URL=http://localhost:5173
ENVIRONMENT=development
```

---

## 5. Estructura del Proyecto

```
proyecto-be_springboot_java-fe_react/                  # Raíz del monorepo
├── .github/
│   └── copilot-instructions.md    # ← ESTE ARCHIVO — reglas del proyecto
├── .gitignore                     # Archivos ignorados por git
├── docker-compose.yml             # Servicios: PostgreSQL 17 + Mailpit
├── README.md                      # Documentación principal del proyecto
│
├── docs/                         # 📚 Documentación del proyecto
│   ├── referencia-tecnica/
│   │   ├── architecture.md        # Arquitectura general y diagramas
│   │   ├── api-endpoints.md       # Documentación de todos los endpoints
│   │   └── database-schema.md     # Esquema de base de datos y ER diagram
│   └── conceptos/
│       ├── owasp-top-10.md        # OWASP Top 10 adaptado a Spring Boot
│       └── accesibilidad-aria-wcag.md # WCAG 2.1 AA en el frontend React
│
├── be/                            # ☕ Backend — Spring Boot (Java 21)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nn/auth/
│   │   │   │   ├── NnAuthApplication.java      # Punto de entrada (@SpringBootApplication)
│   │   │   │   ├── config/
│   │   │   │   │   ├── ApplicationConfig.java  # Beans globales (BCryptPasswordEncoder)
│   │   │   │   │   ├── SecurityConfig.java     # SecurityFilterChain, CORS, JWT Filter
│   │   │   │   │   └── OpenApiConfig.java      # Swagger on/off según ENVIRONMENT
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java     # POST /api/v1/auth/*
│   │   │   │   │   └── UserController.java     # GET /api/v1/users/me
│   │   │   │   ├── service/
│   │   │   │   │   └── AuthService.java        # Lógica de negocio de autenticación
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── PasswordResetTokenRepository.java
│   │   │   │   │   └── EmailVerificationTokenRepository.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── PasswordResetToken.java
│   │   │   │   │   └── EmailVerificationToken.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   ├── ChangePasswordRequest.java
│   │   │   │   │   │   ├── ForgotPasswordRequest.java
│   │   │   │   │   │   ├── ResetPasswordRequest.java
│   │   │   │   │   │   └── VerifyEmailRequest.java
│   │   │   │   │   └── response/
│   │   │   │   │       ├── UserResponse.java
│   │   │   │   │       ├── TokenResponse.java
│   │   │   │   │       └── MessageResponse.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtUtil.java                    # Crear y verificar JWT
│   │   │   │   │   └── JwtAuthenticationFilter.java    # OncePerRequestFilter
│   │   │   │   ├── exception/
│   │   │   │   │   └── GlobalExceptionHandler.java     # @ControllerAdvice
│   │   │   │   └── util/
│   │   │   │       ├── EmailService.java               # Envío de email
│   │   │   │       └── AuditLogger.java                # Logging estructurado JSON
│   │   │   └── resources/
│   │   │       ├── application.yml                     # Configuración base
│   │   │       ├── application-docker.yml              # Perfil Docker
│   │   │       ├── application-noDocker.yml            # Perfil sin Docker (mock email)
│   │   │       └── db/migration/                       # Scripts Flyway SQL
│   │   │           ├── V1__create_users_table.sql
│   │   │           ├── V2__create_password_reset_tokens_table.sql
│   │   │           └── V3__create_email_verification_tokens_table.sql
│   │   └── test/java/com/nn/auth/
│   │       └── controller/
│   │           └── AuthControllerTest.java             # Tests MockMvc + BD de pruebas
│   ├── .env                       # Variables de entorno (NO versionado)
│   ├── .env.example               # Plantilla de variables
│   ├── pom.xml                    # Dependencias y plugins Maven
│   ├── mvnw                       # Maven Wrapper (Linux/macOS)
│   └── mvnw.cmd                   # Maven Wrapper (Windows)
│
└── fe/                            # ⚛️ Frontend — React + Vite + TypeScript
    ├── .env                       # Variables de entorno (NO versionado)
    ├── .env.example               # Plantilla de variables
    ├── index.html                 # HTML base de Vite
    ├── package.json               # Dependencias y scripts
    ├── pnpm-lock.yaml             # Lockfile de pnpm
    ├── vite.config.ts             # Configuración de Vite
    ├── tsconfig.json              # Configuración de TypeScript
    ├── eslint.config.js           # Configuración de ESLint
    └── src/
        ├── main.tsx               # Punto de entrada
        ├── App.tsx                # Componente raíz — define rutas
        ├── index.css              # Estilos globales + imports de Tailwind
        ├── api/
        │   ├── auth.ts            # Funciones para cada endpoint de auth
        │   └── axios.ts           # Instancia de Axios con interceptores JWT
        ├── components/
        │   ├── ui/                # Button, InputField, Alert
        │   └── layout/            # AuthLayout, Navbar
        ├── pages/
        │   ├── LoginPage.tsx
        │   ├── RegisterPage.tsx
        │   ├── DashboardPage.tsx
        │   ├── ChangePasswordPage.tsx
        │   ├── ForgotPasswordPage.tsx
        │   ├── ResetPasswordPage.tsx
        │   └── LandingPage.tsx
        ├── hooks/
        │   └── useAuth.ts
        ├── context/
        │   └── AuthContext.tsx
        ├── types/
        │   └── auth.ts
        └── __tests__/
            └── auth.test.tsx
```

---

## 6. Convenciones de Código

### 6.1 Java (Backend)

| Aspecto           | Convención                                                       |
| ----------------- | ---------------------------------------------------------------- |
| Estilo general    | Google Java Style Guide, reforzado por Checkstyle                |
| Naming variables  | camelCase                                                        |
| Naming clases     | PascalCase                                                       |
| Naming constantes | UPPER_SNAKE_CASE                                                 |
| Naming paquetes   | lowercase (com.nn.auth.service)                                  |
| Anotaciones       | Línea separada por encima del elemento anotado                   |
| Tipos de retorno  | Siempre explícitos — nunca `var` en métodos públicos             |
| Javadoc           | Obligatorio en métodos públicos y clases — en español            |
| Línea máxima      | 120 caracteres                                                   |
| Lombok            | Permitido para `@Getter`, `@Builder`, `@RequiredArgsConstructor` |

```java
// ✅ Ejemplo de servicio bien documentado y tipado
/**
 * ¿Qué? Busca un usuario en la BD por su dirección de email.
 * ¿Para qué? Se usa en el login para verificar si el usuario existe antes de
 *            comparar la contraseña.
 * ¿Impacto? Si devuelve null sin manejarlo, el sistema lanzaría NullPointerException
 *            en el proceso de login, causando un 500 en lugar de un 401 informativo.
 *
 * @param email Dirección de email del usuario (case-insensitive).
 * @return Optional con el User si existe, Optional.empty() si no.
 */
public Optional<User> findUserByEmail(String email) {
    return userRepository.findByEmailIgnoreCase(email);
}
```

### 6.2 TypeScript/React (Frontend)

| Aspecto             | Convención                                                            |
| ------------------- | --------------------------------------------------------------------- |
| Estilo              | ESLint + Prettier                                                     |
| Naming variables    | camelCase                                                             |
| Naming componentes  | PascalCase                                                            |
| Naming archivos     | PascalCase para componentes, camelCase para utilidades                |
| Naming tipos        | PascalCase con sufijo descriptivo (UserResponse, LoginRequest)        |
| Componentes         | Funcionales con hooks — nunca clases                                  |
| Interfaces vs Types | Preferir `interface` para objetos, `type` para uniones/intersecciones |
| CSS                 | TailwindCSS utility classes — evitar CSS custom                       |
| Strict mode         | `"strict": true` en tsconfig.json                                     |

### 6.3 SQL / Base de Datos

| Aspecto             | Convención                                                                   |
| ------------------- | ---------------------------------------------------------------------------- |
| Nombres de tablas   | snake_case, plural (users, password_reset_tokens)                            |
| Nombres de columnas | snake_case (created_at, hashed_password)                                     |
| Primary Keys        | `id` con tipo UUID                                                           |
| Foreign Keys        | `<tabla_singular>_id` (ej: user_id)                                          |
| Timestamps          | `created_at`, `updated_at` en toda tabla                                     |
| Migraciones         | Siempre vía Flyway (`V{n}__{descripcion}.sql`), nunca alterar BD manualmente |

### 6.4 Spring Boot — Patrones obligatorios

```java
// ✅ DTOs con Bean Validation — equivalente a Pydantic en FastAPI
public record RegisterRequest(
    @NotBlank(message = "El email es requerido")
    @Email(message = "Formato de email inválido")
    String email,

    @NotBlank(message = "El nombre es requerido")
    String fullName,

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = ".*[A-Z].*", message = "Debe contener al menos una mayúscula")
    @Pattern(regexp = ".*[a-z].*", message = "Debe contener al menos una minúscula")
    @Pattern(regexp = ".*\\d.*", message = "Debe contener al menos un número")
    String password
) {}
```

```java
// ✅ Controller bien estructurado — separar HTTP de lógica de negocio
// La lógica siempre va en el Service, NO en el Controller
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        // ¿Qué? Recibe el request validado y delega al servicio.
        // ¿Para qué? El controller solo coordina — nunca tiene lógica de negocio.
        // ¿Impacto? Si se pone lógica aquí, se viola la separación de responsabilidades.
        return authService.registerUser(request);
    }
}
```

---

## 7. Conventional Commits — OBLIGATORIO

### 7.1 Formato

```
type(scope): short description in english

What: Detailed description of what was done
For: Why this change is needed
Impact: What effect this has on the system
```

### 7.2 Tipos permitidos

| Tipo       | Uso                                        |
| ---------- | ------------------------------------------ |
| `feat`     | Nueva funcionalidad                        |
| `fix`      | Corrección de bug                          |
| `docs`     | Solo documentación                         |
| `style`    | Formato, espacios (no afecta lógica)       |
| `refactor` | Reestructuración sin cambiar funcionalidad |
| `test`     | Agregar o corregir tests                   |
| `chore`    | Mantenimiento, configuración, dependencias |
| `ci`       | Cambios en CI/CD                           |
| `perf`     | Mejoras de rendimiento                     |

### 7.3 Scopes sugeridos

- `auth` — Autenticación y autorización
- `user` — Entidad/funcionalidad de usuario
- `db` — Entidades JPA y migraciones Flyway
- `api` — Controllers y DTOs
- `security` — JWT, filtros, BCrypt
- `ui` — Componentes y estilos del frontend
- `config` — application.yml, Docker, configuración
- `test` — Tests unitarios e integración
- `deps` — Dependencias (pom.xml, package.json)

### 7.4 Ejemplos

```bash
# ✅ Ejemplo de commit completo
git commit -m "feat(auth): add user registration endpoint

What: Creates POST /api/v1/auth/register endpoint with email validation,
password hashing via BCrypt and duplicate email check
For: Allow new users to create accounts in the NN Auth System
Impact: Enables the user onboarding flow; stores hashed passwords using
BCrypt in the users table and sends verification email via Mailpit"
```

```bash
# ✅ Ejemplo de fix
git commit -m "fix(security): handle expired refresh token with 401 instead of 500

What: JwtUtil now catches ExpiredJwtException and wraps it in AuthException
For: Prevent confusing 500 errors when users try to refresh after 7 days
Impact: Improves UX by redirecting to login instead of showing error page"
```

---

## 8. Calidad — NO es Opcional, es OBLIGACIÓN

### 8.1 Principio fundamental

> **Código que se genera, código que se prueba.**

Cada método de servicio, endpoint, componente o utilidad que se cree debe tener su test correspondiente.
No se considera "terminada" una feature hasta que sus tests pasen.

### 8.2 Testing — Backend

| Herramienta                 | Uso                                     |
| --------------------------- | --------------------------------------- |
| JUnit 5                     | Framework principal de testing          |
| Mockito                     | Mocks para tests unitarios de servicios |
| Spring MockMvc              | Tests de integración de controllers     |
| PostgreSQL de pruebas (db-test) | BD real para tests de integración |
| JaCoCo                      | Medir cobertura de código               |

```bash
# Ejecutar todos los tests del backend (requiere db-test y TEST_DATABASE_URL)
cd be && ./mvnw test

# Ejecutar con reporte de cobertura JaCoCo
./mvnw verify
# → Ver reporte: target/site/jacoco/index.html

# Ejecutar tests de una clase específica
./mvnw test -Dtest=AuthControllerTest

# Ejecutar en modo watch (requiere Maven Daemon)
./mvnw test --watch
```

Cobertura mínima esperada: **80%** en clases de servicio y controladores.

### 8.3 Testing — Frontend

| Herramienta            | Uso                             |
| ---------------------- | ------------------------------- |
| Vitest                 | Test runner compatible con Vite |
| @testing-library/react | Testing de componentes React    |
| jsdom                  | Simular el DOM en Node.js       |

```bash
# Ejecutar todos los tests del frontend
cd fe && pnpm test

# Ejecutar en modo watch
pnpm test:watch

# Ejecutar con cobertura
pnpm test:coverage
```

### 8.4 Linting y Formateo

```bash
# Backend — Checkstyle
cd be && ./mvnw checkstyle:check    # Verificar estilo
cd be && ./mvnw checkstyle:checkstyle && open target/site/checkstyle.html  # Ver reporte

# Frontend — ESLint + Prettier
cd fe && pnpm lint                  # Verificar errores
cd fe && pnpm format                # Formatear código
```

### 8.5 Checklist antes de commit

- [ ] ¿El código tiene tipos explícitos (Java genéricos / TypeScript strict)?
- [ ] ¿Hay comentarios pedagógicos (¿Qué? ¿Para qué? ¿Impacto?)?
- [ ] ¿Los tests pasan? (`./mvnw test` / `pnpm test`)
- [ ] ¿El linter no reporta errores? (`mvnw checkstyle:check` / `pnpm lint`)
- [ ] ¿El commit sigue Conventional Commits con What/For/Impact?
- [ ] ¿Las variables sensibles están en `.env` y no hardcodeadas?
- [ ] ¿El `.env.example` se actualizó si se agregaron nuevas variables?

---

## 9. Seguridad — Mejores Prácticas

### 9.1 Contraseñas

- SIEMPRE hashear con BCrypt (`BCryptPasswordEncoder`) antes de almacenar
- NUNCA almacenar contraseñas en texto plano
- NUNCA loggear contraseñas ni incluirlas en responses
- Validar fortaleza mínima con Bean Validation: ≥8 chars, 1 mayúscula, 1 minúscula, 1 número

### 9.2 JWT (Tokens)

- **Access Token:** corta duración (15 min) — se envía en header `Authorization: Bearer <token>`
- **Refresh Token:** larga duración (7 días) — solo para obtener nuevos access tokens
- **Secret key:** mínimo 32 caracteres, aleatoria, en variable de entorno (`JWT_SECRET`)
- **Algoritmo:** HS256
- NUNCA almacenar tokens en `localStorage` en producción (usar memoria de React o httpOnly cookies)
- Validar al iniciar: si `JWT_SECRET` tiene menos de 32 caracteres, la app NO debe arrancar

### 9.3 CORS

- Configurar orígenes permitidos explícitamente en `SecurityConfig.java`
- En desarrollo: permitir `http://localhost:5173`
- En producción: NUNCA usar `allowedOrigins("*")`

### 9.4 API

- Versionamiento: `/api/v1/...`
- Rate limiting con Bucket4j en endpoints de auth (prevenir brute force)
- Validación de inputs con `@Valid` + DTOs (nunca confiar en datos del cliente)
- Mensajes de error genéricos en auth (no revelar si el email existe)
- Swagger UI deshabilitado en producción (`ENVIRONMENT=production`)

### 9.5 Base de datos

- Usar siempre JPA Repository / JPQL (nunca raw SQL sin parametrizar)
- Credenciales en variables de entorno, nunca en `application.yml` versionado
- Pool de conexiones configurado (HikariCP — incluido en Spring Boot)

---

## 10. Estructura de la API

### 10.1 Prefijo base

Todos los endpoints van bajo `/api/v1/`

> **Nota:** Spring Boot corre en el puerto **8080** por defecto (a diferencia de FastAPI en 8000).
> El Swagger UI está en `http://localhost:8080/swagger-ui.html`

### 10.2 Endpoints de autenticación (`/api/v1/auth/`)

| Método | Ruta               | Descripción                           | Auth requerida |
| ------ | ------------------ | ------------------------------------- | -------------- |
| POST   | `/register`        | Registrar nuevo usuario               | No             |
| POST   | `/login`           | Iniciar sesión, obtener tokens        | No             |
| POST   | `/refresh`         | Renovar access token con refresh      | No (\*)        |
| POST   | `/change-password` | Cambiar contraseña (usuario logueado) | Sí             |
| POST   | `/forgot-password` | Solicitar email de recuperación       | No             |
| POST   | `/reset-password`  | Restablecer contraseña con token      | No (\*)        |
| POST   | `/verify-email`    | Verificar dirección de email          | No (\*)        |

(\*) Requiere un token válido (refresh o reset), pero no el access token estándar.

### 10.3 Endpoints de usuario (`/api/v1/users/`)

| Método | Ruta  | Descripción                       | Auth requerida |
| ------ | ----- | --------------------------------- | -------------- |
| GET    | `/me` | Obtener perfil del usuario actual | Sí             |

---

## 11. Esquema de Base de Datos

### 11.1 Tabla `users`

| Columna           | Tipo         | Constraints                   |
| ----------------- | ------------ | ----------------------------- |
| id                | UUID         | PK, default gen_random_uuid() |
| email             | VARCHAR(255) | UNIQUE, NOT NULL, INDEXED     |
| full_name         | VARCHAR(255) | NOT NULL                      |
| hashed_password   | VARCHAR(255) | NOT NULL                      |
| is_active         | BOOLEAN      | DEFAULT TRUE                  |
| is_email_verified | BOOLEAN      | DEFAULT FALSE                 |
| created_at        | TIMESTAMPTZ  | DEFAULT NOW(), NOT NULL       |
| updated_at        | TIMESTAMPTZ  | DEFAULT NOW(), NOT NULL       |

### 11.2 Tabla `password_reset_tokens`

| Columna    | Tipo         | Constraints                             |
| ---------- | ------------ | --------------------------------------- |
| id         | UUID         | PK, default gen_random_uuid()           |
| user_id    | UUID         | FK → users.id, CASCADE DELETE, NOT NULL |
| token      | VARCHAR(255) | UNIQUE, NOT NULL, INDEXED               |
| expires_at | TIMESTAMPTZ  | NOT NULL                                |
| used       | BOOLEAN      | DEFAULT FALSE                           |
| created_at | TIMESTAMPTZ  | DEFAULT NOW(), NOT NULL                 |

### 11.3 Tabla `email_verification_tokens`

| Columna    | Tipo         | Constraints                             |
| ---------- | ------------ | --------------------------------------- |
| id         | UUID         | PK, default gen_random_uuid()           |
| user_id    | UUID         | FK → users.id, CASCADE DELETE, NOT NULL |
| token      | VARCHAR(255) | UNIQUE, NOT NULL, INDEXED               |
| expires_at | TIMESTAMPTZ  | NOT NULL                                |
| used       | BOOLEAN      | DEFAULT FALSE                           |
| created_at | TIMESTAMPTZ  | DEFAULT NOW(), NOT NULL                 |

---

## 12. Flujos de Autenticación

### 12.1 Registro

```
Cliente → POST /api/v1/auth/register { email, fullName, password }
  → @Valid valida el DTO (Bean Validation)
  → Verificar email no duplicado (userRepository.existsByEmailIgnoreCase)
  → Hash password (BCryptPasswordEncoder.encode)
  → Crear User (isEmailVerified=false)
  → Crear EmailVerificationToken (UUID, expira en 24h)
  → Enviar email de verificación (JavaMailSender)
  → Retornar UserResponse (sin hashed_password)
```

### 12.2 Login

```
Cliente → POST /api/v1/auth/login { email, password }
  → Buscar usuario por email
  → Verificar password (BCryptPasswordEncoder.matches)
  → Verificar isEmailVerified == true
  → Verificar isActive == true
  → Generar access_token (15 min) + refresh_token (7 días) con JwtUtil
  → Registrar evento en AuditLogger
  → Retornar TokenResponse { accessToken, refreshToken, tokenType }
```

### 12.3 Cambio de contraseña (usuario autenticado)

```
Cliente → POST /api/v1/auth/change-password { currentPassword, newPassword }
  → (Requiere Authorization: Bearer <access_token>)
  → JwtAuthenticationFilter extrae user del token → SecurityContext
  → Verificar currentPassword contra hash
  → Hash newPassword
  → Actualizar en BD + AuditLogger
  → Retornar MessageResponse
```

### 12.4 Recuperación de contraseña

```
Paso 1: POST /api/v1/auth/forgot-password { email }
  → Buscar usuario (si no existe, respuesta genérica de todas formas)
  → Generar PasswordResetToken (UUID, expira en 1 hora)
  → Enviar email con enlace: {FRONTEND_URL}/reset-password?token={token}
  → Retornar respuesta SIEMPRE idéntica (no revelar si el email existe)

Paso 2: POST /api/v1/auth/reset-password { token, newPassword }
  → Buscar token en BD
  → Verificar que no haya expirado (expires_at > now()) ni usado (used == false)
  → Hash newPassword + actualizar users.hashed_password
  → Marcar token como usado (used = true)
  → Retornar MessageResponse
```

---

## 13. Configuración de Perfiles Spring Boot

Spring Boot usa **perfiles** (`spring.profiles.active`) para ambientes distintos:

```yaml
# application.yml — configuración base (siempre se carga)
spring:
  application:
    name: nn-auth-system
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:nn_auth_db}
    username: ${DB_USERNAME:nn_user}
    password: ${DB_PASSWORD:nn_password}
  flyway:
    enabled: true
    locations: classpath:db/migration

# application-docker.yml — se activa con: -Dspring.profiles.active=docker
# (equal ports, just mail points to Mailpit)

# application-noDocker.yml — se activa con: -Dspring.profiles.active=noDocker
# spring.mail.host=localhost → pero con MockMailSender que imprime en consola
```

| Variable       | `development`                       | `production`                                   |
| -------------- | ----------------------------------- | ---------------------------------------------- |
| `ENVIRONMENT`  | development                         | production                                     |
| Swagger UI     | ✅ Disponible en `/swagger-ui.html` | ❌ Deshabilitado (404)                         |
| `DATABASE_URL` | localhost:5432                      | Servidor de BD en cloud                        |
| `FRONTEND_URL` | http://localhost:5173               | https://tu-dominio.com                         |
| `JWT_SECRET`   | Clave de desarrollo (≥32 ch)        | Clave aleatoria larga (`openssl rand -hex 64`) |

---

## 14. Configuración de Docker Compose

```yaml
# Solo para desarrollo local — PostgreSQL 17 + Mailpit
services:
  db:
    image: postgres:17-alpine
    container_name: nn_auth_db
    environment:
      POSTGRES_USER: nn_user
      POSTGRES_PASSWORD: nn_password
      POSTGRES_DB: nn_auth_db
    ports:
      - "5432:5432"
    volumes:
      - nn_auth_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nn_user -d nn_auth_db"]
      interval: 5s
      timeout: 5s
      retries: 5

  mailpit:
    image: axllent/mailpit:latest
    container_name: nn_auth_mailpit
    ports:
      - "1025:1025" # SMTP — Spring Mail apunta aquí
      - "8025:8025" # Web UI — ver emails capturados

volumes:
  nn_auth_data:
```

---

## 15. Mejores Prácticas — Resumen

### 15.1 Generales

- ✅ DRY (Don't Repeat Yourself) — reutilizar código
- ✅ KISS (Keep It Simple, Stupid) — preferir soluciones simples
- ✅ YAGNI (You Aren't Gonna Need It) — no agregar lo que no se necesita aún
- ✅ Separation of Concerns — Controller / Service / Repository / Entity bien separados
- ✅ Fail fast — validar inputs al inicio (`@Valid` en controllers)
- ✅ Defensive programming — manejar errores explícitamente

### 15.2 Backend Spring Boot

- ✅ Inyección de dependencias vía constructor (`@RequiredArgsConstructor` de Lombok)
- ✅ Separar Controller (HTTP) de Service (lógica de negocio) de Repository (datos)
- ✅ Usar `@ControllerAdvice` para manejo centralizado de excepciones (`GlobalExceptionHandler`)
- ✅ Usar `@Valid` + DTOs para toda validación de entrada
- ✅ Usar `Optional<T>` en repositorios para evitar NullPointerException
- ✅ Documentar automáticamente con SpringDoc OpenAPI (`/swagger-ui.html`)
- ✅ Usar `@ConfigurationProperties` para configuración validada (equivalente a Pydantic Settings)

### 15.3 Frontend

- ✅ Componentes pequeños y reutilizables
- ✅ Estado global solo cuando es necesario (Context API para auth)
- ✅ Custom hooks para encapsular lógica reutilizable
- ✅ Rutas protegidas con componente `ProtectedRoute`
- ✅ Manejo de errores con feedback visual al usuario
- ✅ Loading states para operaciones asíncronas

### 15.4 Diseño y UX/UI — OBLIGATORIO

| Aspecto        | Regla                                                              |
| -------------- | ------------------------------------------------------------------ |
| Temas          | Dark mode y Light mode con toggle — usar `prefers-color-scheme`    |
| Tipografía     | Fuentes sans-serif exclusivamente (Inter, system-ui)               |
| Colores        | Sólidos y planos — **SIN degradados** (gradient) en ningún lugar   |
| Estilo visual  | Moderno, limpio, minimalista con excelente UX/UI                   |
| Botones acción | Siempre alineados a la derecha (`justify-end`, `text-right`)       |
| Spacing        | Escala consistente de Tailwind (p-4, gap-6, space-y-4)             |
| Responsividad  | Mobile-first — formularios de auth visibles en móvil               |
| Accesibilidad  | Labels en inputs, `aria-*` básicos, contraste suficiente (WCAG AA) |

### 15.5 Sistema de Temas Diferenciales por Stack — OBLIGATORIO

Este proyecto pertenece a una **serie educativa** donde cada variante del sistema
tiene un color de acento único que identifica su stack. Los componentes React
**NUNCA** usan colores hardcodeados — siempre el token `accent-*`.

#### Identidad visual — Spring Boot Java → **amber**

| Stack                  | Proyecto               | Acento Tailwind | Hex           |
| ---------------------- | ---------------------- | --------------- | ------------- |
| FastAPI (Python)       | `proyecto-be_fastapi-fe_react`       | `emerald`       | `#059669`     |
| Express.js (Node)      | `proyecto-beex-fe`     | `blue`          | `#2563eb`     |
| Next.js (fullstack)    | `proyecto-be-fe-next`  | `violet`        | `#7c3aed`     |
| **Spring Boot (Java)** | **`proyecto-be_springboot_java-fe_react`** | **`amber`**     | **`#d97706`** |
| Spring Boot (Kotlin)   | `proyecto-besbk-fe`    | `fuchsia`       | `#c026d3`     |
| Go REST API            | `proyecto-bego-fe`     | `cyan`          | `#0891b2`     |

#### Reglas del token `accent-*`

```tsx
// ✅ CORRECTO — usar siempre el token accent-*
<button className="bg-accent-600 hover:bg-accent-700 text-white">
  Iniciar sesión
</button>

// ❌ NUNCA hardcodear el color del stack en los componentes
<button className="bg-amber-600 hover:bg-amber-700 text-white">
  Iniciar sesión
</button>
```

#### Configuración en `fe/src/index.css`

```css
/* Spring Boot Java → amber */
@theme inline {
  --color-accent-50: var(--color-amber-50);
  --color-accent-100: var(--color-amber-100);
  --color-accent-200: var(--color-amber-200);
  --color-accent-300: var(--color-amber-300);
  --color-accent-400: var(--color-amber-400);
  --color-accent-500: var(--color-amber-500);
  --color-accent-600: var(--color-amber-600); /* botones primarios */
  --color-accent-700: var(--color-amber-700); /* hover */
  --color-accent-800: var(--color-amber-800);
  --color-accent-900: var(--color-amber-900);
  --color-accent-950: var(--color-amber-950);
}
```

#### Colores del logo SVG — Amber

| Elemento           | Atributo SVG       | Color     |
| ------------------ | ------------------ | --------- |
| Borde del badge    | `stroke="#d97706"` | amber-600 |
| Trazos de la letra | `stroke="#fbbf24"` | amber-400 |

> Ver `docs/referencia-tecnica/design-system.md` para el sistema completo de tokens,
> instrucciones de clonación y verificación visual.

---

## 16. Reglas para Copilot / IA — Al Generar Código

1. **Dividir respuestas largas** — Si la implementación es extensa, dividirla en pasos incrementales. No generar todo de golpe.
2. **Código generado = código probado** — Siempre incluir o sugerir tests para lo que se genere.
3. **Comentarios pedagógicos** — Cada bloque significativo debe tener comentarios con ¿Qué? ¿Para qué? ¿Impacto?
4. **Tipos obligatorios** — Nunca omitir tipos en Java (genéricos, retornos) ni en TypeScript.
5. **Formato correcto** — Respetar Google Java Style para Java y Prettier/ESLint para TypeScript.
6. **Usar las herramientas correctas** — `./mvnw` para Java, `pnpm` para Node.js. Sin excepciones.
7. **Variables de entorno** — Toda configuración sensible va en `.env`, nunca hardcodeada.
8. **Conventional Commits** — Sugerir mensajes de commit con formato correcto.
9. **Seguridad primero** — Nunca almacenar passwords en texto plano, nunca exponer secrets.
10. **Legibilidad sobre cleverness** — El código debe ser entendible para un aprendiz.
11. **Stack correcto** — Este proyecto usa Spring Boot (Java 21), NO FastAPI ni Python. Adaptare siempre al stack correcto.

---

## 17. Plan de Trabajo — Fases

> Cada fase es independiente y verificable. No avanzar a la siguiente sin completar y probar la actual.

### Fase 0 — Fundamentos y Configuración Base

- [ ] Crear `.github/copilot-instructions.md` (este archivo)
- [ ] Crear `.gitignore` raíz
- [ ] Crear `docker-compose.yml` con PostgreSQL 17 + Mailpit
- [ ] Crear `README.md` con descripción, stack, prerrequisitos y setup

### Fase 1 — Backend Setup (Spring Boot)

- [ ] Inicializar proyecto Spring Boot en `be/` (Spring Initializr o `./mvnw`)
- [ ] Configurar `pom.xml` con todas las dependencias
- [ ] Crear `application.yml` con configuración base y profiles
- [ ] Crear `ApplicationConfig.java` — BCryptPasswordEncoder bean
- [ ] Crear `SecurityConfig.java` — SecurityFilterChain (sin auth todavía)
- [ ] Crear `OpenApiConfig.java` — Swagger UI condicional
- [ ] Crear `.env.example` y `.env`
- [ ] ✅ Verificar: `./mvnw spring-boot:run` → app arranca en puerto 8080

### Fase 2 — Modelo de Datos y Migraciones Flyway

- [ ] Crear entidades JPA: `User.java`, `PasswordResetToken.java`, `EmailVerificationToken.java`
- [ ] Crear repositorios Spring Data JPA
- [ ] Crear scripts Flyway: `V1__create_users_table.sql`, `V2__create_password_reset_tokens_table.sql`, `V3__create_email_verification_tokens_table.sql`
- [ ] ✅ Verificar: tablas creadas en PostgreSQL al arrancar

### Fase 3 — Autenticación Backend

- [ ] Crear `JwtUtil.java` — crear y verificar tokens JWT con JJWT
- [ ] Crear `JwtAuthenticationFilter.java` — OncePerRequestFilter
- [ ] Actualizar `SecurityConfig.java` — agregar el filtro JWT
- [ ] Crear DTOs de request y response
- [ ] Crear `AuthService.java` — lógica de negocio completa
- [ ] Crear `EmailService.java` — envío de emails con JavaMailSender
- [ ] Crear `AuditLogger.java` — logging estructurado JSON (SLF4J + Logback)
- [ ] Crear `AuthController.java` — todos los endpoints de auth
- [ ] Crear `UserController.java` — GET /api/v1/users/me
- [ ] Crear `GlobalExceptionHandler.java` — manejo de errores
- [ ] ✅ Verificar: probar todos los endpoints en Swagger UI

### Fase 4 — Tests Backend

- [ ] Configurar la BD de pruebas (servicio db-test + TEST_DATABASE_URL)
- [ ] Crear `AuthControllerTest.java` — tests completos con MockMvc
- [ ] ✅ Verificar: `./mvnw test` → todos los tests pasan, cobertura ≥80%

### Fase 5 — Frontend Setup

- [ ] Inicializar proyecto Vite con React + TypeScript en `fe/`
- [ ] Instalar dependencias con `pnpm`
- [ ] Configurar TailwindCSS 4
- [ ] Configurar TypeScript strict mode
- [ ] Crear `.env.example` (apuntar al backend en puerto 8080)
- [ ] ✅ Verificar: `pnpm dev` → app base visible en `http://localhost:5173`

### Fase 6 — Frontend Auth

- [ ] Crear tipos TypeScript (`types/auth.ts`)
- [ ] Crear cliente HTTP (`api/auth.ts` + `api/axios.ts`)
- [ ] Crear AuthContext + Provider
- [ ] Crear hook `useAuth`
- [ ] Crear componentes UI (InputField, Button, Alert)
- [ ] Crear ProtectedRoute
- [ ] Crear todas las páginas (Login, Register, Dashboard, etc.)
- [ ] Crear LandingPage con logo, features, stack y CTAs
- [ ] Crear páginas legales (Términos, Privacidad, Cookies)
- [ ] Crear ContactPage
- [ ] ✅ Verificar: flujo completo funciona contra la API Spring Boot

### Fase 7 — Tests Frontend

- [ ] Configurar Vitest + Testing Library
- [ ] Crear tests para componentes y flujos de auth
- [ ] ✅ Verificar: `pnpm test` → todos los tests pasan

### Fase 8 — Documentación Final

- [ ] Crear `docs/referencia-tecnica/architecture.md`
- [ ] Crear `docs/referencia-tecnica/api-endpoints.md`
- [ ] Crear `docs/referencia-tecnica/database-schema.md`
- [ ] Crear `docs/conceptos/owasp-top-10.md`
- [ ] Crear `docs/conceptos/accesibilidad-aria-wcag.md`
- [ ] Actualizar `README.md` con instrucciones finales
- [ ] ✅ Verificar: documentación completa y coherente

---

## 18. Verificación Final del Sistema

```bash
# 1. Levantar base de datos
docker compose up -d

# 2. Levantar backend
cd be && ./mvnw spring-boot:run

# 3. Levantar frontend (en otra terminal)
cd fe && pnpm dev

# 4. Ejecutar tests backend (requiere db-test y TEST_DATABASE_URL)
cd be && ./mvnw verify

# 5. Ejecutar tests frontend
cd fe && pnpm test

# 6. Flujo manual completo:
#    Registro → Verificar email (Mailpit en http://localhost:8025)
#    → Login → Ver perfil → Cambiar contraseña
#    → Logout → Forgot password → Reset password → Login con nueva contraseña
```

> Recuerda: **La calidad no es una opción, es una obligación.**
> Cada línea de código es una oportunidad de aprender y enseñar.
