package com.example.shop.service;

public record Reservation(Long productId, int quantity, long soldOutProducts) {
}
