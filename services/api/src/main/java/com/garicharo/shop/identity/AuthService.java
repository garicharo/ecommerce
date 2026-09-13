package com.garicharo.shop.identity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garicharo.shop.shared.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public void signup(SignupRequest body) {
        if (body == null) {
            throw new ApiException("INVALID_BODY", "email, password, and displayName are required", HttpStatus.BAD_REQUEST);
        }
        String email = body.email() == null ? "" : body.email().trim().toLowerCase();
        String displayName = body.displayName() == null ? "" : body.displayName().trim();
        String password = body.password() == null ? "" : body.password();
        if (email.isBlank() || displayName.isBlank()) {
            throw new ApiException("INVALID_BODY", "email, password, and displayName are required", HttpStatus.BAD_REQUEST);
        }
        if (password.length() < 8) {
            throw new ApiException("WEAK_PASSWORD", "Password must be at least 8 characters", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ApiException("EMAIL_TAKEN", "An account with that email already exists", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.SHOPPER);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
    }

    public AuthUserResponse login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank() || password == null || password.isBlank()) {
            throw new ApiException("UNAUTHORIZED", "Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalized, password));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        } catch (AuthenticationException ex) {
            throw new ApiException("UNAUTHORIZED", "Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "Invalid credentials", HttpStatus.UNAUTHORIZED));
        return new AuthUserResponse(user.getEmail(), user.getRole().name(), user.getDisplayName());
    }
}
