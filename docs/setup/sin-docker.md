# Setup sin Docker — NN Auth System

> **¿Para qué este archivo?**
> Guía paso a paso para poner en marcha el sistema sin usar Docker,
> instalando PostgreSQL directamente en tu máquina y usando el perfil
> `noDocker` del backend para el manejo de emails de desarrollo.
>
> **¿Cuándo usarlo?**
> Cuando Docker no está disponible (máquinas con poca RAM, entornos de laboratorio,
> restricciones corporativas) o cuando se prefiere controlar directamente
> la instancia de base de datos.
>
> **¿Impacto?**
> Sin Mailpit, los emails de verificación y recuperación **no se envían**
> al usuario — en cambio, el backend los **imprime en la consola** con la línea
> `[MOCK EMAIL]` para que puedas copiar el enlace durante el desarrollo.

---

## Prerrequisitos

| Herramienta   | Versión mínima | Verificar con       |
| ------------- | -------------- | ------------------- |
| JDK           | 21+            | `java -version`     |
| Maven Wrapper | incluido       | `./mvnw --version`  |
| Node.js       | 20 LTS+        | `node --version`    |
| pnpm          | 9+             | `pnpm --version`    |
| PostgreSQL    | 15+ (recom. 17)| `psql --version`    |
| Git           | 2.40+          | `git --version`     |

> ⚠️ **Windows:** Usar siempre **Git Bash** como terminal. CMD y PowerShell no son compatibles con los comandos de este proyecto (`source`, `export`, rutas con `/`).

---

## Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/ergrato-dev/proyecto-be_springboot_java-fe_react.git
cd proyecto-be_springboot_java-fe_react
```

---

## Paso 2 — Instalar PostgreSQL

### Ubuntu / Debian

```bash
# Actualizar lista de paquetes e instalar PostgreSQL
sudo apt update && sudo apt install -y postgresql postgresql-contrib

# Iniciar el servicio y habilitarlo para que arranque con el sistema
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verificar que está corriendo
sudo systemctl status postgresql
# Debe mostrar: Active: active (running)
```

### macOS (Homebrew)

```bash
# Instalar PostgreSQL 17 vía Homebrew
brew install postgresql@17

# Iniciar el servicio
brew services start postgresql@17

# Verificar la versión
psql --version
```

### Windows

1. Descargar el instalador oficial desde: <https://www.postgresql.org/download/windows/>
2. Ejecutar el instalador — usar el puerto por defecto `5432`.
3. Anotar la contraseña del usuario `postgres` que se configure durante la instalación.
4. Asegurarse de que el servicio `postgresql-x64-17` esté en estado **Iniciado** en el Panel de servicios de Windows.

---

## Paso 3 — Crear la base de datos y el usuario

Conectarse a PostgreSQL como superusuario y ejecutar los siguientes comandos SQL:

```bash
# Linux — conectar como el usuario postgres del sistema
sudo -u postgres psql

# macOS — conectar como tu usuario actual (Homebrew no crea usuario postgres del sistema)
psql postgres

# Windows (Git Bash) — conectar con el usuario postgres
psql -U postgres
```

Una vez dentro del prompt `psql`, ejecutar:

```sql
-- Crear el usuario de la aplicación
CREATE USER nn_user WITH PASSWORD 'nn_password';

-- Crear la base de datos asignándola al usuario creado
CREATE DATABASE nn_auth_db OWNER nn_user;

-- Otorgar todos los permisos sobre la base de datos al usuario
GRANT ALL PRIVILEGES ON DATABASE nn_auth_db TO nn_user;

-- Verificar que la BD existe
\l nn_auth_db

-- Salir de psql
\q
```

### Verificar la conexión

```bash
# Conectarse con el usuario de la aplicación para confirmar que funciona
psql -U nn_user -d nn_auth_db -h localhost
# Si conecta sin error, el usuario y la BD están correctamente configurados
\q
```

---

## Paso 4 — Configurar el Backend

```bash
cd be

# Copiar el archivo de variables de entorno
cp .env.example .env
```

Abrir `be/.env` y verificar que los valores apuntan a la BD local:

```bash
# be/.env — valores para instalación local (sin Docker)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nn_auth_db
DB_USERNAME=nn_user
DB_PASSWORD=nn_password     # La misma contraseña usada en el paso anterior

MAIL_HOST=localhost          # Sin Mailpit; el perfil noDocker imprime en consola
MAIL_PORT=1025

FRONTEND_URL=http://localhost:5173
ENVIRONMENT=development
SWAGGER_UI_ENABLED=true
```

> Si en el paso 3 usaste una contraseña diferente para `nn_user`, actualizar `DB_PASSWORD` en `.env`.

### Encontrar la ruta del JDK 21

```bash
# Linux — buscar el ejecutable javac (compilador, solo presente en el JDK, no en el JRE)
find /usr/lib/jvm -name "javac" 2>/dev/null
# Ejemplo de salida: /usr/lib/jvm/java-21-openjdk-amd64/bin/javac
# → JAVA_HOME sería: /usr/lib/jvm/java-21-openjdk-amd64

# macOS
/usr/libexec/java_home -v 21

# Windows (Git Bash)
which java   # muestra la ruta del java en uso
```

Actualizar `JAVA_HOME` en `be/.env` con la ruta del JDK 21 (sin el `/bin/javac` al final):

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # ajustar según tu sistema
```

### Arrancar el backend en modo noDocker

El perfil `noDocker` configura el backend para que, cuando no haya un servidor SMTP disponible,
los emails se **impriman en la consola** en lugar de fallar la petición.

```bash
# Desde la carpeta be/
./mvnw spring-boot:run -Dspring-boot.run.profiles=noDocker
```

Al arrancar, Spring Boot:

1. Lee `be/.env` y conecta a PostgreSQL en `localhost:5432`.
2. Flyway aplica las migraciones SQL creando las tres tablas (`users`, `password_reset_tokens`, `email_verification_tokens`).
3. La API queda disponible en `http://localhost:8080`.

Mensajes esperados en consola:

```
Flyway Community Edition ... by Redgate
Database: jdbc:postgresql://localhost:5432/nn_auth_db
Successfully applied 3 migrations to schema "public"
Started NnAuthApplication in X.XXX seconds
```

> **Swagger UI** (solo en development): `http://localhost:8080/swagger-ui.html`

---

## Paso 5 — Ver emails en la consola (modo noDocker)

Sin Mailpit, los emails aparecen en la consola del backend con el prefijo `[MOCK EMAIL]`.

Ejemplo al registrar un usuario:

```
[MOCK EMAIL] ─────────────────────────────────────────
To:      nuevo@usuario.com
Subject: Verifica tu dirección de email — NN Auth System
Body:
  Hola Ana,

  Para verificar tu cuenta, haz clic en el siguiente enlace:
  http://localhost:5173/verify-email?token=abc123def456...

  Este enlace expira en 24 horas.
[MOCK EMAIL] ─────────────────────────────────────────
```

Copiar el token de la URL y usarlo en:
- El navegador: `http://localhost:5173/verify-email?token=<token>`
- O directamente en Swagger UI: `POST /api/v1/auth/verify-email` con `{ "token": "<token>" }`

---

## Paso 6 — Configurar el Frontend

```bash
# Desde la raíz del proyecto
cd fe

# Instalar dependencias (¡SIEMPRE con pnpm, nunca con npm!)
pnpm install

# Copiar variables de entorno
cp .env.example .env
```

El `.env` del frontend no depende de Docker:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### Arrancar el frontend

```bash
pnpm dev
```

La app queda disponible en `http://localhost:5173`.

---

## Resumen — 2 terminales (sin Docker)

| Terminal | Directorio | Comando                                                          | URL resultado           |
| -------- | ---------- | ---------------------------------------------------------------- | ----------------------- |
| 1        | `be/`      | `./mvnw spring-boot:run -Dspring-boot.run.profiles=noDocker`     | `http://localhost:8080` |
| 2        | `fe/`      | `pnpm dev`                                                       | `http://localhost:5173` |

> No se necesita una terminal para la BD — PostgreSQL corre como servicio del sistema operativo.

---

## Verificar que todo funciona

```bash
# 1. ¿PostgreSQL está corriendo?
sudo systemctl status postgresql   # Linux
brew services list | grep postgresql  # macOS

# 2. ¿El backend responde?
curl http://localhost:8080/actuator/health
# Respuesta esperada: {"status":"UP"}

# 3. ¿El frontend carga?
# Abrir http://localhost:5173 en el navegador

# 4. ¿Los emails aparecen en consola?
# Registrar un usuario en http://localhost:5173/register
# Revisar la terminal del backend — debe aparecer [MOCK EMAIL] con el enlace
```

---

## Testing del Backend sin Docker

Todos los tests del backend usan PostgreSQL real. Con Docker, la BD de pruebas es el servicio
`db-test` (`docker compose up -d --wait db-test`). Sin Docker, crea en tu PostgreSQL local una
BD **distinta** a la de desarrollo y apunta los tests a ella:

```bash
# Una BD solo para tests — nunca la de desarrollo
createdb -U nn_user nn_auth_test

export TEST_DATABASE_URL='jdbc:postgresql://localhost:5432/nn_auth_test?user=nn_user&password=nn_password'
./mvnw test
```

> Sin `TEST_DATABASE_URL`, el contexto de Spring no arranca y los tests se detienen antes de
> conectarse. Ver [con-docker.md](./con-docker.md) para la configuración con Docker.

---

## Solución de problemas frecuentes

### "Connection refused" al arrancar el backend

**Causa:** PostgreSQL no está corriendo.

```bash
# Linux — iniciar el servicio
sudo systemctl start postgresql

# macOS
brew services start postgresql@17
```

### Error de autenticación: "FATAL: password authentication failed for user nn_user"

**Causa:** La contraseña en `be/.env` no coincide con la del usuario en PostgreSQL.

```bash
# Conectarse como superusuario y cambiar la contraseña
sudo -u postgres psql
ALTER USER nn_user WITH PASSWORD 'nn_password';
\q
```

### Error de autenticación local (Linux): "FATAL: Peer authentication failed"

**Causa:** PostgreSQL en Linux usa `peer` authentication por defecto para conexiones locales.
Para conectarse con usuario/contraseña en lugar del usuario del sistema, se necesita `md5`.

```bash
# Encontrar pg_hba.conf
sudo find /etc/postgresql -name "pg_hba.conf"
# Ejemplo: /etc/postgresql/17/main/pg_hba.conf

# Editar el archivo (como superusuario)
sudo nano /etc/postgresql/17/main/pg_hba.conf

# Cambiar la línea de 'local' para que use 'md5' en vez de 'peer':
# ANTES:  local   all             all                                     peer
# DESPUÉS: local  all             all                                     md5

# Reiniciar PostgreSQL para aplicar el cambio
sudo systemctl restart postgresql
```

### "JAVA_HOME is not defined correctly"

```bash
# Encontrar la ruta del JDK 21
find /usr/lib/jvm -name "javac" 2>/dev/null

# Actualizar en be/.env
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # ajustar según tu sistema
```

### Los emails no aparecen en consola

**Causa:** El perfil `noDocker` no está activo — el backend intenta conectarse al puerto SMTP 1025
y falla silenciosamente.

```bash
# Asegurarse de pasar el perfil correcto al arrancar
./mvnw spring-boot:run -Dspring-boot.run.profiles=noDocker
```

---

## Siguiente paso

- Ver [con-docker.md](./con-docker.md) para configurar el entorno recomendado con Docker.
- Explorar la API en `http://localhost:8080/swagger-ui.html`.
- Revisar [architecture.md](../referencia-tecnica/architecture.md) para entender la arquitectura del sistema.
