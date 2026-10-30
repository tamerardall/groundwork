package com.example.shop.exception;

public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(Long orderId) {
        super("Payment declined for order " + orderId);
    }
}
