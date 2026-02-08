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
        assertThat(cart.containsItem(apple.getId())).isTrue();
    }

    @Test
    @DisplayName("Ska kunna lägga till flera olika varor")
    void shouldAddMultipleItems() {
        cart.addItem(apple);
        cart.addItem(banana);

        assertThat(cart.getItemCount()).isEqualTo(2);
        assertThat(cart.containsItem(apple.getId())).isTrue();
        assertThat(cart.containsItem(banana.getId())).isTrue();
    }

    @Test
    @DisplayName("Ska öka kvantitet när samma vara läggs till igen")
    void shouldIncreaseQuantityWhenAddingSameItem() {
        cart.addItem(apple, 1);
        cart.addItem(apple, 1);

        assertThat(cart.getItemCount()).isEqualTo(1); // Fortfarande 1 unik vara
        assertThat(cart.getQuantity(apple.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("Ska kunna lägga till vara med specifik kvantitet")
    void shouldAddItemWithQuantity() {
        cart.addItem(apple, 5);

        assertThat(cart.getQuantity(apple.getId())).isEqualTo(5);
    }
}