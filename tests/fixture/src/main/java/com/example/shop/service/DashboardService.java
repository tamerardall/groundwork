package com.example.shop.service;

import com.example.shop.client.InventoryClient;
import com.example.shop.client.PricingClient;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final PricingClient pricingClient;
    private final InventoryClient inventoryClient;

    public DashboardService(PricingClient pricingClient, InventoryClient inventoryClient) {
        this.pricingClient = pricingClient;
        this.inventoryClient = inventoryClient;
    }

    public ProductSummary summary(Long productId) {
        BigDecimal price = pricingClient.quote(productId);
        int available = inventoryClient.available(productId);
        return new ProductSummary(productId, price, available);
    }
}
