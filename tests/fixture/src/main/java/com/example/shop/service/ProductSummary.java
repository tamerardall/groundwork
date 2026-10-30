package com.example.shop.service;

import java.math.BigDecimal;

public record ProductSummary(Long productId, BigDecimal price, int available) {
}
