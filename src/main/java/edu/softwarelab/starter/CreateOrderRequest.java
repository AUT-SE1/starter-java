package edu.softwarelab.starter;

/**
 * The request body of POST /orders.
 *
 * <p>Both fields are boxed so that a missing field can be told apart from a
 * zero value: {"quantity": 0} and {} must produce different messages.
 */
public record CreateOrderRequest(String item, Integer quantity) {
}
