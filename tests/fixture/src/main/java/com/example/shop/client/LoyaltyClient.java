package com.example.shop.client;

import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class LoyaltyClient {

    public int points(Long customerId) {
        SimulatedLatency.pause(250);
        return 120;
    }
}
