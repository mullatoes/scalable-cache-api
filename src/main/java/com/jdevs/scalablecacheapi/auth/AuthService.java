package com.jdevs.scalablecacheapi.auth;

import com.jdevs.scalablecacheapi.dto.LoginRequest;
import com.jdevs.scalablecacheapi.dto.RegisterRequest;
import com.jdevs.scalablecacheapi.security.JwtService;
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

        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, "Bearer");
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

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer");
    }
}
