package com.example.shop.client;

import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class MailClient {

    public void send(String to, String body) {
        SimulatedLatency.pause(300);
    }
}
