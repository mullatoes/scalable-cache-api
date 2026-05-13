package com.jdevs.scalablecacheapi.auth;

import com.jdevs.scalablecacheapi.dto.*;
import com.jdevs.scalablecacheapi.entity.RefreshToken;
import com.jdevs.scalablecacheapi.security.JwtService;
import com.jdevs.scalablecacheapi.service.RefreshTokenService;
import com.jdevs.scalablecacheapi.user.AppUser;
import com.jdevs.scalablecacheapi.user.AppUserRepository;
import com.jdevs.scalablecacheapi.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        AppUser user = AppUser.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        AppUser savedUser = appUserRepository.save(user);

        String accessToken = jwtService.generateToken(savedUser);

        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid login credentials"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());

        AppUser user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user);

        return new AuthResponse(newAccessToken, request.refreshToken(), "Bearer");
    }

    public MessageResponse logout(LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());

        return new MessageResponse("Logout successful");
    }
}
