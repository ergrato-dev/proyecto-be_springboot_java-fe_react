/**
 * Archivo: NnAuthSystemApplicationTests.java
 * Descripción: Test de smoke que verifica que el contexto de Spring carga correctamente.
 * ¿Para qué? Detectar rápidamente problemas de configuración, beans mal definidos
 *            o dependencias faltantes — si este test falla, todo falla.
 * ¿Impacto? Es el test más básico pero el más revelador: si el contexto no carga,
 *           ningún otro test tiene sentido ejecutar.
 */
package com.nn.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * ¿Qué? Test de integración que arranca el contexto completo de Spring Boot
 * conectando a la BD de test (nn_auth_test_db) definida en
 * application-test.yml.
 * ¿Para qué? Verificar que todos los beans, configuraciones y dependencias
 * se cargan correctamente sin errores al arrancar.
 * ¿Impacto? @ActiveProfiles("test") activa application-test.yml que apunta
 * a nn_auth_test_db en lugar de la BD de desarrollo nn_auth_db.
 */
@SpringBootTest
@ActiveProfiles("test")
class NnAuthSystemApplicationTests {

	/**
	 * ¿Qué? Verifica que el contexto de Spring Boot carga sin errores.
	 * ¿Para qué? Si algún bean falla al inicializarse, este test falla
	 * con un mensaje claro del bean problemático.
	 */
	@Test
	@DisplayName("El contexto de Spring Boot carga correctamente con la BD de pruebas")
	void contextLoads() {
		// Si llega aquí sin excepción, el contexto cargó correctamente
	}
}
