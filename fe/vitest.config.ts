/**
 * Archivo: vitest.config.ts
 * Descripción: Configuración de Vitest — el test runner compatible con Vite.
 * ¿Para qué? Configurar el entorno de testing (jsdom para simular el DOM del
 *            navegador), el archivo de setup global y los plugins para
 *            transformar JSX/TypeScript en los tests.
 * ¿Impacto? Sin este archivo, Vitest usaría sus defaults (sin jsdom),
 *           haciendo que tests de componentes React fallen porque `document`
 *           y `window` no existen en el entorno Node.js puro.
 */
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [
    // Plugin para transformar JSX/TSX — necesario para que Vitest entienda
    // los archivos .tsx de los componentes y páginas a testear.
    // No incluimos TailwindCSS aquí porque los tests no necesitan procesar CSS.
    react(),
  ],
  test: {
    // jsdom: simula el DOM del navegador (document, window, localStorage, etc.)
    // dentro de Node.js. Sin esto, los componentes React no pueden renderizarse
    // porque presuponen la existencia de un DOM.
    environment: "jsdom",

    // Archivo que se ejecuta ANTES de cada suite de tests.
    // Aquí registramos los matchers de @testing-library/jest-dom
    // (toBeInTheDocument, toHaveTextContent, toBeDisabled, etc.)
    setupFiles: ["./src/test/setup.ts"],

    coverage: {
      provider: "v8",
      // ¿Qué? "html" genera coverage/index.html, que el CI sube como artefacto.
      reporter: ["text", "text-summary", "html"],
      include: ["src/**/*.{ts,tsx}"],
      // ¿Qué? Se mide la lógica de la interfaz. Quedan fuera los tests, los tipos, el
      //       arranque (main.tsx, App.tsx con las rutas), la configuración de idiomas
      //       (i18n.ts) y el cliente HTTP (src/api), que se prueba con MSW a nivel de red.
      exclude: [
        "src/__tests__/**",
        "src/test/**",
        "src/types/**",
        "src/**/*.d.ts",
        "src/main.tsx",
        "src/App.tsx",
        "src/i18n.ts",
        "src/api/**",
      ],
      // ¿Qué? Umbral mínimo: `pnpm test:coverage` falla si la cobertura baja de aquí.
      // ¿Impacto? Es la cobertura real redondeada hacia abajo (regla de trinquete): solo
      //           sube, PR a PR, hasta el 80%. Sin tests aún: Dashboard, ChangePassword,
      //           ForgotPassword, ResetPassword, VerifyEmail y AuthContext.
      thresholds: { statements: 33, branches: 36, functions: 30, lines: 33 },
    },
  },
});
