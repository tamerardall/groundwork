package com.example.shop.service;

import com.example.shop.domain.Product;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal total(List<Long> productIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : productIds) {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Product", id));
            total = total.add(product.getPrice());
        }
        return total;
    }
}
