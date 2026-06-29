package com.flowpilot.common.web;

import java.util.List;

import com.flowpilot.common.exception.ApplicationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = exception.getStatus();
        ApiError error = ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiError.FieldErrorDetail> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ApiError error = ApiError.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed.",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccessDenied(HttpServletRequest request) {
        return ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "You do not have permission to perform this action.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiError handleMaxUploadSize(HttpServletRequest request) {
        return ApiError.of(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                "The uploaded file exceeds the 10 MB limit.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled exception while processing {}", request.getRequestURI(), exception);

        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiError.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return new ApiError.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
