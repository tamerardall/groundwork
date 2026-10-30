package com.example.shop.client;

final class SimulatedLatency {

    private SimulatedLatency() {
    }

    static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling remote service", e);
        }
    }
}
