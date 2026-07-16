package com.specguard.auth;

import com.specguard.auth.dto.AuthResponse;
import com.specguard.auth.dto.LoginRequest;
import com.specguard.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String EMAIL = "dev@example.com";
    private static final String PASSWORD = "correct-horse-battery-staple";
    private static final AuthResponse RESPONSE = new AuthResponse("fake.jwt.token", EMAIL);

    @Mock
    private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void register_returns201WithServiceResponse() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD);
        when(authService.register(request)).thenReturn(RESPONSE);

        ResponseEntity<AuthResponse> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(RESPONSE);
        verify(authService).register(request);
    }

    @Test
    void login_returns200WithServiceResponse() {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        when(authService.login(request)).thenReturn(RESPONSE);

        ResponseEntity<AuthResponse> result = controller.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(RESPONSE);
        verify(authService).login(request);
    }

    @Test
    void handleConflict_maps_illegalArgumentException_to409WithMessage() {
        var result = controller.handleConflict(new IllegalArgumentException("an account with this email already exists"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().message()).isEqualTo("an account with this email already exists");
    }

    @Test
    void handleBadCredentials_maps_badCredentialsException_to401WithMessage() {
        var result = controller.handleBadCredentials(new BadCredentialsException("invalid email or password"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().message()).isEqualTo("invalid email or password");
    }
}