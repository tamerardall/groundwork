package com.example.shop.service;

import com.example.shop.domain.Order;
import com.example.shop.domain.Product;
import com.example.shop.exception.InvalidRequestException;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public ShipmentService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ShipmentResult ship(Long orderId) {
        List<Product> lowStock = productRepository.findByStockLessThan(5);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        if (!"PAID".equals(order.getStatus())) {
            throw new InvalidRequestException("Order is not paid: " + orderId);
        }
        order.setStatus("SHIPPED");
        long shipped = orderRepository.countByStatus("SHIPPED");
        return new ShipmentResult(orderId, shipped, lowStock.size());
    }
}
