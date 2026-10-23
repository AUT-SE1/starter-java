package edu.softwarelab.starter;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderStore store;

    public OrderController(OrderStore store) {
        this.store = store;
    }

    @PostMapping(value = "/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> create(@RequestBody(required = false) CreateOrderRequest request) {
        Orders.Validated validated = Orders.validateCreateOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(store.create(validated.item(), validated.quantity()));
    }

    @GetMapping(value = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable String id) {
        return store.get(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiError.notFound("no order with that id")));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> onValidationFailure(ValidationException failure) {
        return ResponseEntity.badRequest().body(failure.asBody());
    }

    /**
     * Spring's own 400 for unparseable JSON carries a different body shape, so
     * translate it into the error shape the contract promises.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadableBody(HttpMessageNotReadableException ignored) {
        return ResponseEntity.badRequest()
                .body(ApiError.validation("body", "request body is not valid JSON"));
    }
}
