package com.example.billingapp.models;

public class CartItem extends Product {
    private double quantity;
    private double totalPrice;

    public CartItem() {
        super();
    }

    public CartItem(Product product, double quantity) {
        super(product.getId(), product.getName(), product.getPricePerUnit(), product.getUnitType());
        this.quantity = quantity;
        this.totalPrice = product.getPricePerUnit() * quantity;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        this.totalPrice = this.getPricePerUnit() * quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
