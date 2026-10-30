package com.example.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    private String status;
    private BigDecimal total;
    private Instant createdAt;

    protected Order() {
    }

    public Order(Customer customer, BigDecimal total, Instant createdAt) {
        this.customer = customer;
        this.total = total;
        this.createdAt = createdAt;
        this.status = "NEW";
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
}
