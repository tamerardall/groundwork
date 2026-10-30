package com.example.shop.service;

import java.math.BigDecimal;

public record MonthlyReport(BigDecimal totalEur, String firstCustomerEmail) {
}
