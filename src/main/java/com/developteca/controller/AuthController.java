package com.developteca.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.developteca.dto.LoginRequest;
import com.developteca.dto.LoginResponse;
import com.developteca.dto.RegisterRequest;
import com.developteca.dto.UserResponse;
import com.developteca.exception.ApiException;
import com.developteca.service.AuthService;
import com.developteca.util.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController (AuthService authService){
        this.authService = authService;
    }

    // ============== ENDPOINT: REGISTRO ============
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request){
        try {
            UserResponse userResponse = authService.register(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse(
                    true,
                    "Usuario registrado exitosamente. Verifica tu email.",
                    userResponse
                )
            );
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse(false, e.getMessage(), null)
            );

        }
    }

    // ============= ENDPOINT: LOGIN ==============
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
        try{
            LoginResponse loginResponse = authService.login(request);
            
            return ResponseEntity.ok(
                new ApiResponse(
                    true,
                    "Login exitoso",
                    loginResponse
                )
            );
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    // ============= ENDPOINT: VERIFICAR EMAIL =============
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
        
            return ResponseEntity.ok(
                new ApiResponse(
                    true,
                "Email verificado exitosamente. Ahora puedes iniciar sesión.",
                null
            )
        );
    } catch (ApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiResponse(false, e.getMessage(), null)
        );
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiResponse(false, "Error verificando email: " + e.getMessage(), null)
        );
    }
}
}
