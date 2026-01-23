package ec.edu.ups.icc.fundamentos01.security.filters;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.ups.icc.fundamentos01.exceptions.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component  // Spring lo registra como bean para inyección
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Logger para registrar errores de autenticación
     * 
     * Útil para:
     * - Debugging de problemas de autenticación
     * - Auditoría de intentos de acceso no autorizados
     * - Monitoreo de ataques (múltiples 401 desde misma IP)
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    /**
     * ObjectMapper: Convierte objetos Java a JSON
     * 
     * Jackson ObjectMapper:
     * - Serializa ErrorResponse a JSON
     * - Configurado automáticamente por Spring Boot
     * - Incluye JavaTimeModule para fechas
     * 
     * Inyección:
     * - Spring proporciona su ObjectMapper configurado
     * - Es el MISMO ObjectMapper que usan los @RestController
     * - Garantiza consistencia en formato de respuestas
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor: Inyección de dependencias
     * 
     * Spring inyecta su ObjectMapper configurado
     */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * commence: MÉTODO PRINCIPAL que maneja errores de autenticación
     * 
     * Se ejecuta AUTOMÁTICAMENTE cuando:
     * 1. JwtAuthenticationFilter NO encuentra token válido
     * 2. SecurityContext está VACÍO al llegar a endpoint protegido
     * 3. Spring Security detecta falta de autenticación
     * 
     * FLUJO:
     * 1. Spring Security detecta falta de autenticación
     * 2. Llama a commence() con detalles del error
     * 3. Este método construye respuesta JSON 401
     * 4. Escribe respuesta directamente en HttpServletResponse
     * 5. La request se termina (NO llega al controlador)
     * 
     * @param request: Petición HTTP que causó el error
     * @param response: Respuesta HTTP donde escribimos el error
     * @param authException: Excepción de autenticación con detalles del error
     * 
     * IMPORTANTE:
     * - Este método escribe DIRECTAMENTE en response
     * - NO retorna nada (void)
     * - Después de ejecutar, la request se termina
     * - El controlador NUNCA se ejecuta
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        /**
         * 1. Loguear el error
         * 
         * logger.error():
         * - Registra error en logs de aplicación
         * - Incluye mensaje de la excepción
         * - Útil para debugging y auditoría
         * 
         * authException.getMessage():
         * - Descripción del error de autenticación
         * - Ejemplos:
         *   * "Full authentication is required to access this resource"
         *   * "JWT token is expired"
         *   * "Bad credentials"
         * 
         * Ejemplo de log:
         * ERROR JwtAuthenticationEntryPoint - Error de autenticación: 
         *   Full authentication is required to access this resource
         */
        logger.error("Error de autenticación: {}", authException.getMessage());

        /**
         * 2. Crear respuesta de error estructurada
         * 
         * ErrorResponse:
         * - Clase personalizada de nuestro GlobalExceptionHandler
         * - Formato CONSISTENTE con otros errores de la API
         * - Incluye: status, message, timestamp, path
         * 
         * ¿Por qué usar ErrorResponse?
         * - Consistencia: Mismo formato para todos los errores
         * - Reutilización: Ya existe en GlobalExceptionHandler
         * - Claridad: Cliente recibe estructura conocida
         * 
         * Estructura de ErrorResponse:
         * {
         *   "timestamp": "2024-01-15T10:30:00",
         *   "status": 401,
         *   "error": "Unauthorized",
         *   "message": "Token de autenticación inválido...",
         *   "path": "/api/products"
         * }
         * 
         * Parámetros:
         * 1. HttpStatus.UNAUTHORIZED = 401
         * 2. Mensaje descriptivo en español
         * 3. request.getRequestURI() = path del endpoint que causó error
         * 
         * Mensaje detallado:
         * - Explica QUÉ salió mal: "Token inválido o no proporcionado"
         * - Explica CÓMO solucionarlo: "Debe incluir token en header"
         * - Muestra formato esperado: "Authorization: Bearer <token>"
         */
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.UNAUTHORIZED,  // Status 401
            "Token de autenticación inválido o no proporcionado. " +
                "Debe incluir un token válido en el header Authorization: Bearer <token>",
            request.getRequestURI()   // Path del endpoint (ej: /api/products)
        );

        /**
         * 3. Configurar Content-Type de la respuesta
         * 
         * MediaType.APPLICATION_JSON_VALUE = "application/json"
         * 
         * ¿Por qué es importante?
         * - Cliente sabrá que la respuesta es JSON
         * - Navegadores/clientes parsearán como JSON automáticamente
         * - Evita errores de parsing en frontend
         * 
         * Si olvidamos esto:
         * - Content-Type sería "text/html" por defecto
         * - Cliente intentaría parsear JSON como HTML
         * - Errores en frontend: "Unexpected token < in JSON"
         */
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        /**
         * 4. Establecer código de estado HTTP
         * 
         * HttpServletResponse.SC_UNAUTHORIZED = 401
         * 
         * Códigos de autenticación:
         * - 401 Unauthorized: Falta autenticación o token inválido
         * - 403 Forbidden: Autenticado pero sin permisos (lo maneja Spring Security)
         * 
         * ¿Qué ve el cliente?
         * HTTP/1.1 401 Unauthorized
         * Content-Type: application/json
         * { "status": 401, "message": "..." }
         */
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        /**
         * 5. Escribir JSON en la respuesta
         * 
         * objectMapper.writeValueAsString(errorResponse):
         * - Convierte ErrorResponse a String JSON
         * - Usa configuración de Jackson (fechas, null handling, etc.)
         * 
         * response.getWriter().write(...):
         * - Escribe el JSON en el cuerpo de la respuesta
         * - PrintWriter escribe directamente en el stream de salida
         * - La respuesta se envía al cliente
         * 
         * Resultado final enviado al cliente:
         * HTTP/1.1 401 Unauthorized
         * Content-Type: application/json
         * 
         * {
         *   "timestamp": "2024-01-15T10:30:00",
         *   "status": 401,
         *   "error": "Unauthorized",
         *   "message": "Token de autenticación inválido o no proporcionado. Debe incluir un token válido en el header Authorization: Bearer <token>",
         *   "path": "/api/products"
         * }
         * 
         * IMPORTANTE:
         * - Después de esto, la request se termina
         * - El controlador NUNCA se ejecuta
         * - No hay más filtros que procesen esta request
         */
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
