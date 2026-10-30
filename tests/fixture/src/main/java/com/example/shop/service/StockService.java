package com.example.shop.service;

import com.example.shop.domain.Product;
import com.example.shop.exception.InvalidRequestException;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final ProductRepository productRepository;

    public StockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Reservation reserve(Long productId, int quantity) {
        long soldOut = productRepository.countSoldOut();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));
        if (product.getStock() < quantity) {
            throw new InvalidRequestException("Not enough stock for product " + productId);
        }
        product.setStock(product.getStock() - quantity);
        return new Reservation(productId, quantity, soldOut);
    }
}
