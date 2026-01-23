package ec.edu.ups.icc.fundamentos01.security.filters;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import ec.edu.ups.icc.fundamentos01.security.config.JwtProperties;
import ec.edu.ups.icc.fundamentos01.security.services.UserDetailsServiceImpl;
import ec.edu.ups.icc.fundamentos01.security.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component   // Spring lo registra automáticamente como bean
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Logger para debugging y errores
     * 
     * Niveles de log:
     * - logger.debug(): Solo en desarrollo (no aparece en producción)
     * - logger.error(): Errores críticos (aparece en producción)
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * Dependencias inyectadas por Spring
     */
    private final JwtUtil jwtUtil; // Para validar y extraer datos del JWT
    private final UserDetailsServiceImpl userDetailsService; // Para cargar usuario desde BD
    private final JwtProperties jwtProperties; // Configuración JWT (header, prefix)

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
            UserDetailsServiceImpl userDetailsService,
            JwtProperties jwtProperties) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * doFilterInternal: MÉTODO PRINCIPAL del filtro
     * 
     * Se ejecuta UNA VEZ por cada request HTTP
     * 
     * @param request:     Petición HTTP entrante
     * @param response:    Respuesta HTTP saliente
     * @param filterChain: Cadena de filtros restantes
     * 
     *                     IMPORTANTE:
     *                     - Este método NO debe lanzar excepciones
     *                     - Si hay error, solo logueamos y continuamos
     *                     - El SecurityContext quedará vacío → Spring Security
     *                     rechazará la petición
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            /**
             * PASO 1: Extraer token del header Authorization
             */
            String jwt = getJwtFromRequest(request);

            /**
             * PASO 2: Validar y autenticar SOLO si hay token
             */
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {

                /**
                 * PASO 3: Extraer email del token
                 */
                String email = jwtUtil.getEmailFromToken(jwt);

                /**
                 * PASO 4: Cargar usuario desde base de datos
                 */
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                /**
                 * PASO 5: Crear objeto Authentication
                 * 
                 * UsernamePasswordAuthenticationToken:
                 * - Implementación de Authentication de Spring Security
                 * - Aunque se llama "Password", NO usamos contraseña aquí
                 * - Ya validamos el JWT, no necesitamos validar password
                 * 
                 * Constructor con 3 parámetros:
                 * 
                 * @param principal:   El usuario (UserDetails)
                 * @param credentials: Credenciales (null porque ya autenticamos con JWT)
                 * @param authorities: Roles/permisos del usuario
                 */
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, // Principal (el usuario)
                        null, // Credentials (no necesarias)
                        userDetails.getAuthorities() // Authorities (roles/permisos)
                );

                /**
                 * Establecer detalles adicionales de la request
                 * 
                 * WebAuthenticationDetailsSource:
                 * - Extrae información de la HttpServletRequest
                 * - IP del cliente
                 * - Session ID (si existe)
                 * - Otros metadatos de la petición
                 * 
                 * .buildDetails(request):
                 * - Crea objeto WebAuthenticationDetails
                 * - Útil para auditoría y logs
                 * 
                 * Ejemplo de details:
                 * {
                 * remoteAddress: "192.168.1.100",
                 * sessionId: null (porque somos stateless)
                 * }
                 */
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                /**
                 * PASO 6: Establecer autenticación en SecurityContext
                 * 
                 * SecurityContextHolder:
                 * - ThreadLocal que almacena el contexto de seguridad
                 * - ThreadLocal: Una variable por thread (cada request = thread diferente)
                 * - Permite acceder al usuario autenticado desde cualquier parte del código
                 * 
                 * .getContext():
                 * - Obtiene o crea el SecurityContext para este thread
                 * 
                 * .setAuthentication(authentication):
                 * - Almacena el objeto Authentication
                 * - A partir de ahora, el usuario está AUTENTICADO
                 * - Spring Security permitirá acceso a endpoints protegidos
                 * 
                 * ¿Cómo se usa después?
                 * 
                 * En controladores:
                 * 
                 * @AuthenticationPrincipal UserDetailsImpl currentUser
                 * 
                 *                          En servicios:
                 *                          Authentication auth =
                 *                          SecurityContextHolder.getContext().getAuthentication();
                 *                          UserDetailsImpl user = (UserDetailsImpl)
                 *                          auth.getPrincipal();
                 * 
                 *                          En @PreAuthorize:
                 *                          @PreAuthorize("hasRole('ADMIN')") ← Lee authorities
                 *                          de aquí
                 */
                SecurityContextHolder.getContext().setAuthentication(authentication);

                /**
                 * Log de debug: Solo en desarrollo
                 * 
                 * logger.debug():
                 * - Solo aparece si logging.level.root=DEBUG
                 * - NO aparece en producción (logging.level.root=INFO)
                 * - Útil para debugging durante desarrollo
                 * 
                 * Mensaje de ejemplo:
                 * "Usuario autenticado: pablo@example.com"
                 */
                logger.debug("Usuario autenticado: {}", email);
            }

        } catch (Exception ex) {
            /**
             * Manejo de errores: Solo loguear, NO lanzar excepción
             * 
             * ¿Por qué no lanzar la excepción?
             * - Si lanzamos excepción, la request se aborta completamente
             * - Mejor: Dejar que continúe sin autenticación
             * - Spring Security se encargará de rechazarla con 401
             * 
             */
            logger.error("No se pudo establecer la autenticación del usuario", ex);
        }

        /**
         * PASO 7: Continuar con la cadena de filtros
         */
        filterChain.doFilter(request, response);
    }

    /**
     * getJwtFromRequest: Método helper para extraer JWT del header
     * 
     * FLUJO:
     * 1. Lee header "Authorization"
     * 2. Verifica que empiece con "Bearer "
     * 3. Extrae solo el token (sin "Bearer ")
     * 4. Retorna token o null
     * 
     * @param request: Petición HTTP
     * @return String: Token JWT o null si no existe
     * 
     *         Ejemplo:
     *         Header: "Authorization: Bearer eyJhbGci..."
     *         Retorna: "eyJhbGci..."
     * 
     *         Header: "Authorization: Basic abc123" (no es Bearer)
     *         Retorna: null
     * 
     *         Sin header Authorization
     *         Retorna: null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        /**
         * 1. Leer header Authorization
         * 
         * request.getHeader(jwtProperties.getHeader()):
         * - jwtProperties.getHeader() = "Authorization"
         * - Lee el valor del header
         * - Retorna null si no existe
         * 
         * Ejemplo:
         * bearerToken = "Bearer eyJhbGciOiJIUzI1NiJ9..."
         */
        String bearerToken = request.getHeader(jwtProperties.getHeader());

        /**
         * 2. Validar y extraer token
         * 
         * StringUtils.hasText(bearerToken):
         * - Verifica que NO sea null, vacío o solo espacios
         * 
         * bearerToken.startsWith(jwtProperties.getPrefix()):
         * - jwtProperties.getPrefix() = "Bearer "
         * - Verifica que el header comience con "Bearer "
         * - Importante: Incluye el espacio después de "Bearer"
         * 
         * bearerToken.substring(jwtProperties.getPrefix().length()):
         * - Extrae desde la posición 7 (longitud de "Bearer ")
         * - Ejemplo: "Bearer abc123".substring(7) = "abc123"
         * - Retorna solo el token, sin el prefijo
         * 
         * Si NO cumple las condiciones:
         * - Retorna null
         * - El filtro NO procesará autenticación
         * - La request continuará sin autenticación
         */
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getPrefix())) {
            return bearerToken.substring(jwtProperties.getPrefix().length());
        }

        return null;
    }
}
