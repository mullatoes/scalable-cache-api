package com.jdevs.scalablecacheapi.auth;


import com.jdevs.scalablecacheapi.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshAccessToken(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return authService.logout(request, authorizationHeader);
    }
}
