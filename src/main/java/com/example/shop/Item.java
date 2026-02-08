package com.example.shop;

/*
 * Representerar en vara i shoppingcarten.
 */
public class Item {
    private final String id;
    private final String name;
    private final double price;

    public Item(String id, String name, double price) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID kan inte vara null eller tomt");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name kan inte vara null eller tomt");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price kan inte vara negativt");
        }

        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
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