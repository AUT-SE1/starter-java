package edu.softwarelab.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests: validation and storage, with no HTTP in sight. */
class OrdersTest {

    @Test
    void validateCreateOrder_whenRequestIsValid_returnsCleanedValues() {
        Orders.Validated validated =
                Orders.validateCreateOrder(new CreateOrderRequest("  notebook  ", 3));

        assertEquals("notebook", validated.item());
        assertEquals(3, validated.quantity());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(nullValues = "NULL", value = {
        "NULL,      1,    item is missing",
        "'',        1,    item is empty",
        "'   ',     1,    item is only whitespace",
        "book,      NULL, quantity is missing",
        "book,      0,    quantity is zero",
        "book,      -5,   quantity is negative",
        "book,      1001, quantity is above the maximum",
    })
    void validateCreateOrder_whenInputIsInvalid_namesTheOffendingField(
            String item, Integer quantity, String description) {
        ValidationException failure = assertThrows(ValidationException.class,
                () -> Orders.validateCreateOrder(new CreateOrderRequest(item, quantity)));

        String expectedField = description.startsWith("item") ? "item" : "quantity";
        assertEquals(expectedField, failure.field(), description);
        assertEquals(Orders.ERR_VALIDATION, failure.asBody().error());
    }

    @Test
    void validateCreateOrder_whenItemIsTooLong_isRejected() {
        String tooLong = "a".repeat(Orders.MAX_ITEM_LENGTH + 1);

        ValidationException failure = assertThrows(ValidationException.class,
                () -> Orders.validateCreateOrder(new CreateOrderRequest(tooLong, 1)));

        assertEquals("item", failure.field());
    }

    @ParameterizedTest
    @ValueSource(ints = {Orders.MIN_QUANTITY, Orders.MAX_QUANTITY})
    void validateCreateOrder_atTheQuantityBoundaries_succeeds(int quantity) {
        Orders.Validated validated =
                Orders.validateCreateOrder(new CreateOrderRequest("book", quantity));

        assertEquals(quantity, validated.quantity());
    }

    @Test
    void validateCreateOrder_whenItemIsNonAscii_keepsItIntact() {
        // The limit is 64 characters, not 64 bytes — a Persian item name must fit.
        Orders.Validated validated =
                Orders.validateCreateOrder(new CreateOrderRequest("کتاب", 1));

        assertEquals("کتاب", validated.item());
    }

    @Test
    void orderStore_afterCreate_returnsTheSameOrder() {
        OrderStore store = new OrderStore();

        Order created = store.create("notebook", 2);

        assertEquals(created, store.get(created.id()).orElseThrow());
        assertTrue(created.id().startsWith("ord_"), "expected the id to start with ord_");
        assertEquals(Orders.STATUS_CREATED, created.status());
    }

    @Test
    void orderStore_whenIdIsUnknown_returnsEmpty() {
        OrderStore store = new OrderStore();

        assertTrue(store.get("ord_does_not_exist").isEmpty());
    }

    @Test
    void orderStore_givesEachOrderItsOwnId() {
        OrderStore store = new OrderStore();

        assertTrue(!store.create("book", 1).id().equals(store.create("book", 1).id()));
    }
}
