package com.aisandbox.auth.model;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
