package com.example.shop.service;

import com.example.shop.client.AuthHeaderClient;
import com.example.shop.client.LoyaltyClient;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final AuthHeaderClient authHeaderClient;
    private final LoyaltyClient loyaltyClient;

    public ProfileService(AuthHeaderClient authHeaderClient, LoyaltyClient loyaltyClient) {
        this.authHeaderClient = authHeaderClient;
        this.loyaltyClient = loyaltyClient;
    }

    public ProfileView view(Long customerId) {
        String displayName = authHeaderClient.displayName();
        int points = loyaltyClient.points(customerId);
        return new ProfileView(displayName, points);
    }
}
