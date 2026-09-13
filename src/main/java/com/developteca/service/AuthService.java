package com.developteca.service;

import com.developteca.dto.LoginRequest;
import com.developteca.dto.LoginResponse;
import com.developteca.dto.RegisterRequest;
import com.developteca.dto.UserResponse;
import com.developteca.entity.Role;
import com.developteca.entity.User;
import com.developteca.exception.ApiException;
import com.developteca.repository.UserRepository;
import com.developteca.util.TokenUtil;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    // ============= REGISTRAR USUARIO =============
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 1. Validar que el email no existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("El email ya está registrado");
        }

        // 2. Crear nuevo usuario
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),  // Hashear contraseña
                request.getFirstName(),
                request.getLastName()
        );
        user.setRole(Role.USER);
        user.setEmailVerified(false);  // Pendiente de verificación

        // 3. Generar token de verificación
        String verificationToken = TokenUtil.generateToken();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        // 4. Guardar en BD
        User savedUser = userRepository.save(user);

        // 5. Enviar email de verificación
            emailService.sendVerificationEmail(
            savedUser.getEmail(),
            verificationToken,
            savedUser.getFirstName()
    );

        // 4. Retornar como DTO (sin password)
        return mapToUserResponse(savedUser);
    }

    // ============= LOGIN =============
    public LoginResponse login(LoginRequest request) {
        // 1. Buscar usuario por email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Email o contraseña invalidos") );

        // 2. Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Email o contraseña inválidos");
        }

        // 3. Validar que el usuario esté activo
        if (!user.getStatus().name().equals("ACTIVE")) {
            throw new ApiException("El usuario no está activo");
        }

        // 4. Generar tokens JWT
        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateToken(user.getEmail());

        // 5. Retornar respuesta con tokens
        UserResponse userResponse = mapToUserResponse(user);
        return new LoginResponse(accessToken, refreshToken, userResponse);
    }

    // ============= MAPEAR USER A USERRESPONSE =============
    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getEmailVerified()
        );
    }

    // ============= VERIFICAR EMAIL =============
@Transactional
public void verifyEmail(String token) {
    // 1. Buscar usuario con ese token
    User user = userRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new ApiException("Token de verificación inválido o expirado"));

    // 2. Validar que el token no haya expirado
    if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
        throw new ApiException("Token de verificación expirado");
    }

    // 3. Marcar email como verificado
    user.setEmailVerified(true);
    user.setEmailVerificationToken(null);
    user.setEmailVerificationTokenExpiry(null);
    
    // 4. Guardar cambios
    userRepository.save(user);
}
}