package com.example.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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

    @Test
    @DisplayName("Ska skapa en tom shoppingcart")
    void shouldCreateEmptyCart() {
        assertThat(cart).isNotNull();
        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.getItemCount()).isEqualTo(0);
    }
}