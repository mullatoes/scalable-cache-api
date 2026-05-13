package com.jdevs.scalablecacheapi.security;

import com.jdevs.scalablecacheapi.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) {
        writeResponse(response, request, HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required");
    }

    private void writeResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String error,
            String message
    ) {
        try {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),
                    status.value(),
                    error,
                    message,
                    request.getRequestURI()
            );

            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        } catch (Exception ignored) {
            response.setStatus(status.value());
        }
    }
}
