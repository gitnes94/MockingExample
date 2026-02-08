package com.example.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCart {
    private final ItemRegistry registry;
    private final Map<String, Integer> items = new HashMap<>();
    private final List<Discount> discounts = new ArrayList<>();

    public ShoppingCart(ItemRegistry registry) {
        this.registry = registry;
    }

    public void applyDiscount(Discount discount) {
        discounts.add(discount);
    }

    public void addItem(Item item) {
        addItem(item, 1);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.size();
    }

    public boolean containsItem(String itemId) {
        return items.containsKey(itemId);
    }

    public void addItem(Item item, int quantity) {
        if (item == null) {
            throw new IllegalArgumentException("Item kan inte vara null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Kvantitet måste vara större än 0");
        }


        items.merge(item.getId(), quantity, Integer::sum);
    }

    public int getQuantity(String itemId) {
        return items.getOrDefault(itemId, 0);
    }

    public void removeItem(String itemId) {
        items.remove(itemId);
    }

    public void updateQuantity(String itemId, int quantity) {
        if (!items.containsKey(itemId)) {
            throw new IllegalArgumentException("Varan finns inte i Shopping-carten");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Kvantitet kan inte vara negativ");
        }
        if (quantity == 0) {
            items.remove(itemId);
        } else {
            items.put(itemId, quantity);
        }
    }

    public double getTotal() {
        double subtotal = items.entrySet().stream()
                .mapToDouble(entry -> {
                    Item item = registry.getItem(entry.getKey());
                    return item.getPrice() * entry.getValue();
                })
                .sum();

        double total = subtotal;
        for (Discount discount : discounts) {
            total = discount.apply(total);
        }

        return total;
    }
}