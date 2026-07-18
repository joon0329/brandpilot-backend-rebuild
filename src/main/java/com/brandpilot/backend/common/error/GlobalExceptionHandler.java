package com.brandpilot.backend.common.error;

import com.brandpilot.backend.user.DuplicateEmailException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request
    ) {

        ErrorCode errorCode = ErrorCode.DUPLICATE_EMAIL;

        ErrorResponse response = new ErrorResponse(
                OffsetDateTime.now(ZoneId.of("Asia/Seoul")),
                errorCode.getStatus().value(),
                errorCode.name(),
                errorCode.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }
}
