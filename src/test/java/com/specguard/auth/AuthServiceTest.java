package com.specguard.auth;

import com.specguard.auth.dto.AuthResponse;
import com.specguard.auth.dto.LoginRequest;
import com.specguard.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}'s register/login logic. Mocks {@link UserRepository}
 * and {@link JwtService} (external collaborators), but uses a real {@link BCryptPasswordEncoder}
 * rather than mocking it — it's a thin, already-correct wrapper, and mocking password
 * matching would mean the test no longer exercises the actual hash/compare behavior it's
 * meant to protect. No Spring context or database needed, same pattern as
 * {@code RiskScoreCalculatorTest}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "dev@example.com";
    private static final String RAW_PASSWORD = "correct-horse-battery-staple";
    private static final String TOKEN = "fake.jwt.token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_savesUserAndReturnsToken_whenEmailNotTaken() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issueToken(any(User.class))).thenReturn(TOKEN);

        AuthResponse response = authService.register(new RegisterRequest(EMAIL, RAW_PASSWORD));

        assertThat(response).isEqualTo(new AuthResponse(TOKEN, EMAIL));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_hashesPassword_neverStoresRawPassword() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issueToken(any(User.class))).thenReturn(TOKEN);

        authService.register(new RegisterRequest(EMAIL, RAW_PASSWORD));

        verify(userRepository).save(argThatSavedUser(user ->
                !user.getPasswordHash().equals(RAW_PASSWORD)
                        && passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())));
    }

    @Test
    void register_throwsIllegalArgumentException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_returnsToken_whenCredentialsCorrect() {
        User existing = new User(EMAIL, passwordEncoder.encode(RAW_PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(jwtService.issueToken(existing)).thenReturn(TOKEN);

        AuthResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response).isEqualTo(new AuthResponse(TOKEN, EMAIL));
    }

    @Test
    void login_throwsBadCredentialsException_whenEmailNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_throwsBadCredentialsException_whenPasswordWrong() {
        User existing = new User(EMAIL, passwordEncoder.encode(RAW_PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    /** Small local helper so the "saved user satisfies this predicate" assertion reads cleanly. */
    private static User argThatSavedUser(java.util.function.Predicate<User> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}