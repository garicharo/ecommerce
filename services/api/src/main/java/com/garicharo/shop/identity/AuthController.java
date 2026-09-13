package com.garicharo.shop.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.garicharo.shop.shared.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUserResponse signup(
            @RequestBody SignupRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.signup(body);
        return authService.login(body.email(), body.password(), request, response);
    }

    @PostMapping("/login")
    public AuthUserResponse login(
            @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (body == null) {
            throw new ApiException("INVALID_BODY", "email and password are required", HttpStatus.BAD_REQUEST);
        }
        return authService.login(body.email(), body.password(), request, response);
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        var user = currentUser.require(authentication);
        return new AuthUserResponse(user.getEmail(), user.getRole().name(), user.getDisplayName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}
