/**
 * Archivo: e2e/tests/support/auth-pages.js
 * Descripción: Objeto de página de los formularios de registro e inicio de sesión.
 * ¿Para qué? Que los tests no repitan los mismos locators: si una etiqueta cambia, se
 *   corrige aquí y no en cada test.
 * ¿Impacto? Solo tiene locators y acciones; las aserciones se quedan en los tests.
 */

export class AuthPages {
  constructor(page) {
    this.page = page;
  }

  async register({ firstName, lastName, email, password }) {
    await this.page.goto("/register");
    await this.page.getByLabel("Nombres").fill(firstName);
    await this.page.getByLabel("Apellidos").fill(lastName);
    await this.page.getByLabel("Correo electrónico", { exact: true }).fill(email);
    await this.page.getByLabel("Confirmar correo electrónico").fill(email);
    await this.page.getByLabel("Contraseña", { exact: true }).fill(password);
    await this.page.getByLabel("Confirmar contraseña").fill(password);
    for (const consent of await this.page.getByRole("checkbox").all()) {
      await consent.check();
    }
    await this.page.getByRole("button", { name: "Crear cuenta" }).click();
  }

  async login(email, password) {
    await this.page.goto("/login");
    await this.page.getByLabel("Correo electrónico").fill(email);
    await this.page.getByLabel("Contraseña", { exact: true }).fill(password);
    await this.page.getByRole("button", { name: "Iniciar sesión" }).click();
  }
}
