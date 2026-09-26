/**
 * Archivo: AuthControllerTest.java
 * Descripción: Tests de integración para todos los endpoints de AuthController.
 * ¿Para qué? Verificar el comportamiento completo del sistema de autenticación —
 *            desde la petición HTTP hasta la BD — sin mocks de infraestructura.
 *            La BD de pruebas (db-test) es PostgreSQL real, MockMvc simula el cliente HTTP.
 * ¿Impacto? Sin estos tests, los errores de integración (BD, validaciones, flujos)
 *           solo se detectarían en producción. Con ellos, se detectan en cada commit.
 *           Cobertura mínima esperada: 80% en AuthService y AuthController.
 */
package com.nn.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nn.auth.dto.request.ChangePasswordRequest;
import com.nn.auth.dto.request.ForgotPasswordRequest;
import com.nn.auth.dto.request.LoginRequest;
import com.nn.auth.dto.request.RefreshTokenRequest;
import com.nn.auth.dto.request.RegisterRequest;
import com.nn.auth.dto.request.ResetPasswordRequest;
import com.nn.auth.dto.request.VerifyEmailRequest;
import com.nn.auth.entity.EmailVerificationToken;
import com.nn.auth.entity.PasswordResetToken;
import com.nn.auth.entity.User;
import com.nn.auth.repository.EmailVerificationTokenRepository;
import com.nn.auth.repository.PasswordResetTokenRepository;
import com.nn.auth.repository.UserRepository;
import com.nn.auth.security.JwtService;
import com.nn.auth.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ¿Qué? Suite de tests de integración que arranca el contexto completo de
 * Spring Boot
 * con @AutoConfigureMockMvc para simular peticiones HTTP reales.
 * ¿Para qué? Testear el comportamiento end-to-end: validaciones Bean
 * Validation,
 * lógica de AuthService, persistencia en BD y respuestas HTTP.
 * ¿Impacto? @MockitoBean sobre EmailService evita envíos reales en tests —
 * el servicio de email se reemplaza por un spy de Mockito sin efectos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordResetTokenRepository passwordResetTokenRepository;

  @Autowired
  private EmailVerificationTokenRepository emailVerificationTokenRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  /**
   * ¿Qué? MockitoBean reemplaza EmailService por un spy de Mockito.
   * ¿Para qué? Evitar que los tests intenten conectar a Mailpit (no disponible en
   * CI).
   * ¿Impacto? Los tests verifican el flujo de negocio sin depender de
   * infraestructura
   * de email — tests más rápidos y deterministas.
   */
  @MockitoBean
  private EmailService emailService;

  // -------------------------------------------------------------------------
  // Helpers — datos de prueba reutilizables
  // -------------------------------------------------------------------------

  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PASSWORD = "Test1234";
  private static final String TEST_FIRST_NAME = "Test";
  private static final String TEST_LAST_NAME = "User";

  /**
   * ¿Qué? Crea y persiste un usuario verificado listo para tests de login.
   * ¿Para qué? Reutilizar la configuración de usuario en varios tests sin repetir
   * código.
   * ¿Impacto? @Transactional en la clase garantiza que cada test parte de BD
   * limpia
   * — el usuario creado aquí no persiste entre tests.
   *
   * @return User persitido con email verificado y contraseña hasheada
   */
  private User createVerifiedUser() {
    User user = User.builder()
        .email(TEST_EMAIL)
        .firstName(TEST_FIRST_NAME)
        .lastName(TEST_LAST_NAME)
        .hashedPassword(passwordEncoder.encode(TEST_PASSWORD))
        .build();
    user.markEmailAsVerified();
    return userRepository.save(user);
  }

  /**
   * ¿Qué? Crea un usuario SIN verificar email — útil para testear el flujo de
   * verificación.
   */
  private User createUnverifiedUser() {
    User user = User.builder()
        .email(TEST_EMAIL)
        .firstName(TEST_FIRST_NAME)
        .lastName(TEST_LAST_NAME)
        .hashedPassword(passwordEncoder.encode(TEST_PASSWORD))
        .build();
    return userRepository.save(user);
  }

  /**
   * ¿Qué? Genera un access token JWT válido para el usuario dado.
   * ¿Para qué? Simplificar la configuración de tests que requieren autenticación
   * sin pasar por el flujo de login completo.
   *
   * @param user Usuario para quien generar el token
   * @return String del JWT access token
   */
  private String generateAccessToken(User user) {
    return jwtService.generateAccessToken(user);
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/register
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/register")
  class RegisterTests {

    @Test
    @DisplayName("201 — registro exitoso con datos válidos")
    void register_success() throws Exception {
      RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_PASSWORD);

      mockMvc.perform(post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value(TEST_EMAIL))
          .andExpect(jsonPath("$.firstName").value(TEST_FIRST_NAME))
          .andExpect(jsonPath("$.lastName").value(TEST_LAST_NAME))
          .andExpect(jsonPath("$.emailVerified").value(false))
          .andExpect(jsonPath("$.id").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — email inválido rechazado por Bean Validation")
    void register_invalidEmail() throws Exception {
      RegisterRequest request = new RegisterRequest("not-an-email", TEST_FIRST_NAME, TEST_LAST_NAME, TEST_PASSWORD);

      mockMvc.perform(post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors.email").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — contraseña sin mayúscula rechazada por Bean Validation")
    void register_weakPassword_noUppercase() throws Exception {
      RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, "test1234");

      mockMvc.perform(post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors.password").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — contraseña sin número rechazada por Bean Validation")
    void register_weakPassword_noDigit() throws Exception {
      RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, "TestPassword");

      mockMvc.perform(post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors.password").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — email duplicado devuelve error genérico (OWASP A01)")
    void register_duplicateEmail() throws Exception {
      createVerifiedUser(); // ya existe en la BD

      RegisterRequest request = new RegisterRequest(TEST_EMAIL, "Another", "User", TEST_PASSWORD);

      mockMvc.perform(post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — body vacío devuelve errores de validación")
    void register_emptyBody() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors").value(notNullValue()));
    }
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/login
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/login")
  class LoginTests {

    @Test
    @DisplayName("200 — login exitoso devuelve access y refresh tokens")
    void login_success() throws Exception {
      createVerifiedUser();

      LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

      mockMvc.perform(post("/api/v1/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value(notNullValue()))
          .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
          .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("401 — contraseña incorrecta devuelve error genérico (OWASP A01)")
    void login_wrongPassword() throws Exception {
      createVerifiedUser();

      LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPass1");

      mockMvc.perform(post("/api/v1/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 — email no registrado devuelve error genérico (OWASP A01)")
    void login_emailNotFound() throws Exception {
      LoginRequest request = new LoginRequest("noexiste@example.com", TEST_PASSWORD);

      mockMvc.perform(post("/api/v1/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400 — login con email sin verificar devuelve 400 con mensaje descriptivo")
    void login_emailNotVerified() throws Exception {
      createUnverifiedUser();

      LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

      mockMvc.perform(post("/api/v1/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value(notNullValue()));
    }
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/refresh
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/refresh")
  class RefreshTokenTests {

    @Test
    @DisplayName("200 — refresh exitoso con token de refresco válido")
    void refresh_success() throws Exception {
      User user = createVerifiedUser();
      String refreshToken = jwtService.generateRefreshToken(user);

      RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

      mockMvc.perform(post("/api/v1/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value(notNullValue()))
          .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    @DisplayName("401 — usar access token como refresh token es rechazado")
    void refresh_withAccessToken_rejected() throws Exception {
      User user = createVerifiedUser();
      String accessToken = jwtService.generateAccessToken(user);

      RefreshTokenRequest request = new RefreshTokenRequest(accessToken);

      mockMvc.perform(post("/api/v1/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 — token malformado devuelve 401")
    void refresh_malformedToken() throws Exception {
      RefreshTokenRequest request = new RefreshTokenRequest("esto.no.es.un.jwt.valido");

      mockMvc.perform(post("/api/v1/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/change-password
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/change-password")
  class ChangePasswordTests {

    @Test
    @DisplayName("200 — cambio de contraseña exitoso con contraseña actual correcta")
    void changePassword_success() throws Exception {
      User user = createVerifiedUser();
      String accessToken = generateAccessToken(user);
      String newPassword = "NewPass1234";

      ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, newPassword);

      mockMvc.perform(post("/api/v1/auth/change-password")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    @Test
    @DisplayName("401 — sin token de autorización devuelve 401")
    void changePassword_noToken() throws Exception {
      ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "NewPass1234");

      mockMvc.perform(post("/api/v1/auth/change-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 — contraseña actual incorrecta devuelve 401")
    void changePassword_wrongCurrentPassword() throws Exception {
      User user = createVerifiedUser();
      String accessToken = generateAccessToken(user);

      ChangePasswordRequest request = new ChangePasswordRequest("WrongCurrent1", "NewPass1234");

      mockMvc.perform(post("/api/v1/auth/change-password")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/forgot-password
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/forgot-password")
  class ForgotPasswordTests {

    @Test
    @DisplayName("200 — respuesta genérica si el email existe (no revela existencia)")
    void forgotPassword_emailExists() throws Exception {
      createVerifiedUser();

      ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);

      mockMvc.perform(post("/api/v1/auth/forgot-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    @Test
    @DisplayName("200 — respuesta genérica si el email NO existe (OWASP A01 — no enumerar)")
    void forgotPassword_emailNotExist_sameResponse() throws Exception {
      ForgotPasswordRequest request = new ForgotPasswordRequest("noexiste@example.com");

      mockMvc.perform(post("/api/v1/auth/forgot-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — email inválido rechazado por validación")
    void forgotPassword_invalidEmail() throws Exception {
      ForgotPasswordRequest request = new ForgotPasswordRequest("no-es-email");

      mockMvc.perform(post("/api/v1/auth/forgot-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/reset-password
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/reset-password")
  class ResetPasswordTests {

    @Test
    @DisplayName("200 — reset exitoso con token válido")
    void resetPassword_success() throws Exception {
      User user = createVerifiedUser();

      // Crear token de reset directamente en la BD (sin email)
      String tokenValue = "valid-reset-token-uuid-12345678";
      PasswordResetToken resetToken = PasswordResetToken.builder()
          .user(user)
          .token(tokenValue)
          .expiresAt(OffsetDateTime.now().plusHours(1))
          .build();
      passwordResetTokenRepository.save(resetToken);

      ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "NewResetPass1");

      mockMvc.perform(post("/api/v1/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — token inexistente devuelve 400")
    void resetPassword_invalidToken() throws Exception {
      ResetPasswordRequest request = new ResetPasswordRequest("token-que-no-existe", "NewPass1234");

      mockMvc.perform(post("/api/v1/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 — token expirado devuelve 400")
    void resetPassword_expiredToken() throws Exception {
      User user = createVerifiedUser();

      String tokenValue = "expired-token-uuid-12345678";
      PasswordResetToken expiredToken = PasswordResetToken.builder()
          .user(user)
          .token(tokenValue)
          .expiresAt(OffsetDateTime.now().minusHours(2)) // expirado hace 2 horas
          .build();
      passwordResetTokenRepository.save(expiredToken);

      ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "NewPass1234");

      mockMvc.perform(post("/api/v1/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  // -------------------------------------------------------------------------
  // Tests de POST /api/v1/auth/verify-email
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /api/v1/auth/verify-email")
  class VerifyEmailTests {

    @Test
    @DisplayName("200 — verificación exitosa activa la cuenta del usuario")
    void verifyEmail_success() throws Exception {
      User user = createUnverifiedUser();

      String tokenValue = "valid-verification-token-uuid5678";
      EmailVerificationToken verificationToken = EmailVerificationToken.builder()
          .user(user)
          .token(tokenValue)
          .expiresAt(OffsetDateTime.now().plusHours(24))
          .build();
      emailVerificationTokenRepository.save(verificationToken);

      VerifyEmailRequest request = new VerifyEmailRequest(tokenValue);

      mockMvc.perform(post("/api/v1/auth/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    @Test
    @DisplayName("400 — token de verificación inexistente devuelve 400")
    void verifyEmail_invalidToken() throws Exception {
      VerifyEmailRequest request = new VerifyEmailRequest("token-que-no-existe");

      mockMvc.perform(post("/api/v1/auth/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 — token de verificación expirado devuelve 400")
    void verifyEmail_expiredToken() throws Exception {
      User user = createUnverifiedUser();

      String tokenValue = "expired-verification-token-5678";
      EmailVerificationToken expiredToken = EmailVerificationToken.builder()
          .user(user)
          .token(tokenValue)
          .expiresAt(OffsetDateTime.now().minusHours(25)) // expirado hace 25 horas
          .build();
      emailVerificationTokenRepository.save(expiredToken);

      VerifyEmailRequest request = new VerifyEmailRequest(tokenValue);

      mockMvc.perform(post("/api/v1/auth/verify-email")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  // -------------------------------------------------------------------------
  // Tests de GET /api/v1/users/me
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /api/v1/users/me")
  class UserProfileTests {

    @Test
    @DisplayName("200 — perfil del usuario autenticado retorna datos correctos")
    void getProfile_authenticated() throws Exception {
      User user = createVerifiedUser();
      String accessToken = generateAccessToken(user);

      mockMvc.perform(get("/api/v1/users/me")
          .header("Authorization", "Bearer " + accessToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value(TEST_EMAIL))
          .andExpect(jsonPath("$.firstName").value(TEST_FIRST_NAME))
          .andExpect(jsonPath("$.lastName").value(TEST_LAST_NAME))
          .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @DisplayName("401 — sin token de autorización devuelve 401")
    void getProfile_noToken() throws Exception {
      mockMvc.perform(get("/api/v1/users/me"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 — con token inválido devuelve 401")
    void getProfile_invalidToken() throws Exception {
      mockMvc.perform(get("/api/v1/users/me")
          .header("Authorization", "Bearer token.invalido.aqui"))
          .andExpect(status().isUnauthorized());
    }
  }
}
