package edu.softwarelab.starter.entity;

/** The response body of POST /orders and GET /orders/{id}. */
public record Order(String id, String item, int quantity, String status) {
}
