package com.example.shop.service;

import com.example.shop.domain.Order;
import com.example.shop.exception.InvalidRequestException;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private final OrderRepository orderRepository;

    public InvoiceService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String buildInvoice(Long orderId) {
        InvoiceDraft draft = InvoiceDraft.forOrder(orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        if (!"PAID".equals(order.getStatus())) {
            throw new InvalidRequestException("Order is not paid: " + orderId);
        }
        return draft.render(order.getTotal());
    }
}
