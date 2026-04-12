package com.kjt.lms.exception;

import com.kjt.lms.common.response.APIResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error at {}: {}", request.getRequestURI(), errors);

        APIResponse<Map<String, String>> response = APIResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle Entity Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        log.error("Entity not found at {}: {}", request.getRequestURI(), ex.getMessage());

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle Illegal Argument
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.error("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle Access Denied
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.error("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.FORBIDDEN.value(),
                "Access denied: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle Type Mismatch (path variable, request param type mismatch)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        assert ex.getRequiredType() != null;
        String error = String.format("Parameter '%s' should be of type %s",
                ex.getName(), ex.getRequiredType().getSimpleName());

        log.error("Type mismatch at {}: {}", request.getRequestURI(), error);

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                error
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle Custom Business Exception
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIResponse<Object>> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.error("Business exception at {}: {}", request.getRequestURI(), ex.getMessage());

        int statusCode = ex.getErrorCode() != null ? ex.getErrorCode() : HttpStatus.BAD_REQUEST.value();

        APIResponse<Object> response = APIResponse.error(statusCode, ex.getMessage());

        return ResponseEntity.status(statusCode).body(response);
    }

    /**
     * Handle Duplicate Resource Exception
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<APIResponse<Object>> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.error("Duplicate resource at {}: {}", request.getRequestURI(), ex.getMessage());

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle Resource Not Found Exception
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.error("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle Runtime Exception (fallback cho tất cả RuntimeException không được handle cụ thể)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<APIResponse<Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        log.error("Runtime exception at {}: ", request.getRequestURI(), ex);

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Lỗi hệ thống. Vui lòng thử lại sau."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Object>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);

        APIResponse<Object> response = APIResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An internal server error occurred. Please try again later."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}