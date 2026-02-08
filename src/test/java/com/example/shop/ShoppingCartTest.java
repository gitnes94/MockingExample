package com.example.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;


@DisplayName("ShoppingCart TDD Tests")
class ShoppingCartTest {

    private ShoppingCart cart;
    private Item apple;
    private Item banana;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
        apple = new Item("1", "Apple", 10.0);
        banana = new Item("2", "Banana", 5.0);
    }
}