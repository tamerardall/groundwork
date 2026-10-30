package com.example.shop.client;

import com.example.shop.exception.PaymentDeclinedException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class PaymentGateway {

    public void charge(Long orderId, BigDecimal amount) {
        SimulatedLatency.pause(400);
        if (amount.signum() <= 0) {
            throw new PaymentDeclinedException(orderId);
        }
    }
}
