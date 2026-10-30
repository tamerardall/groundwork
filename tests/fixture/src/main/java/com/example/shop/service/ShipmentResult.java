package com.example.shop.service;

public record ShipmentResult(Long orderId, long shippedOrders, int lowStockProducts) {
}
