---
description: "Crea un endpoint Spring Boot completo: Controller, Service, DTO y test MockMvc. Usar cuando se necesite agregar una nueva ruta a la API."
name: "Nuevo endpoint Spring Boot"
argument-hint: "Describe el endpoint: método HTTP, ruta, qué hace, qué datos recibe y devuelve"
agent: "agent"
---

# Nuevo endpoint Spring Boot — NN Auth System

Crea un endpoint Spring Boot completo siguiendo las convenciones del proyecto.

## Convenciones obligatorias

- **Idioma del código**: inglés (nombres de clases, métodos, variables, rutas, columnas)
- **Idioma de comentarios y Javadoc**: español
- **Comentarios pedagógicos**: cada bloque significativo responde ¿Qué? ¿Para qué? ¿Impacto?
- **Tipos explícitos**: obligatorios en todos los parámetros y retornos — nunca `var` en métodos públicos
- **Cabecera de archivo**: incluir si se crea un archivo nuevo (ver copilot-instructions.md §3.4)

## Lo que debes generar

### 1. DTO de request con Bean Validation (`be/src/main/java/com/nn/auth/dto/request/`)

```java
/**
 * Archivo: NombreRequest.java
 * ¿Qué? DTO que encapsula los datos del request para [descripción].
 * ¿Para qué? Validar y transportar los datos del cliente al service sin exponer la entidad.
 * ¿Impacto? Sin este DTO, la entidad JPA quedaría expuesta a modificaciones directas.
 */
public record NombreRequest(
    @NotBlank(message = "El campo es requerido")
    @Email(message = "Formato de email inválido")
    String email
    // ... más campos con sus validaciones
) {}
```

- Usar `@NotBlank` para strings requeridos
- Usar `@Email` para emails
- Usar `@Size(min=X, max=Y)` para longitud
- Usar `@Pattern(regexp="...")` para reglas complejas
- Usar `@NotNull` para tipos no primitivos requeridos

### 2. DTO de respuesta (`be/src/main/java/com/nn/auth/dto/response/`)

```java
/**
 * Archivo: NombreResponse.java
 * ¿Qué? DTO de respuesta que expone solo los campos seguros al cliente.
 * ¿Para qué? Evitar filtrar campos sensibles (hashedPassword, etc.) en la respuesta HTTP.
 * ¿Impacto? Si se devolviera la entidad JPA directamente, se expondrían datos internos.
 */
public record NombreResponse(
    UUID id,
    String email
    // ... solo campos seguros para el cliente
) {
    public static NombreResponse from(NombreEntidad entity) {
        return new NombreResponse(entity.getId(), entity.getEmail());
    }
}
```

### 3. Lógica en el service (`be/src/main/java/com/nn/auth/service/`)

- Lógica de negocio pura — sin lógica HTTP ni de presentación
- Recibe DTOs como parámetros, retorna DTOs
- Lanza excepciones específicas ante errores de negocio
- Referencia: [be/src/main/java/com/nn/auth/service/AuthService.java](../../../be/src/main/java/com/nn/auth/service/AuthService.java)

```java
/**
 * ¿Qué? Método que [descripción corta de lo que hace].
 * ¿Para qué? [razón de negocio].
 * ¿Impacto? [consecuencia si falla o se omite].
 *
 * @param request DTO con [descripción de los parámetros].
 * @return [descripción del retorno].
 */
public NombreResponse nombreMetodo(NombreRequest request) {
    // lógica aquí
}
```

### 4. Endpoint en el controller (`be/src/main/java/com/nn/auth/controller/`)

- Usar el controller existente o crear uno nuevo si corresponde a un nuevo dominio
- `@Valid` obligatorio en el `@RequestBody`
- Endpoints de auth van en [AuthController.java](../../../be/src/main/java/com/nn/auth/controller/AuthController.java)
- Endpoints de usuario van en [UserController.java](../../../be/src/main/java/com/nn/auth/controller/UserController.java)

```java
/**
 * ¿Qué? Endpoint que [descripción].
 * ¿Para qué? [razón HTTP del endpoint].
 * ¿Impacto? [qué habilita este endpoint en el sistema].
 */
@PostMapping("/ruta")
@ResponseStatus(HttpStatus.CREATED)  // o HttpStatus.OK según corresponda
public NombreResponse nombreEndpoint(@Valid @RequestBody NombreRequest request) {
    return nombreService.nombreMetodo(request);
}
```

### 5. Test con MockMvc (`be/src/test/java/com/nn/auth/controller/`)

- Una clase de test por controller (`class AuthControllerTest`)
- Casos mínimos: éxito, input inválido (422), no autenticado si aplica (401), error de negocio (400/409)
- Usar `@SpringBootTest` + `MockMvc` o `@WebMvcTest` + mock de services
- Usar la BD de pruebas (servicio db-test) para tests de integración con BD real
- Referencia: [be/src/test/java/com/nn/auth/controller/AuthControllerTest.java](../../../be/src/test/java/com/nn/auth/controller/AuthControllerTest.java)

```java
@Test
@DisplayName("POST /api/v1/auth/ruta — 201 cuando los datos son válidos")
void nombreEndpoint_shouldReturn201_whenDataIsValid() throws Exception {
    // given
    String requestBody = """
        {"campo": "valor"}
        """;

    // when + then
    mockMvc.perform(post("/api/v1/auth/ruta")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.campo").value("valor"));
}
```

## Prefijo base de la API

Todos los endpoints se registran bajo `/api/v1/`. Verificar que el controller
tenga `@RequestMapping("/api/v1/auth")` o la ruta correspondiente.

> Spring Boot corre en el puerto **8080**. Swagger UI: `http://localhost:8080/swagger-ui.html`

## Seguridad (OWASP)

- Validar todos los inputs con `@Valid` + Bean Validation — nunca confiar en datos del cliente
- Usar mensajes de error genéricos en auth (no revelar si un email existe)
- Rate limiting configurado vía Bucket4j en `AuthController`
- Passwords: siempre `BCryptPasswordEncoder.encode()` antes de almacenar, nunca texto plano
- Endpoints protegidos: verificar que `JwtAuthenticationFilter` los proteja en `SecurityConfig`

## Descripción del endpoint a crear

$input
