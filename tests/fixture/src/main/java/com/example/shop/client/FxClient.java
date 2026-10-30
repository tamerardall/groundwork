package com.example.shop.client;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class FxClient {

    public BigDecimal rate(String currency) {
        SimulatedLatency.pause(300);
        return new BigDecimal("0.92");
    }
}
