package com.jdevs.scalablecacheapi.auth;

public record AuthResponse (
        String accessToken,
        String tokenType
) {
}
