package com.jdevs.scalablecacheapi.security;

import com.jdevs.scalablecacheapi.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) {
        writeResponse(response, request, HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource");
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
