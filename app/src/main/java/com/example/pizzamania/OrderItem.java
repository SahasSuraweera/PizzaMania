package com.example.pizzamania;

public class OrderItem {
    private String name;
    private String size;
    private double price;
    private int quantity;

    public OrderItem() {}

    public String getName() { return name; }
    public String getSize() { return size; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void setName(String name) { this.name = name; }
    public void setSize(String size) { this.size = size; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
