package com.example.shop.client;

import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class RiskClient {

    public String score(Long orderId) {
        SimulatedLatency.pause(500);
        return "LOW";
    }
}
