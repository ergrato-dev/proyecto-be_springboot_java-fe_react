# 🔐 NN Auth System

> Proyecto educativo — SENA | Marzo 2026

Sistema de autenticación completo para una empresa genérica "NN", diseñado como ejercicio formativo.
Incluye landing page pública, registro de usuarios, login, cambio de contraseña y recuperación por email.

> **Referencia:** Este proyecto es una reimplementación del sistema [proyecto-be_fastapi-fe_react](https://github.com/ergrato-dev/proyecto-be_fastapi-fe_react)
> con **idéntica funcionalidad**, usando un stack tecnológico diferente:
> **Java 21 + Spring Boot 3** en el backend (en lugar de Python + FastAPI).

---

## 📝 Antes de empezar

Este repo **no es solo para clonar y cambiar el look**. Antes de presentarlo como evidencia de
aprendizaje, completa la [**Bitácora obligatoria**](BITACORA.md) — es un checklist secuencial
que verifica, con commits de tu propio repo, que entendiste el flujo real (arquitectura, auth,
base de datos) y no solo la apariencia.

---

## 📋 Tabla de Contenidos

- [🛠️ Stack Tecnológico](#️-stack-tecnológico)
- [✅ Prerrequisitos](#-prerrequisitos)
  - [Instalar Maven Wrapper (incluido en el proyecto)](#instalar-maven-wrapper-incluido-en-el-proyecto)
  - [Instalar pnpm (si no lo tienes)](#instalar-pnpm-si-no-lo-tienes)
- [🚀 Instalación y Setup — Con Docker](#-instalación-y-setup--con-docker)
  - [1. Clonar el repositorio](#1-clonar-el-repositorio)
  - [2. Levantar la base de datos (Docker)](#2-levantar-la-base-de-datos-docker)
  - [3. Configurar el Backend](#3-configurar-el-backend)
  - [4. Configurar el Frontend](#4-configurar-el-frontend)
- [🐧 Instalación y Setup — Sin Docker](#-instalación-y-setup--sin-docker)
- [▶️ Ejecución](#️-ejecución)
- [🧪 Testing](#-testing)
- [📁 Estructura del Proyecto](#-estructura-del-proyecto)
- [📏 Convenciones](#-convenciones)
- [📚 Documentación Adicional](#-documentación-adicional)
- [🎓 Propósito Educativo](#-propósito-educativo)
- [⚠️ Exención de Responsabilidades](#️-exención-de-responsabilidades)
- [📄 Licencia](#-licencia)

---

## 🛠️ Stack Tecnológico

| Capa            | Tecnología                                                                  |
|-----------------|-----------------------------------------------------------------------------|
| **Backend**     | Java 21 (JDK 21), Spring Boot 3.5.0, Spring Data JPA, Spring Security (BCrypt), Flyway 10 |
| **Frontend**    | React 19.2.4, Vite 8.0.7, TypeScript 6.0.2, TailwindCSS 4.2.2              |
| **Router FE**   | React Router DOM 7.14.0                                                     |
| **HTTP client** | Axios 1.14.0 (interceptor Bearer JWT)                                       |
| **Base de datos** | PostgreSQL 17 (Docker Compose o instalación local)                        |
| **Auth**        | JWT — JJWT 0.12.6 (access 15 min + refresh 7 días, HS256)                  |
| **Email (dev)** | Mailpit — captura SMTP local, UI en puerto 8025                             |
| **Rate Limiting** | Bucket4j 8.x — límite por IP en endpoints de auth                        |
| **Documentación API** | SpringDoc OpenAPI 2.8.9 (Swagger UI en `/swagger-ui.html`)         |
| **Testing BE**  | JUnit 5 + MockMvc + PostgreSQL de pruebas (db-test) → 29 tests            |
| **Testing FE**  | Vitest 4.1.3 + Testing Library React 16.3.2 → 37 tests (6 suites)         |
| **Linting**     | Checkstyle (Java), ESLint 10 + Prettier 3.8.1 (TypeScript)                 |
| **Build**       | Maven Wrapper (`./mvnw`) para BE, pnpm 11 para FE                          |

---

## ✅ Prerrequisitos

Antes de comenzar, asegúrate de tener instalado:

| Herramienta     | Versión mínima | Verificar con          |
|-----------------|---------------|------------------------|
| JDK             | 21+           | `java -version`        |
| Maven           | 3.9+          | `mvn -version` (o usar `./mvnw`) |
| Node.js         | 20 LTS+       | `node --version`       |
| pnpm            | 9+            | `pnpm --version`       |
| Docker          | 24+           | `docker --version`     |
| Docker Compose  | 2.20+         | `docker compose version` |
| Git             | 2.40+         | `git --version`        |

> ⚠️ **Importante:** Usar **pnpm** como gestor de paquetes de Node.js. Nunca usar `npm` ni `yarn`.

> 🖥️ **Usuarios de Windows** — Usar siempre **Git Bash** como terminal.
> Los comandos de este proyecto usan sintaxis Bash (`source`, `export`, `/`, etc.).
> No usar CMD ni PowerShell — los comandos no funcionarán igual.

### Instalar pnpm (si no lo tienes)

```bash
# Opción recomendada — vía corepack (incluido con Node.js 16+)
corepack enable
corepack prepare pnpm@11.20.0 --activate

# Alternativa — instalación independiente
curl -fsSL https://get.pnpm.io/install.sh | sh -
```

---

## 🚀 Instalación y Setup — Con Docker

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd proyecto-be_springboot_java-fe_react
```

### 2. Levantar la base de datos (Docker)

```bash
# Inicia PostgreSQL 17 + Mailpit (captura de emails) en contenedores Docker
docker compose up -d

# Verificar que están corriendo
docker compose ps
# Deberías ver nn_auth_db y nn_auth_mailpit con estado "healthy"
```

### 3. Configurar el Backend

```bash
cd be

# Copiar y configurar variables de entorno
cp .env.example .env
# Editar .env con tus valores si es necesario

# Verificar que el JDK 21 está activo
java -version   # debe mostrar "21.x.x"

# Compilar y ejecutar las migraciones de Flyway (se ejecutan automáticamente al iniciar)
./mvnw spring-boot:run
# La app arranca, Flyway aplica las migraciones y el servidor queda escuchando
```

### 4. Configurar el Frontend

```bash
cd fe

# Instalar dependencias con pnpm (¡NUNCA con npm!)
pnpm install

# Copiar y configurar variables de entorno
cp .env.example .env
```

---

## 🐧 Instalación y Setup — Sin Docker

Para usar el proyecto sin Docker, se necesita una instancia de PostgreSQL instalada localmente.

### Instalar PostgreSQL localmente

```bash
# Ubuntu / Debian
sudo apt update && sudo apt install -y postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql

# macOS (vía Homebrew)
brew install postgresql@17
brew services start postgresql@17

# Windows — descargar el instalador oficial en:
# https://www.postgresql.org/download/windows/
```

### Crear la base de datos y usuario

```sql
-- Conectarse como superusuario
sudo -u postgres psql   # Linux
psql -U postgres        # macOS / Windows

-- Ejecutar en psql:
CREATE USER nn_user WITH PASSWORD 'nn_password';
CREATE DATABASE nn_auth_db OWNER nn_user;
GRANT ALL PRIVILEGES ON DATABASE nn_auth_db TO nn_user;
\q
```

### Configurar el Backend (sin Docker)

```bash
cd be

# Copiar variables de entorno
cp .env.example .env

# Editar .env — apuntar a la BD local (no al contenedor Docker)
# DB_HOST=localhost
# DB_PORT=5432
# DB_NAME=nn_auth_db
# DB_USERNAME=nn_user
# DB_PASSWORD=nn_password

# Para email en desarrollo sin Mailpit Docker, usar el perfil "noDocker"
# que configura un MockMailSender que imprime emails en consola
./mvnw spring-boot:run -Dspring-boot.run.profiles=noDocker
```

### Configurar el Frontend (sin Docker)

```bash
cd fe
pnpm install
cp .env.example .env
# El frontend no depende de Docker — conecta siempre al backend por HTTP
```

> 📧 **Email en modo noDocker:** Sin Mailpit corriendo, los emails de verificación y
> recuperación de contraseña se **imprimen en la consola del backend** para facilitar
> el desarrollo. Busca en los logs la línea `[MOCK EMAIL]` con el enlace de verificación.

---

## ▶️ Ejecución

### Levantar todo el sistema (3 terminales)

```bash
# Terminal 1 — Base de datos (solo con Docker; si usas instalación local, ya corre como servicio)
docker compose up -d

# Terminal 2 — Backend (Spring Boot)
cd be
./mvnw spring-boot:run
# → API disponible en http://localhost:8080
# → Swagger UI en http://localhost:8080/swagger-ui.html  (solo si ENVIRONMENT=development)
# → Alternativa: http://localhost:8080/api-docs (OpenAPI JSON)

# Terminal 3 — Frontend (React + Vite)
cd fe && pnpm dev
# → Landing page en http://localhost:5173
# → App disponible en http://localhost:5173
```

> 📧 **Mailpit** — bandeja de entrada de emails de desarrollo: `http://localhost:8025`
> Aquí se capturan los emails de verificación de cuenta y recuperación de contraseña.

---

## 🧪 Testing

### Backend

```bash
# BD de pruebas desechable (una vez por sesión de trabajo)
docker compose up -d --wait db-test

cd be
export TEST_DATABASE_URL='jdbc:postgresql://localhost:5433/nn_auth_test?user=nn_user&password=nn_password'

# Ejecutar todos los tests
./mvnw test

# Ejecutar con reporte de cobertura (JaCoCo)
./mvnw verify

# Ver reporte de cobertura
# Abre en el navegador: target/site/jacoco/index.html

# Ejecutar tests de una clase específica
./mvnw test -Dtest=AuthControllerTest
```

> ⚠️ Los tests del backend usan una **BD exclusiva** (`db-test`, puerto 5433), nunca la de
> desarrollo. La URL llega por `TEST_DATABASE_URL`: sin ella, el contexto de Spring no arranca
> y los tests se detienen antes de conectarse.

### Frontend

```bash
cd fe

# Ejecutar todos los tests
pnpm test

# Ejecutar en modo watch
pnpm test:watch

# Ejecutar con cobertura
pnpm test:coverage
```

### Linting

```bash
# Backend — Checkstyle
cd be && ./mvnw checkstyle:check

# Frontend — ESLint + Prettier
cd fe && pnpm lint && pnpm format
```

---

## 📁 Estructura del Proyecto

```
proyecto-be_springboot_java-fe_react/
├── .github/copilot-instructions.md    # Reglas y convenciones del proyecto
├── .gitignore                         # Archivos ignorados por git
├── docker-compose.yml                 # PostgreSQL 17 + Mailpit para desarrollo
├── README.md                          # ← Este archivo
├── docs/                             # Documentación técnica
│   ├── referencia-tecnica/
│   │   ├── architecture.md            # Arquitectura general, flujos y decisiones técnicas
│   │   ├── api-endpoints.md           # Todos los endpoints con parámetros y respuestas
│   │   ├── database-schema.md         # Esquema ER, tablas, columnas y migraciones
│   │   └── design-system.md           # Sistema de tokens accent-* (amber) y temas dark/light
│   ├── conceptos/
│   │   ├── owasp-top-10.md            # Implementación del OWASP Top 10 2021
│   │   ├── accesibilidad-aria-wcag.md # Estándares ARIA/WCAG 2.1 AA aplicados
│   │   └── patrones-arquitectonicos.md # Patrones usados: Repository, DTO, Context, etc.
│   └── requisitos/
│       ├── restricciones.md           # Restricciones técnicas y de negocio
│       ├── RFs/                       # Requisitos funcionales (RF-001 a RF-008)
│       └── HUs/                       # Historias de usuario (HU-001 a HU-008)
│
├── be/                                # Backend — Java 21 + Spring Boot 3
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nn/auth/
│   │   │   │   ├── NnAuthApplication.java          # Punto de entrada
│   │   │   │   ├── config/                         # Configuración Spring
│   │   │   │   │   ├── ApplicationConfig.java      # Beans globales (BCrypt, etc.)
│   │   │   │   │   ├── SecurityConfig.java         # Filtros, CORS, endpoints públicos
│   │   │   │   │   └── OpenApiConfig.java          # Swagger (on/off según perfil)
│   │   │   │   ├── controller/                     # Capa HTTP (REST)
│   │   │   │   │   ├── AuthController.java         # POST /api/v1/auth/*
│   │   │   │   │   └── UserController.java         # GET /api/v1/users/me
│   │   │   │   ├── service/                        # Lógica de negocio
│   │   │   │   │   └── AuthService.java
│   │   │   │   ├── repository/                     # Spring Data JPA
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── PasswordResetTokenRepository.java
│   │   │   │   │   └── EmailVerificationTokenRepository.java
│   │   │   │   ├── entity/                         # Entidades JPA (tablas de BD)
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── PasswordResetToken.java
│   │   │   │   │   └── EmailVerificationToken.java
│   │   │   │   ├── dto/                            # Data Transfer Objects
│   │   │   │   │   ├── request/                    # Schemas de entrada (Bean Validation)
│   │   │   │   │   └── response/                   # Schemas de salida
│   │   │   │   ├── security/                       # JWT y filtros de seguridad
│   │   │   │   │   ├── JwtUtil.java                # Crear y verificar tokens
│   │   │   │   │   └── JwtAuthenticationFilter.java # OncePerRequestFilter
│   │   │   │   ├── exception/                      # Manejo global de errores
│   │   │   │   │   └── GlobalExceptionHandler.java # @ControllerAdvice
│   │   │   │   └── util/                           # Utilidades transversales
│   │   │   │       ├── EmailService.java           # Envío de emails (JavaMailSender)
│   │   │   │       └── AuditLogger.java            # Logging estructurado JSON
│   │   │   └── resources/
│   │   │       ├── application.yml                 # Config principal
│   │   │       ├── application-docker.yml          # Perfil Docker
│   │   │       ├── application-noDocker.yml        # Perfil sin Docker
│   │   │       └── db/migration/                   # Scripts Flyway (V1__, V2__, ...)
│   │   └── test/java/com/nn/auth/                  # Tests con JUnit 5 + MockMvc
│   ├── .env                                        # Variables de entorno (NO versionado)
│   ├── .env.example                                # Plantilla de variables
│   ├── pom.xml                                     # Dependencias Maven
│   └── mvnw / mvnw.cmd                             # Maven Wrapper
│
└── fe/                                # Frontend — React + Vite + TypeScript
    ├── src/
    │   ├── api/                       # Clientes HTTP
    │   ├── components/                # Componentes reutilizables (UI + layout)
    │   ├── pages/                     # Páginas/vistas (una por ruta)
    │   ├── hooks/                     # Custom hooks
    │   ├── context/                   # Context providers (AuthContext)
    │   ├── types/                     # Tipos TypeScript (auth.ts)
    │   └── __tests__/                 # Tests Vitest (Button, InputField, Alert, Login, Register, useAuth)
    ├── src/test/setup.ts              # Setup global: jest-dom matchers + afterEach cleanup
    ├── .env                           # Variables de entorno (NO versionado)
    ├── .env.example                   # Plantilla de variables
    ├── package.json                   # Dependencias (pnpm, versiones exactas)
    ├── vitest.config.ts               # Configuración de Vitest (jsdom, setupFiles)
    └── vite.config.ts                 # Configuración de Vite
```

---

## 📏 Convenciones

| Aspecto              | Convención                                                           |
|----------------------|----------------------------------------------------------------------|
| Nomenclatura técnica | Inglés (variables, funciones, clases, endpoints, tablas, columnas)   |
| Comentarios / docs   | Español (con ¿Qué? ¿Para qué? ¿Impacto?)                            |
| Commits              | Conventional Commits en inglés + What/For/Impact                    |
| Java                 | Google Java Style Guide + Checkstyle + Javadoc en español           |
| TypeScript           | strict mode + ESLint + Prettier                                      |
| Gestor de build      | Maven Wrapper (`./mvnw`) para Java, pnpm para Node.js                |
| Testing              | Código generado = código probado                                     |

Para las reglas completas, ver [.github/copilot-instructions.md](.github/copilot-instructions.md).

---

## 📚 Documentación Adicional

| Documento                                       | Descripción                                                  |
|-------------------------------------------------|--------------------------------------------------------------|
| [BITACORA.md](BITACORA.md)                       | Checklist obligatorio de aprendizaje, fase por fase          |
| [AUDITORIA.md](AUDITORIA.md)                     | Auditoría de pertinencia/relevancia/completitud/actualidad/seguridad |
| [docs/referencia-tecnica/architecture.md](docs/referencia-tecnica/architecture.md)     | Arquitectura de 3 capas, flujos auth, decisiones técnicas |
| [docs/referencia-tecnica/api-endpoints.md](docs/referencia-tecnica/api-endpoints.md)   | 8 endpoints con parámetros, respuestas, rate limiting y JWT |
| [docs/referencia-tecnica/database-schema.md](docs/referencia-tecnica/database-schema.md) | Esquema ER, tablas, columnas y migraciones Flyway |
| [docs/referencia-tecnica/design-system.md](docs/referencia-tecnica/design-system.md)   | Tokens `accent-*` amber, dark mode, logo SVG |
| [docs/conceptos/owasp-top-10.md](docs/conceptos/owasp-top-10.md)                       | Implementación del OWASP Top 10 2021 con Spring Boot |
| [docs/conceptos/accesibilidad-aria-wcag.md](docs/conceptos/accesibilidad-aria-wcag.md) | ARIA/WCAG 2.1 AA implementados en el frontend React |
| [docs/conceptos/patrones-arquitectonicos.md](docs/conceptos/patrones-arquitectonicos.md) | Patrones Repository, DTO, Context, Interceptor, etc. |
| [docs/requisitos/restricciones.md](docs/requisitos/restricciones.md)                   | Restricciones técnicas y de negocio del proyecto |
| [.github/copilot-instructions.md](.github/copilot-instructions.md)                       | Reglas y convenciones del proyecto (stack, commits, etc.) |

---

## 🎓 Propósito Educativo

Este proyecto está diseñado para aprender haciendo. Cada archivo, clase y componente incluye
comentarios pedagógicos que explican:

- **¿Qué?** — Qué hace este código
- **¿Para qué?** — Por qué existe y cuál es su propósito
- **¿Impacto?** — Qué pasa si no existiera o si se implementa mal

> "La calidad no es una opción, es una obligación."

---

## ⚠️ Exención de Responsabilidades

Este proyecto es de naturaleza exclusivamente educativa, desarrollado como ejercicio formativo en el marco del SENA.

- **No apto para producción** — El sistema no ha sido auditado ni endurecido para entornos productivos
  reales. No debe usarse para proteger datos sensibles de usuarios reales sin una revisión de seguridad
  profesional previa.
- **Credenciales de ejemplo** — Las contraseñas, claves secretas y cadenas de conexión presentes en
  `.env.example` y en la documentación son únicamente ilustrativas. Nunca usarlas en producción.
- **Sin garantía de disponibilidad** — El proyecto puede contener bugs o comportamientos no documentados
  propios de un entorno de aprendizaje.
- **Uso de terceros** — El proyecto referencia servicios externos (Resend, Neon, Supabase, Railway)
  como ejemplos pedagógicos. El autor no tiene afiliación con dichos servicios ni garantiza su
  disponibilidad o condiciones de uso.
- **Responsabilidad del aprendiz** — Cada aprendiz es responsable de comprender el código que ejecuta
  en su equipo y de no reutilizarlo sin entenderlo completamente.

> Este material se provee "tal cual", sin garantías explícitas ni implícitas de ningún tipo.

---

## 📄 Licencia

[![CC BY-NC-SA 4.0](https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc-sa/4.0/)

Este proyecto está licenciado bajo **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)**. Proyecto educativo — SENA. Uso exclusivamente académico.

Consulta el archivo [LICENSE](./LICENSE) o visita [creativecommons.org/licenses/by-nc-sa/4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) para más información.
