package ec.edu.ups.icc.fundamentos01.security.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import ec.edu.ups.icc.fundamentos01.security.config.JwtProperties;
import ec.edu.ups.icc.fundamentos01.security.services.UserDetailsImpl;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String generateToken(Authentication authentication) {
        // 1. Extraer información del usuario autenticado
        // Cast seguro porque siempre retorna UserDetailsImpl
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        // 2. Calcular fechas de emisión y expiración
        Date now = new Date(); // Fecha actual
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        // Ejemplo: now = 2024-01-26 10:00:00
        // expiration = 1800000 ms (30 minutos)
        // expiryDate = 2024-01-26 10:30:00

        // 3. Extraer roles del usuario y convertir a String
        // Ejemplo: [ROLE_USER, ROLE_ADMIN] → "ROLE_USER,ROLE_ADMIN"
        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Extrae "ROLE_USER", "ROLE_ADMIN"
                .collect(Collectors.joining(",")); // Une con comas

        // 4. Construir y firmar el token JWT
        return Jwts.builder()
                // Subject: Identificador único del usuario (su ID)
                .subject(String.valueOf(userPrincipal.getId())) // "1"

                // Claims personalizados (datos adicionales en el payload)
                .claim("email", userPrincipal.getEmail()) // "pablo@example.com"
                .claim("name", userPrincipal.getName()) // "Pablo Torres"
                .claim("roles", roles) // "ROLE_USER,ROLE_ADMIN"

                // Issuer: Quién emitió el token
                .issuer(jwtProperties.getIssuer()) // "fundamentos01-api"

                // Fechas
                .issuedAt(now) // Cuándo se creó
                .expiration(expiryDate) // Cuándo expira

                // Firma digital con algoritmo HS256
                .signWith(key, Jwts.SIG.HS256) // Firma con clave secreta

                // Compactar: Genera el String final
                .compact(); // → "eyJhbGci...header.eyJzdWI...payload.firma"
    }

    public String generateTokenFromUserDetails(UserDetailsImpl userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .claim("email", userDetails.getEmail())
                .claim("name", userDetails.getName())
                .claim("roles", roles)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        // 1. Parsear y validar el token
        Claims claims = Jwts.parser()
                .verifyWith(key) // Verifica firma con clave secreta
                .build() // Construye el parser
                .parseSignedClaims(token) // Parsea el token
                .getPayload(); // Obtiene el payload (claims)

        // 2. Extraer el subject (ID del usuario)
        // subject = "1" (guardado como String en el token)
        // Long.parseLong("1") = 1L
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Extraer claim "email" como String
        return claims.get("email", String.class);
    }

    public boolean validateToken(String authToken) {
        try {
            // Intenta parsear el token
            // Si algo falla, lanza excepción
            Jwts.parser()
                    .verifyWith(key) // Verifica firma con nuestra clave
                    .build()
                    .parseSignedClaims(authToken);

            // Si llegamos aquí, el token es VÁLIDO
            return true;

        } catch (SignatureException ex) {
            // Firma inválida: Token modificado o clave incorrecta
            // Ejemplo: Alguien cambió el payload pero no puede firmar correctamente
            logger.error("Firma JWT inválida: {}", ex.getMessage());

        } catch (MalformedJwtException ex) {
            // Token malformado: No tiene estructura correcta (header.payload.signature)
            // Ejemplo: "abc123" en lugar de token válido
            logger.error("Token JWT malformado: {}", ex.getMessage());

        } catch (ExpiredJwtException ex) {
            // Token expirado: Pasaron más de 30 minutos desde su creación
            // Ejemplo: Token creado a las 10:00, ahora son las 10:35
            logger.error("Token JWT expirado: {}", ex.getMessage());

        } catch (UnsupportedJwtException ex) {
            // Token no soportado: Usa algoritmo que no soportamos
            // Ejemplo: Token firmado con RS256 pero esperamos HS256
            logger.error("Token JWT no soportado: {}", ex.getMessage());

        } catch (IllegalArgumentException ex) {
            // Claims vacío: Token sin payload
            logger.error("JWT claims string está vacío: {}", ex.getMessage());
        }

        // Si cayó en cualquier catch, el token es INVÁLIDO
        return false;
    }
}
