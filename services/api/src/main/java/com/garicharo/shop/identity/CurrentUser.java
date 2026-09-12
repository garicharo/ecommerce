package com.garicharo.shop.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.garicharo.shop.shared.ApiException;

@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ApiException("UNAUTHORIZED", "Not logged in", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "Not logged in", HttpStatus.UNAUTHORIZED));
    }
}
