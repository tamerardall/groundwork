package com.example.shop.client;

import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class InventoryClient {

    public int available(Long productId) {
        SimulatedLatency.pause(250);
        return 42;
    }
}
