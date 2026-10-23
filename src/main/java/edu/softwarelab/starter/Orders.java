package edu.softwarelab.starter;

/**
 * Order validation, kept apart from the web layer so the tests for it are
 * unit tests: no HTTP, no network, no storage.
 *
 * <p>The bounds are part of the shared contract in openapi.yaml. Keep them and
 * the specification in step with each other.
 */
public final class Orders {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 1000;
    public static final int MAX_ITEM_LENGTH = 64;

    public static final String STATUS_CREATED = "created";
    public static final String ERR_VALIDATION = "validation_failed";
    public static final String ERR_NOT_FOUND = "not_found";

    private Orders() {
    }

    /** The cleaned values of a request body that satisfied the contract. */
    public record Validated(String item, int quantity) {
    }

    /**
     * Checks one request body and returns the cleaned values.
     *
     * @throws ValidationException naming the first field that is wrong
     */
    public static Validated validateCreateOrder(CreateOrderRequest request) {
        if (request == null || request.item() == null) {
            throw new ValidationException("item", "item is required");
        }
        String item = request.item().strip();
        if (item.isEmpty()) {
            throw new ValidationException("item", "item must not be empty");
        }
        // codePointCount, not length: the limit is 64 characters, not 64 chars
        // of UTF-16, so an emoji or a surrogate pair counts once.
        if (item.codePointCount(0, item.length()) > MAX_ITEM_LENGTH) {
            throw new ValidationException("item", "item is longer than 64 characters");
        }

        if (request.quantity() == null) {
            throw new ValidationException("quantity", "quantity is required");
        }
        int quantity = request.quantity();
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new ValidationException("quantity", "quantity must be between 1 and 1000");
        }

        return new Validated(item, quantity);
    }
}
