package com.example.shop.client;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Stands in for a remote HTTP service. */
@Component
public class AuthHeaderClient {

    /** Fetches the current user's display name from the profile service. */
    public String displayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authenticated user on this thread");
        }
        SimulatedLatency.pause(250);
        return auth.getName();
    }
}
