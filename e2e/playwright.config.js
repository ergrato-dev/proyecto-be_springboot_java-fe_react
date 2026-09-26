/**
 * Archivo: e2e/playwright.config.js
 * Descripción: Configuración de Playwright para los tests E2E del sistema completo.
 * ¿Para qué? Levantar backend y frontend, y correr los flujos críticos en un navegador real.
 * ¿Impacto? Con un solo `pnpm test` se prueba la app de punta a punta. Antes hay que levantar
 *   la BD de pruebas y Mailpit: docker compose up -d --wait db-test mailpit
 */

import { defineConfig, devices } from "@playwright/test";

// ¿Qué? Puertos del frontend y del backend. Cámbialos si ya están ocupados en tu equipo.
const frontPort = Number(process.env.FRONT_PORT ?? 5173);
const apiPort = Number(process.env.API_PORT ?? 8080);
const frontUrl = `http://localhost:${frontPort}`;
const apiUrl = `http://localhost:${apiPort}`;

export default defineConfig({
  testDir: "./tests",
  use: {
    baseURL: frontUrl,
    // ¿Qué? La app detecta el idioma del navegador; los tests esperan los textos en español.
    locale: "es-CO",
    // ¿Qué? Guarda la traza cuando un test falla y se reintenta: se abre con `pnpm report`.
    trace: "on-first-retry",
  },
  reporter: [["list"], ["html", { open: "never" }]],
  retries: process.env.CI ? 1 : 0,
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],

  // ¿Qué? Playwright levanta backend y frontend antes de los tests y los apaga al terminar.
  // ¿Impacto? En tu equipo reutiliza los que ya estén corriendo (reuseExistingServer).
  webServer: [
    {
      // ¿Qué? Compila el jar y arranca Spring Boot; Flyway migra la BD al iniciar.
      //   `exec` deja a java como proceso principal, así Playwright lo detiene al terminar.
      command: `sh -c "./mvnw -B -q -DskipTests package && exec java -jar target/auth-0.0.1-SNAPSHOT.jar --server.port=${apiPort}"`,
      cwd: "../be",
      // ¿Qué? Ruta pública que responde cuando la app ya arrancó.
      url: `${apiUrl}/v3/api-docs`,
      // La primera compilación descarga dependencias: puede tardar más de un minuto.
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
      env: {
        // BD de pruebas desechable (servicio db-test), nunca la de desarrollo.
        DB_PORT: "5433",
        DB_NAME: "nn_auth_test",
        // Valor de prueba, nunca el de producción (mínimo 32 caracteres).
        JWT_SECRET: process.env.JWT_SECRET ?? "e2e-secret-key-not-for-production-32chars",
        // Los tests registran e inician sesión muchas veces desde la misma IP: sin esto,
        // el rate limit (10 intentos cada 15 minutos) los haría fallar al azar.
        RATE_LIMIT_ENABLED: "false",
        // Los correos van a Mailpit; los tests leen el enlace de verificación desde su API.
        MAIL_HOST: "localhost",
        MAIL_PORT: "1025",
        FRONTEND_URL: frontUrl,
      },
    },
    {
      // Se lanza vite con node, sin pnpm de por medio, para que Playwright pueda detenerlo.
      command: `node node_modules/vite/bin/vite.js --port ${frontPort} --strictPort`,
      cwd: "../fe",
      url: frontUrl,
      reuseExistingServer: !process.env.CI,
      env: { VITE_API_BASE_URL: apiUrl },
    },
  ],
});
