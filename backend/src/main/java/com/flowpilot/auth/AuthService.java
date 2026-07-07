package com.flowpilot.auth;

import com.flowpilot.common.exception.ApplicationException;
import com.flowpilot.user.AppUser;
import com.flowpilot.user.UserRepository;
import com.flowpilot.user.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthenticatedSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(HttpStatus.CONFLICT, "A user with this email already exists.");
        }

        AppUser user = new AppUser(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                UserRole.SUPPORT_AGENT
        );

        AppUser savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthenticatedSession login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        if(!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Authentication authentication) {
        AppUser user = userRepository.findByEmail(normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new ApplicationException(HttpStatus.UNAUTHORIZED, "Authenticated user not found."));

        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    private AuthenticatedSession buildAuthResponse(AppUser user) {
        String token = jwtTokenService.generateToken(user);
        AuthResponse.UserSummary userSummary = new AuthResponse.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole()
        );

        return new AuthenticatedSession(token, jwtTokenService.getExpirationSeconds(), userSummary);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
