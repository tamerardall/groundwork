package com.example.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;
    private BigDecimal price;
    private int stock;

    protected Product() {
    }

    public Product(BigDecimal price, int stock) {
        this.price = price;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
