package com.example.shop.client;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class PricingClient {

    public BigDecimal quote(Long productId) {
        SimulatedLatency.pause(250);
        return new BigDecimal("19.90");
    }
}
