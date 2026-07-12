package com.specguard.auth;

import com.specguard.auth.dto.AuthResponse;
import com.specguard.auth.dto.LoginRequest;
import com.specguard.auth.dto.RegisterRequest;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository _userRepository;
    private final PasswordEncoder _passwordEncoder;
    private final JwtService _jwtService;

    public AuthResponse register(@NonNull RegisterRequest request) {
        if (_userRepository.existsByEmail(request.email())) {
            log.warn("Registration rejected, email already exists: {}", request.email());
            throw new IllegalArgumentException("an account with this email already exists");
        }
        User user = new User(request.email(), _passwordEncoder.encode(request.password()));
        user = _userRepository.save(user);
        // TODO: encrypt email before logging for now keeping it for testing purpose
        log.info("Registered new user: {}", user.getEmail());
        return new AuthResponse(_jwtService.issueToken(user), user.getEmail());
    }

    public AuthResponse login(@NonNull LoginRequest request) {
        User user = _userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed, no account for email: {}", request.email());
                    return new BadCredentialsException("invalid email or password");
                });

        if (!_passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed, bad password for email: {}", request.email());
            throw new BadCredentialsException("invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(_jwtService.issueToken(user), user.getEmail());
    }

    // TODO(phase-later): rate-limit login attempts to avoid brute-force/white-washing attacks
    // TODO(phase-later): password reset flow (fancy shyt) 
}
