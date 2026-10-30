package com.example.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;
    private String email;
    private String tier;
    private boolean active;

    protected Customer() {
    }

    public Customer(String email, String tier) {
        this.email = email;
        this.tier = tier;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getTier() { return tier; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
