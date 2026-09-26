<!--
  Archivo: patrones-arquitectonicos.md
  Descripción: Documentación técnica de los patrones arquitectónicos aplicados
               en el proyecto NN Auth System — edición Spring Boot (Java 21).
  ¿Para qué? Servir como referencia de estudio y consulta para entender por qué
             el sistema está estructurado como lo está.
  ¿Impacto? Comprender los patrones facilita mantener, extender y defender
            decisiones técnicas del proyecto ante evaluaciones o presentaciones.
-->

# Patrones Arquitectónicos — NN Auth System (Spring Boot Java)

> **Proyecto:** NN Auth System — Spring Boot Edition
> **Stack:** Spring Boot 3 (Java 21) + React + PostgreSQL 17 + Docker
> **Cobertura tests objetivo:** ≥80% backend (JaCoCo) · frontend (Vitest)

---

## Resumen ejecutivo

El sistema aplica **10 patrones arquitectónicos y de diseño** de uso profesional.
No son solo teoría: cada patrón resuelve un problema concreto y está presente
en el código del proyecto.

| #   | Patrón                      | Dónde vive (Spring Boot)               | Qué resuelve                                            |
| --- | --------------------------- | -------------------------------------- | ------------------------------------------------------- |
| 1   | Arquitectura en Capas       | `be/src/main/java/com/nn/auth/`        | Separación de responsabilidades en el backend           |
| 2   | DTO — Data Transfer Object  | `dto/request/` + `dto/response/`       | Nunca exponer datos internos de BD en respuestas        |
| 3   | Inyección de Dependencias   | `@RequiredArgsConstructor` + Spring DI | Desacoplar servicios transversales (BD, auth, email)    |
| 4   | JWT Stateless               | `security/JwtUtil.java`                | Autenticación sin estado en el servidor                 |
| 5   | Context / Provider          | `context/AuthContext.tsx`              | Estado de auth global en toda la app React              |
| 6   | Custom Hook                 | `hooks/useAuth.ts`                     | Encapsular y reutilizar lógica de autenticación         |
| 7   | Interceptor                 | `api/axios.ts`                         | Adjuntar token JWT en cada petición automáticamente     |
| 8   | SPA + Route Guard           | `components/ProtectedRoute.tsx`        | Proteger rutas sin renderizar páginas no autorizadas    |
| 9   | Monorepo                    | `be/` + `fe/`                          | Código fuente unificado en un solo repositorio          |
| 10  | REST API                    | `controller/AuthController.java`       | Interfaz estándar entre frontend y backend              |

---

## Vista general del sistema

El sistema sigue una **arquitectura Cliente–Servidor** de tres capas lógicas:

1. **Frontend (React)** — Interfaz de usuario. Nunca guarda estado en el servidor.
2. **Backend (Spring Boot)** — Lógica de negocio. Expone una API REST bajo `/api/v1/`.
3. **Base de datos (PostgreSQL)** — Persistencia. Solo accedida desde el backend.

La comunicación entre frontend y backend es exclusivamente **HTTP + JSON**.
Los tokens JWT viajan en el header `Authorization: Bearer <token>`.
Nunca hay sesiones en el servidor.

---

## Patrón 1 — Arquitectura en Capas

### ¿Qué es?

Organizar el código en capas horizontales donde **cada capa solo puede comunicarse
con la capa directamente inferior**.

### ¿Cómo se aplica aquí?

```
HTTP Request
      ↓
┌─────────────────────────────────────────┐
│  controller/      → Capa HTTP           │  Recibe y devuelve HTTP
├─────────────────────────────────────────┤
│  service/         → Capa de Negocio     │  Reglas y decisiones
├─────────────────────────────────────────┤
│  repository/      → Capa de Acceso      │  Spring Data JPA (ORM)
├─────────────────────────────────────────┤
│  entity/          → Capa de Datos       │  Entidades JPA + Hibernate
├─────────────────────────────────────────┤
│  util/ + security → Capa Transversal    │  JWT · email · logging
└─────────────────────────────────────────┘
      ↓
PostgreSQL
```

### Ejemplo en código

```java
// AuthController.java — solo recibe el request y llama al service
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    // ¿Qué? Recibe el request HTTP validado y delega al service.
    // ¿Para qué? El controller no tiene lógica de negocio.
    // ¿Impacto? Si se pone lógica aquí, se viola la separación de capas.
    return authService.registerUser(request);
}

// AuthService.java — contiene la lógica
public UserResponse registerUser(RegisterRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
        throw new RuntimeException("El email ya está registrado");
    }
    String hashedPassword = passwordEncoder.encode(request.password());
    // ...guarda usuario, genera token de verificación, envía email
}
```

### Ventaja

Un cambio en la base de datos **no afecta** el controller. Un cambio en el controller
**no afecta** la lógica de negocio. Cada capa es testeable de forma independiente.

---

## Patrón 2 — DTO (Data Transfer Object)

### ¿Qué es?

Un objeto diseñado exclusivamente para transportar datos entre capas,
diferente del modelo de base de datos.

### ¿Por qué es crítico aquí?

La entidad JPA `User` tiene siete columnas incluyendo `hashedPassword`.
Si devolviéramos la entidad directamente, **el hash de la contraseña quedaría
expuesto en la respuesta HTTP**. El DTO actúa como filtro.

### Ejemplo en código

```java
// entity/User.java — Entidad JPA (lo que hay en la BD)
@Entity
@Table(name = "users")
public class User {
    private UUID id;
    private String email;
    private String fullName;
    private String hashedPassword;  // ← NUNCA debe salir en la respuesta
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
}

// dto/response/UserResponse.java — DTO (lo que se devuelve al cliente)
public record UserResponse(
    UUID id,
    String email,
    String fullName,
    // hashedPassword: ← OMITIDO intencionalmente
    boolean isActive,
    boolean isEmailVerified,
    LocalDateTime createdAt
) {
    // ¿Qué? Conversión estática desde la entidad JPA al DTO de respuesta.
    // ¿Para qué? Centralizar el mapeo y evitar filtrar campos sensibles.
    // ¿Impacto? Si se omite este paso, hashedPassword quedaría expuesto.
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(), user.getEmail(), user.getFullName(),
            user.getIsActive(), user.getIsEmailVerified(), user.getCreatedAt()
        );
    }
}
```

### Ventaja

La API puede cambiar su contrato (el DTO) **sin alterar la estructura de la base
de datos**, y viceversa. También permite tener diferentes vistas del mismo dato
(`UserResponse` vs `UserSummaryResponse`).

---

## Patrón 3 — Inyección de Dependencias (DI)

### ¿Qué es?

En lugar de que cada clase cree sus propias dependencias, las recibe **inyectadas
desde afuera** por el contenedor de Spring. En Spring Boot esto se implementa
mediante inyección por constructor con `@RequiredArgsConstructor` de Lombok.

### Ejemplo en código

```java
// AuthService.java — declara sus dependencias vía constructor
@Service
@RequiredArgsConstructor
public class AuthService {
    // ¿Qué? Spring inyecta automáticamente estas dependencias al crear el bean.
    // ¿Para qué? El service no necesita saber cómo se construye UserRepository.
    // ¿Impacto? En tests se puede inyectar un mock sin modificar AuthService.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public UserResponse registerUser(RegisterRequest request) {
        // Usa las dependencias inyectadas
        if (userRepository.existsByEmailIgnoreCase(request.email())) { ... }
        String hash = passwordEncoder.encode(request.password());
        // ...
    }
}
```

```java
// AuthControllerTest.java — override de dependencia en tests con Mockito
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;  // Mock de BD en memoria

    @InjectMocks
    private AuthService authService;        // Se inyecta el mock automáticamente

    @Test
    void register_shouldThrowWhenEmailExists() {
        when(userRepository.existsByEmailIgnoreCase("test@test.com")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.registerUser(request));
    }
}
```

### Ventaja

Para los tests se puede **reemplazar** cualquier dependencia con un mock sin
tocar el service. Spring se encarga de resolver el grafo de dependencias
en tiempo de ejecución.

---

## Patrón 4 — JWT Stateless

### ¿Qué es?

El servidor **no guarda sesión**. En cambio, emite un token firmado
criptográficamente que el cliente presenta en cada request. El servidor
solo verifica la firma con `JwtUtil`.

### Tokens del sistema

| Token           | Duración       | Propósito                                               |
| --------------- | -------------- | ------------------------------------------------------- |
| `access_token`  | **15 minutos** | Autenticar cada request a endpoints protegidos          |
| `refresh_token` | **7 días**     | Obtener un nuevo `access_token` sin volver a hacer login |

### Ejemplo en código

```java
// security/JwtUtil.java — creación del access token
public String generateAccessToken(String email) {
    // ¿Qué? Crea un JWT firmado con HS256 que expira en 15 minutos.
    // ¿Para qué? Permitir al cliente autenticar requests sin sesión en el servidor.
    // ¿Impacto? Si el secret es débil (<32 chars), el token puede ser forjado.
    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
}

// security/JwtUtil.java — verificación del token
public String extractEmail(String token) {
    try {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    } catch (JwtException e) {
        throw new AuthException("Token inválido o expirado");
    }
}
```

### Ventaja

El backend puede escalar a múltiples instancias sin compartir estado de sesión.
No hay tabla de sesiones. La información del usuario viaja **dentro** del token.

---

## Patrones 5, 6, 7 y 8 — Frontend React

Los patrones 5 al 8 aplican al frontend React y son **idénticos en todos los
proyectos de la serie** (FastAPI, Express, Spring Boot, etc.) porque el frontend
no cambia. Solo cambia el color de acento.

---

## Patrón 5 — Context / Provider

### ¿Qué es?

React usa el patrón **Provider** para compartir estado global sin necesidad de
pasar props manualmente por cada nivel del árbol de componentes.

### Ejemplo en código

```typescript
// context/AuthContext.tsx
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);

  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);
    setAccessToken(response.access_token);
    // ...
  };

  return (
    <AuthContext.Provider value={{ user, accessToken, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
```

```tsx
// main.tsx — AuthProvider envuelve toda la app
<AuthProvider>
  <BrowserRouter>
    <App />
  </BrowserRouter>
</AuthProvider>
```

### Ventaja

`DashboardPage`, `Navbar`, `ChangePasswordPage` **todos** acceden al mismo
estado de autenticación sin recibir props.

---

## Patrón 6 — Custom Hook

### ¿Qué es?

Una función de React que encapsula lógica reutilizable y puede usar otros
hooks internamente.

### Ejemplo en código

```typescript
// hooks/useAuth.ts
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth() debe usarse dentro de <AuthProvider>");
  }
  return context;
}
```

```typescript
// pages/DashboardPage.tsx — consumo del hook
export function DashboardPage() {
  const { user, logout } = useAuth();  // una línea, acceso completo al contexto
  return <h1>Bienvenido, {user?.fullName}</h1>;
}
```

### Ventaja

En lugar de escribir `useContext(AuthContext)` con su validación en cada componente,
se centraliza en `useAuth()`. Si el contexto cambia, **solo se modifica el hook**.

---

## Patrón 7 — Interceptor

### ¿Qué es?

Middleware a nivel de cliente HTTP que procesa **todas** las peticiones/respuestas
antes de que lleguen al código de la aplicación.

### Ejemplo en código

```typescript
// api/axios.ts
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL });

// Interceptor de request — adjunta el token automáticamente
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("access_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor de response — maneja errores de autenticación
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.clear();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);
```

### Ventaja

Ningún componente ni función de API necesita preocuparse por añadir el header
`Authorization`. Si el token cambia de lugar, se modifica **un solo lugar**.

---

## Patrón 8 — SPA + Route Guard

### ¿Qué es?

En una SPA (_Single Page Application_), el enrutamiento ocurre en el cliente
(JavaScript), sin recargar la página. El **Route Guard** protege rutas que
requieren autenticación.

### Ejemplo en código

```typescript
// components/ProtectedRoute.tsx
export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <LoadingSpinner />;

  // Si no está autenticado, redirige sin mostrar la página
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}
```

```tsx
// App.tsx — configuración de rutas
<Routes>
  {/* Rutas públicas */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />

  {/* Rutas protegidas — requieren autenticación */}
  <Route element={<ProtectedRoute />}>
    <Route path="/dashboard" element={<DashboardPage />} />
    <Route path="/change-password" element={<ChangePasswordPage />} />
  </Route>
</Routes>
```

### Ventaja

Un usuario que visita `/dashboard` sin autenticarse **nunca ve** el HTML de la
página. Es redirigido inmediatamente. No hay necesidad de protección en cada
componente individualmente.

---

## Patrón 9 — Monorepo

### ¿Qué es?

Múltiples proyectos (frontend, backend, infraestructura) conviven en
**un solo repositorio git**.

### Estructura

```
proyecto-be_springboot_java-fe_react/              ← Un solo repositorio git
├── be/                        ← Backend (Spring Boot / Java 21)
│   ├── src/
│   ├── pom.xml
│   └── mvnw
├── fe/                        ← Frontend (React / TypeScript)
│   ├── src/
│   └── package.json
├── docker-compose.yml         ← Infraestructura compartida
└── .github/
    └── copilot-instructions.md
```

### Ventaja

- Un `git clone` obtiene todo el proyecto
- Los cambios que afectan a backend **y** frontend viajan en el mismo commit
- La infraestructura (`docker-compose.yml`) es parte del código versionado
- El `README.md` único describe el setup completo del sistema

---

## Patrón 10 — REST API

### ¿Qué es?

Interfaz de comunicación basada en recursos HTTP con verbos (`GET`, `POST`,
`PUT`, `DELETE`) y códigos de estado estándar (`200`, `201`, `400`, `401`,
`404`, `422`).

### Endpoints del sistema

| Verbo  | Ruta                           | Código OK | Descripción                            |
| ------ | ------------------------------ | --------- | -------------------------------------- |
| `POST` | `/api/v1/auth/register`        | `201`     | Registrar nuevo usuario                |
| `POST` | `/api/v1/auth/login`           | `200`     | Iniciar sesión, obtener tokens         |
| `POST` | `/api/v1/auth/refresh`         | `200`     | Renovar access token                   |
| `POST` | `/api/v1/auth/change-password` | `200`     | Cambiar contraseña (auth requerida)    |
| `POST` | `/api/v1/auth/forgot-password` | `200`     | Solicitar email de recuperación        |
| `POST` | `/api/v1/auth/reset-password`  | `200`     | Restablecer contraseña con token       |
| `POST` | `/api/v1/auth/verify-email`    | `200`     | Verificar dirección de email           |
| `GET`  | `/api/v1/users/me`             | `200`     | Obtener perfil del usuario autenticado |

> Spring Boot corre en el puerto **8080** (a diferencia de FastAPI en 8000).
> Swagger UI disponible en `http://localhost:8080/swagger-ui.html`

### Ejemplo de respuesta estándar

```json
// POST /api/v1/auth/login → 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}

// POST /api/v1/auth/register → 400 Bad Request (email duplicado)
{
  "timestamp": "2026-03-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El email ya está registrado"
}

// POST /api/v1/auth/register → 422 Unprocessable Entity (validación Bean Validation)
{
  "timestamp": "2026-03-15T10:30:00Z",
  "status": 422,
  "errors": {
    "email": "Formato de email inválido",
    "password": "Debe contener al menos una mayúscula"
  }
}
```

### Ventaja

Cualquier cliente (React, Android, iOS, Postman, curl) puede consumir la API
porque habla HTTP estándar. La documentación es automática en `/swagger-ui.html`
(SpringDoc OpenAPI).

---

## Relación entre patrones

```
┌─────────────────────────────────────────────────────────────────┐
│ Monorepo (#9)                                                   │
│                                                                 │
│  ┌─── REST API (#10) ─────────────────────────────────────────┐ │
│  │                                                           │ │
│  │  Frontend (SPA #8)          Backend (Capas #1)            │ │
│  │ ┌────────────────────┐     ┌──────────────────────────┐   │ │
│  │ │ Provider (#5)      │     │ controller/              │   │ │
│  │ │  Hook (#6)         │←───→│  service/   ← DI (#3)    │   │ │
│  │ │  RouteGuard (#8)   │     │  repository/← JPA        │   │ │
│  │ │  Interceptor (#7)  │     │  entity/    ← DTO (#2)   │   │ │
│  │ └────────────────────┘     │  security/  ← JWT (#4)   │   │ │
│  │                            └──────────────┬───────────┘   │ │
│  │                                           ↓               │ │
│  │                                      PostgreSQL           │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

Cada patrón resuelve un problema específico. Juntos, hacen que el sistema sea:

- **Seguro** — DTO + JWT + BCrypt + Bucket4j (rate limiting)
- **Mantenible** — Capas + DI + CustomHook + Convenciones Java
- **Escalable** — Stateless + REST + Monorepo + JAR ejecutable
- **Testeable** — DI override + Mockito + BD de pruebas (db-test) + JaCoCo
