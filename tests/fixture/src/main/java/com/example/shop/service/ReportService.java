package com.example.shop.service;

import com.example.shop.client.FxClient;
import com.example.shop.domain.Order;
import com.example.shop.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final FxClient fxClient;

    public ReportService(OrderRepository orderRepository, FxClient fxClient) {
        this.orderRepository = orderRepository;
        this.fxClient = fxClient;
    }

    @Transactional(readOnly = true)
    public MonthlyReport monthly() {
        BigDecimal rate = fxClient.rate("EUR");
        List<Order> orders = orderRepository.findAll();
        BigDecimal total = orders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String firstCustomer = orders.isEmpty() ? null : orders.get(0).getCustomer().getEmail();
        return new MonthlyReport(total.multiply(rate), firstCustomer);
    }
}
