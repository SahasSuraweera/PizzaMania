package com.example.pizzamania;

import java.util.List;

public class OrderModel {
    public String userUid;
    public String deliveryAddress;
    public String nearestBranch;
    public double subtotal;
    public double deliveryFee;
    public double totalFee;
    public String paymentMethod;
    public String orderStatus;
    public String orderDate;
    public String orderTime;
    // Extra fields for displaying orders
    private String orderId;               // Firebase key for this order
    private List<OrderItem> items;        // Items in this order

    // Empty constructor required for Firebase
    public OrderModel() {}

    public OrderModel(String orderID, String userUid, String deliveryAddress, String nearestBranch,
                      double subtotal, double deliveryFee, double totalFee,
                      String paymentMethod, String orderStatus, String orderDate, String orderTime) {
        this.orderId = orderID;
        this.userUid = userUid;
        this.deliveryAddress = deliveryAddress;
        this.nearestBranch = nearestBranch;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.totalFee = totalFee;
        this.paymentMethod = paymentMethod;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.orderTime = orderTime;
    }

    // Getter and Setter for orderId
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderId() { return orderId; }

    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderStatus() { return orderStatus; }

    public void setTotalFee(Double totalFee) { this.totalFee = totalFee; }
    public Double getTotalFee() { return totalFee; }

    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public String getOrderDate() { return orderDate; }

    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }
    public String getOrderTime() { return orderTime; }

    // Getter and Setter for items
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
