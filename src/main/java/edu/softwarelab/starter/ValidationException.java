package edu.softwarelab.starter;

/** Thrown when a request body does not satisfy the contract. */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }

    public ApiError asBody() {
        return ApiError.validation(field, getMessage());
    }
}
