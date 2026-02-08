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

    @Test
    @DisplayName("Ska kunna lägga till en vara")
    void shouldAddSingleItem() {
        cart.addItem(apple);

        assertThat(cart.isEmpty()).isFalse();
        assertThat(cart.getItemCount()).isEqualTo(1);
        assertThat(cart.containsItem(apple.id())).isTrue();
    }

    @Test
    @DisplayName("Ska kunna lägga till flera olika varor")
    void shouldAddMultipleItems() {
        cart.addItem(apple);
        cart.addItem(banana);

        assertThat(cart.getItemCount()).isEqualTo(2);
        assertThat(cart.containsItem(apple.id())).isTrue();
        assertThat(cart.containsItem(banana.id())).isTrue();
    }
}