package com.example.billingapp.models;

public class Product {
    private String id;
    private String name;
    private double pricePerUnit;
    private String unitType; // "KG" or "L"

    public Product() {
        // Required for Firestore
    }

    public Product(String id, String name, double pricePerUnit, String unitType) {
        this.id = id;
        this.name = name;
        this.pricePerUnit = pricePerUnit;
        this.unitType = unitType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }
}
