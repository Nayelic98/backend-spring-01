package ec.edu.ups.icc.fundamentos01.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    /**
     * Configura ObjectMapper global para toda la aplicación
     * 
     * @Primary: Marca este bean como el ObjectMapper principal
     * Se usa automáticamente en:
     * - @RestController para serializar respuestas
     * - JwtAuthenticationEntryPoint para serializar errores
     * - Cualquier componente que inyecte ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ============== CONFIGURACIÓN CRÍTICA ==============
        
        // Registrar módulo para manejo de fechas Java 8+
        // Permite serializar: LocalDateTime, LocalDate, LocalTime, Instant, etc.
        mapper.registerModule(new JavaTimeModule());

        // Serializar fechas como ISO-8601 ("2024-01-26T10:30:00")
        // En lugar de timestamp numérico (1706268600000)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ============== CONFIGURACIONES OPCIONALES ==============
        
        // No fallar si un bean está vacío (sin propiedades)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Indentar JSON para mejor legibilidad (opcional, desactivar en producción)
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }
}
