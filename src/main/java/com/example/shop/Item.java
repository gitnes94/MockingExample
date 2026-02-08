package com.example.shop;

// Representerar en vara i shoppingcarten.
public record Item(String id, String name, double price) {
    public Item {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID kan inte vara null eller tomt");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name kan inte vara null eller tomt");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price kan inte vara negativt");
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id.equals(item.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}