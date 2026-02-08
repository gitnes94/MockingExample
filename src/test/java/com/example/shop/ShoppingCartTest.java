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
        ItemRegistry registry = new ItemRegistry();
        cart = new ShoppingCart(registry);

        apple = new Item("1", "Apple", 10.0);
        banana = new Item("2", "Banana", 5.0);

        registry.registerItem(apple);
        registry.registerItem(banana);
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

    @Test
    @DisplayName("Ska kunna ta bort en vara från carten")
    void shouldRemoveItem() {
        cart.addItem(apple);
        cart.addItem(banana);

        cart.removeItem(apple.getId());

        assertThat(cart.containsItem(apple.getId())).isFalse();
        assertThat(cart.getItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ska kunna uppdatera kvantitet för en vara")
    void shouldUpdateQuantity() {
        cart.addItem(apple, 3);

        cart.updateQuantity(apple.getId(), 5);

        assertThat(cart.getQuantity(apple.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("Ska ta bort vara när kvantitet sätts till 0")
    void shouldRemoveItemWhenQuantitySetToZero() {
        cart.addItem(apple);

        cart.updateQuantity(apple.getId(), 0);

        assertThat(cart.containsItem(apple.getId())).isFalse();
    }

    @Test
    @DisplayName("Ska beräkna totalpris för en vara")
    void shouldCalculateTotalForSingleItem() {
        cart.addItem(apple, 3);

        assertThat(cart.getTotal()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Ska beräkna totalpris för flera varor")
    void shouldCalculateTotalForMultipleItems() {
        cart.addItem(apple, 2);  // 2 * 10 = 20
        cart.addItem(banana, 3); // 3 * 5 = 15

        assertThat(cart.getTotal()).isEqualTo(35.0);
    }

    @Test
    @DisplayName("Ska applicera procentrabatt")
    void shouldApplyPercentageDiscount() {
        cart.addItem(apple, 2);  // 20.0

        Discount discount = new PercentageDiscount(10); // 10% off
        cart.applyDiscount(discount);

        assertThat(cart.getTotal()).isEqualTo(18.0);
    }

    @Test
    @DisplayName("Ska kunna applicera flera rabatter")
    void shouldApplyMultipleDiscounts() {
        cart.addItem(apple, 2);  // 20.0

        cart.applyDiscount(new PercentageDiscount(10)); // 18.0
        cart.applyDiscount(new PercentageDiscount(10)); // 16.2

        assertThat(cart.getTotal()).isCloseTo(16.2, within(0.01));
    }

    @Test
    @DisplayName("Ska kasta exception när man lägger till null vara")
    void shouldThrowExceptionWhenAddingNullItem() {
        assertThatThrownBy(() -> cart.addItem(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ska kasta exception vid negativ kvantitet")
    void shouldThrowExceptionForNegativeQuantity() {
        assertThatThrownBy(() -> cart.addItem(apple, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ska returnera 0 för tom cart")
    void shouldReturnZeroForEmptyCart() {
        assertThat(cart.getTotal()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Ska hantera uppdatering av icke-existerande vara")
    void shouldHandleUpdatingNonExistentItem() {
        assertThatThrownBy(() -> cart.updateQuantity("999", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}