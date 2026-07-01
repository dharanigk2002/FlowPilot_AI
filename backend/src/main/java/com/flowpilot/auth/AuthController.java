package com.flowpilot.auth;

import com.flowpilot.common.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return issueSession(authService.register(request), httpRequest, httpResponse);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return issueSession(authService.login(request), httpRequest, httpResponse);
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieService.clear(response);
        csrfTokenRepository.saveToken(null, request, response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public CurrentUserResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }

    private AuthResponse issueSession(
            AuthenticatedSession session,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authCookieService.write(response, session.accessToken(), session.expiresInSeconds());
        csrfTokenRepository.saveToken(null, request, response);
        return session.toResponse();
    }
}
