package ec.edu.ups.icc.fundamentos01.security.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.fundamentos01.security.dtos.AuthResponseDto;
import ec.edu.ups.icc.fundamentos01.security.dtos.LoginRequestDto;
import ec.edu.ups.icc.fundamentos01.security.dtos.RegisterRequestDto;
import ec.edu.ups.icc.fundamentos01.security.services.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth") // Prefijo para todos los endpoints de autenticación
public class AuthController {

    private final AuthService authService; // Servicio de lógica de autenticación

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Login - Endpoint público (configurado en SecurityConfig)
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        // @Valid valida anotaciones en LoginRequestDto (email, password requeridos)
        AuthResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(response); // 200 OK con JWT
    }

    /**
     * Registro - Endpoint público (configurado en SecurityConfig)
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto registerRequest) {
        // @Valid valida anotaciones en RegisterRequestDto
        AuthResponseDto response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created con JWT
    }
}
