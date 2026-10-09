package com.jobtracker.main;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import java.util.Arrays;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException exception,
                                                               @NonNull HttpHeaders headers,
                                                               @NonNull HttpStatusCode status,
                                                               @NonNull WebRequest request) {
        LinkedMultiValueMap<String, Object> errors = new LinkedMultiValueMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.add(error.getField(), error.getDefaultMessage());
        }
        exception.getBody().setProperty("errors", errors);
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException exception,
                                                                @NonNull HttpHeaders headers,
                                                                @NonNull HttpStatusCode status,
                                                                @NonNull WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Could not read HTTP message.");
        if(exception.getCause() != null) {
            switch (exception.getCause()){
                case InvalidFormatException invalidFormatException -> {
                    String error = getErrorMessage(invalidFormatException);
                    problemDetail.setProperty("error", error);
                }

                case StreamReadException ignored ->
                        problemDetail.setProperty("error", "Malformed JSON request");
                default ->
                        logger.debug("Unreadable request body", exception);
            }
        }
        return handleExceptionInternal(exception, problemDetail, headers, status, request);
    }

    private static @NonNull String getErrorMessage(InvalidFormatException invalidFormatException) {
        String error = "\"" + invalidFormatException.getValue();
        error += invalidFormatException.getTargetType().isEnum() ?
                "\": not one of the values accepted for " +
                invalidFormatException.getPath().getLast().getPropertyName() +
                ": " + Arrays.toString(invalidFormatException.getTargetType().getEnumConstants()) + "."
                : "invalid value for field " + invalidFormatException.getPath().getLast().getPropertyName() + ".";
        return error;
    }
    @ExceptionHandler(IllegalStatusTransitionException.class)
    public ProblemDetail handleIllegalStatusTransitionException(@NonNull IllegalStatusTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleObjectOptimisticLockingFailureException(@NonNull ObjectOptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Requested object has been recently modified, reload and try again");
    }
}
