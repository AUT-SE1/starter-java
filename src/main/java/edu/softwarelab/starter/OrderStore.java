package edu.softwarelab.starter;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Orders held in memory. A real service would use a database. */
@Component
public class OrderStore {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order create(String item, int quantity) {
        Order order = new Order(newId(), item, quantity, Orders.STATUS_CREATED);
        orders.put(order.id(), order);
        return order;
    }

    public Optional<Order> get(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    /** Reset between tests, so no test inherits another test's state. */
    public void clear() {
        orders.clear();
    }

    private static String newId() {
        byte[] buffer = new byte[8];
        RANDOM.nextBytes(buffer);
        return "ord_" + HexFormat.of().formatHex(buffer);
    }
}
