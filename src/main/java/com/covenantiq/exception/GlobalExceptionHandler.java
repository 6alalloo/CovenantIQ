package com.covenantiq.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ProblemDetail handleUnprocessable(UnprocessableEntityException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Unprocessable Entity");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        pd.setTitle("Validation Error");
        pd.setInstance(URI.create(request.getRequestURI()));

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        pd.setProperty("errors", fieldErrors);
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Validation Error");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuth(AuthenticationFailedException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Authentication Failed");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Forbidden");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(Exception ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action"
        );
        pd.setTitle("Forbidden");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
        pd.setTitle("Internal Server Error");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ProblemDetail handlePayloadTooLarge(PayloadTooLargeException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        pd.setTitle("Payload Too Large");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ProblemDetail handleUnsupportedType(UnsupportedFileTypeException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        pd.setTitle("Unsupported Media Type");
        pd.setInstance(URI.create(request.getRequestURI()));
        addMeta(pd);
        return pd;
    }

    private void addMeta(ProblemDetail problemDetail) {
        problemDetail.setProperty("timestamp", java.time.OffsetDateTime.now().toString());
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            problemDetail.setProperty("correlationId", correlationId);
        }
    }
}
