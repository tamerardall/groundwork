package com.example.shop.service;

import com.example.shop.client.PaymentGateway;
import com.example.shop.domain.Order;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(OrderRepository orderRepository, PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public void capture(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        order.setStatus("PAID");
        paymentGateway.charge(orderId, order.getTotal());
    }
}
