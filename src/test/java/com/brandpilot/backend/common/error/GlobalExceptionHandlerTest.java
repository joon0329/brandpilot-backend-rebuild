package com.brandpilot.backend.common.error;

import com.brandpilot.backend.user.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void handleDuplicateEmail_returnsConflictResponse_whenDuplicateEmailExceptionOccurs() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateEmail(
                new DuplicateEmailException(),
                request
        );
        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode()),
                () -> assertEquals(409, body.status()),
                () -> assertEquals("DUPLICATE_EMAIL", body.code()),
                () -> assertEquals("이미 사용 중인 이메일입니다.", body.message()),
                () -> assertEquals("/api/v1/users", body.path()),
                () -> assertEquals(ZoneOffset.ofHours(9), body.timestamp().getOffset()),
                () -> assertTrue(body.fieldErrors().isEmpty())
        );
    }
}
