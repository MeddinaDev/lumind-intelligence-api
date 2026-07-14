package com.lumind.api.common.exception;

import com.lumind.api.habit.exception.HabitNotFoundException;
import com.lumind.api.pomodoro.exception.PomodoroSessionNotFoundException;
import com.lumind.api.task.exception.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MSG_VALIDATION_FAILED = "Validation failed";
    private static final String MSG_MALFORMED_JSON = "Malformed JSON request";
    private static final String MSG_EMAIL_ALREADY_REGISTERED = "Email is already registered";
    private static final String MSG_INVALID_CREDENTIALS = "Invalid email or password";
    private static final String MSG_ACCOUNT_DISABLED = "Account is disabled";
    private static final String MSG_INVALID_REFRESH_TOKEN = "Invalid or expired refresh token";
    private static final String MSG_HABIT_NOT_FOUND = "Habit not found";
    private static final String MSG_TASK_NOT_FOUND = "Task not found";
    private static final String MSG_POMODORO_SESSION_NOT_FOUND = "Pomodoro session not found";
    private static final String MSG_UNEXPECTED_ERROR = "An unexpected error occurred";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.debug("Validation failed for path: {}", request.getRequestURI());

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, MSG_VALIDATION_FAILED, request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.debug("Malformed JSON request for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.BAD_REQUEST, MSG_MALFORMED_JSON, request.getRequestURI(), null);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, MSG_EMAIL_ALREADY_REGISTERED, request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid credentials attempt for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_INVALID_CREDENTIALS, request.getRequestURI(), null);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
            AccountDisabledException ex,
            HttpServletRequest request
    ) {
        log.warn("Disabled account access attempt for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.FORBIDDEN, MSG_ACCOUNT_DISABLED, request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid refresh token attempt for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_INVALID_REFRESH_TOKEN, request.getRequestURI(), null);
    }

    @ExceptionHandler(HabitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHabitNotFound(
            HabitNotFoundException ex,
            HttpServletRequest request
    ) {
        log.debug("Habit not found for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.NOT_FOUND, MSG_HABIT_NOT_FOUND, request.getRequestURI(), null);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(
            TaskNotFoundException ex,
            HttpServletRequest request
    ) {
        log.debug("Task not found for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.NOT_FOUND, MSG_TASK_NOT_FOUND, request.getRequestURI(), null);
    }

    @ExceptionHandler(PomodoroSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePomodoroSessionNotFound(
            PomodoroSessionNotFoundException ex,
            HttpServletRequest request
    ) {
        log.debug("Pomodoro session not found for path: {}", request.getRequestURI());

        return buildResponse(HttpStatus.NOT_FOUND, MSG_POMODORO_SESSION_NOT_FOUND, request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error for path: {}", request.getRequestURI(), ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, MSG_UNEXPECTED_ERROR, request.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<FieldError> errors
    ) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errors
        );

        return ResponseEntity.status(status).body(body);
    }
}
