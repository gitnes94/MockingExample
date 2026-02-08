package com.example.shop;

import java.util.HashMap;
import java.util.Map;

public class ShoppingCart {
    private final Map<String, Integer> items = new HashMap<>();

    public void addItem(Item item) {
        items.put(item.getId(), 1);
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
}