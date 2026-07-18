package com.brandpilot.backend.common.error;

public record FieldErrorResponse(
        String field,
        String reason
) {
}
