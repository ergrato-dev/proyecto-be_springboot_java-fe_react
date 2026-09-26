/**
 * Archivo: e2e/tests/auth.spec.js
 * Descripción: Flujos críticos de autenticación en un navegador real, con backend, BD y Mailpit.
 */

import { expect, test } from "@playwright/test";
import { AuthPages } from "./support/auth-pages.js";
import { apiUrl } from "./support/env.js";
import { findLinkInEmail } from "./support/mailpit.js";

const password = "Segura1234";

// ¿Qué? Correo único por test: los tests corren en paralelo sobre la misma BD.
function uniqueEmail() {
  return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@nn-company.com`;
}

test("should register, verify the email from Mailpit and reach the dashboard", async ({
  page,
  request,
}) => {
  // Arrange
  const auth = new AuthPages(page);
  const email = uniqueEmail();

  // Act: registro por la UI
  await auth.register({ firstName: "Ana", lastName: "Prueba", email, password });

  // Assert: la app pide verificar el correo
  await expect(page.getByRole("alert")).toContainText(email);

  // Act: la persona abre el enlace del correo que llegó a Mailpit.
  // ¿Qué? Se espera la respuesta 200 del API de verificación antes de seguir: sin esta
  //   espera, el test navega al login y corta la verificación a mitad de camino.
  const link = await findLinkInEmail(request, email, "/verify-email");
  const verified = page.waitForResponse(
    (response) => response.url().endsWith("/auth/verify-email") && response.ok(),
  );
  await page.goto(link);
  await verified;

  // Act: inicia sesión
  await auth.login(email, password);

  // Assert: llega al dashboard con su nombre
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Hola, Ana");
});

test("should show an error and stay on login when the password is wrong", async ({
  page,
  request,
}) => {
  // Arrange: la cuenta se crea por API; el test prueba solo el login
  const email = uniqueEmail();
  const response = await request.post(`${apiUrl}/api/v1/auth/register`, {
    data: { email, firstName: "Ana", lastName: "Prueba", password },
  });
  expect(response.ok()).toBe(true);
  const auth = new AuthPages(page);

  // Act
  await auth.login(email, "Incorrecta123");

  // Assert
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
});

test("should redirect to login when opening the dashboard without a session", async ({
  page,
}) => {
  await page.goto("/dashboard");

  await expect(page).toHaveURL(/\/login$/);
});
