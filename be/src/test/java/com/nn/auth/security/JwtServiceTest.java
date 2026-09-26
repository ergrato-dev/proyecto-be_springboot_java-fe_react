/**
 * Archivo: JwtServiceTest.java
 * Descripción: Tests unitarios de JwtService — generación y validación de tokens JWT.
 * ¿Para qué? Probar la lógica de los tokens de forma aislada: sin Spring, sin HTTP y sin
 *            BD. JwtService se crea con `new`, pasándole la configuración por constructor.
 *            Cada test sigue el patrón AAA (Arrange, Act, Assert).
 * ¿Impacto? Son los tests más rápidos de la suite (milisegundos, sin levantar el contexto)
 *           y señalan el error exacto: si falla aquí, el problema está en el JWT.
 */
package com.nn.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nn.auth.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

  private JwtService jwtService;
  private UserDetails ana;

  @BeforeEach
  void setUp() {
    // ¿Qué? Configuración de prueba: secreto de 32+ caracteres y expiraciones por defecto.
    AppProperties properties = new AppProperties(
        new AppProperties.Jwt("unit-test-secret-key-at-least-32-characters", 15, 7),
        new AppProperties.RateLimit(false, 10, 15),
        "http://localhost:5173",
        "test");
    jwtService = new JwtService(properties);
    ana = User.withUsername("ana@nn-company.com").password("irrelevante").build();
  }

  @Test
  @DisplayName("should put the user email in the access token")
  void shouldPutTheUserEmailInTheAccessToken() {
    // Act
    String token = jwtService.generateAccessToken(ana);

    // Assert
    assertThat(jwtService.extractEmail(token)).isEqualTo("ana@nn-company.com");
    assertThat(jwtService.isAccessToken(token)).isTrue();
    assertThat(jwtService.isRefreshToken(token)).isFalse();
  }

  @Test
  @DisplayName("should reject a token that belongs to another user")
  void shouldRejectATokenThatBelongsToAnotherUser() {
    // Arrange
    String token = jwtService.generateAccessToken(ana);
    UserDetails luis = User.withUsername("luis@nn-company.com").password("irrelevante").build();

    // Act / Assert
    assertThat(jwtService.isTokenValid(token, luis)).isFalse();
  }

  @Test
  @DisplayName("should reject a token that was modified after being signed")
  void shouldRejectATokenThatWasModifiedAfterBeingSigned() {
    // Arrange: se altera el contenido (payload) sin volver a firmarlo.
    // ¿Por qué no el último carácter de la firma? En base64url ese carácter tiene bits de
    // relleno: cambiar "A" por "B" puede dejar la firma idéntica y el test fallaría al azar.
    String token = jwtService.generateAccessToken(ana);
    String[] parts = token.split("\\.");
    String payload = (parts[1].startsWith("f") ? "e" : "f") + parts[1].substring(1);
    String tampered = parts[0] + "." + payload + "." + parts[2];

    // Act / Assert
    assertThat(jwtService.isTokenValid(tampered, ana)).isFalse();
  }
}
