package com.garicharo.shop.shared;

import java.util.List;

public record ApiError(String code, String message, List<?> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }

    public static ApiError from(ApiException ex) {
        return new ApiError(ex.getCode(), ex.getMessage(), ex.getDetails());
    }
}
