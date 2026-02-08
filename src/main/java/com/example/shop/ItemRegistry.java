package com.example.shop;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private final Map<String, Item> items = new HashMap<>();

    public void registerItem(Item item) {
        items.put(item.getId(), item);
    }

    public Item getItem(String itemId) {
        return items.get(itemId);
    }
}