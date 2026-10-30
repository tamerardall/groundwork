package com.example.shop.service;

import com.example.shop.client.FxClient;
import com.example.shop.domain.Product;
import com.example.shop.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository productRepository;
    private final FxClient fxClient;

    public CatalogService(ProductRepository productRepository, FxClient fxClient) {
        this.productRepository = productRepository;
        this.fxClient = fxClient;
    }

    public List<CatalogItem> list() {
        List<Product> products = productRepository.findAll();
        BigDecimal rate = fxClient.rate("EUR");
        return products.stream()
                .map(product -> new CatalogItem(product.getId(), product.getPrice().multiply(rate)))
                .toList();
    }
}
