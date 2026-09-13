package com.garicharo.shop.identity;

public record AuthUserResponse(String email, String role, String displayName) {
}
