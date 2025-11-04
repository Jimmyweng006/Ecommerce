package com.jimmyweng.ecommerce.exception;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.controller.common.ApiResponseEnvelope;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseEnvelope> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildExceptionResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ErrorMessages.VALIDATION_FAILED;
        }
        return buildExceptionResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponseEnvelope> handleAuthentication(AuthenticationException ex) {
        return buildExceptionResponse(HttpStatus.UNAUTHORIZED, ErrorMessages.AUTHENTICATION_FAILED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseEnvelope> handleAccessDenied(AccessDeniedException ex) {
        return buildExceptionResponse(HttpStatus.FORBIDDEN, ErrorMessages.ACCESS_DENIED);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponseEnvelope> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return buildExceptionResponse(HttpStatus.CONFLICT, ErrorMessages.RESOURCE_MODIFIED);
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ApiResponseEnvelope> handleOutOfStock(OutOfStockException ex) {
        return buildExceptionResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseEnvelope> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildExceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.UNEXPECTED_ERROR);
    }

    private ResponseEntity<ApiResponseEnvelope> buildExceptionResponse(HttpStatus status, String message) {
        Map<String, Object> meta = Map.of("timestamp", Instant.now().toString());
        ApiResponseEnvelope envelope = new ApiResponseEnvelope(-1, message, Collections.emptyMap(), meta);

        return ResponseEntity.status(status).body(envelope);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}
