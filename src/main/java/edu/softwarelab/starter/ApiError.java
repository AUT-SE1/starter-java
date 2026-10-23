package edu.softwarelab.starter;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The body returned with every 4xx response.
 *
 * <p>Null fields are left out rather than serialised as {@code null}: the
 * shared contract types {@code field} and {@code message} as strings, and a
 * null would fail a neighbouring team's schema validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, String field, String message) {

    public static ApiError validation(String field, String message) {
        return new ApiError(Orders.ERR_VALIDATION, field, message);
    }

    public static ApiError notFound(String message) {
        return new ApiError(Orders.ERR_NOT_FOUND, null, message);
    }
}
