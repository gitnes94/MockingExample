package com.example.shop;

import java.util.HashMap;
import java.util.Map;

public class ShoppingCart {
    private final Map<String, Integer> items = new HashMap<>();

    public void addItem(Item item) {
        items.put(item.id(), 1);
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
}