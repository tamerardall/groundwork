package com.example.shop.service;

import com.example.shop.domain.Customer;
import com.example.shop.repository.CustomerRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class DiscountService {

    private static final BigDecimal GOLD_RATE = new BigDecimal("0.10");
    private static final BigDecimal STANDARD_RATE = new BigDecimal("0.02");

    private final CustomerRepository customerRepository;

    public DiscountService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public BigDecimal discountFor(Long customerId, BigDecimal amount) {
        String tier = customerRepository.findById(customerId)
                .map(Customer::getTier)
                .orElse("STANDARD");
        if (amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = "GOLD".equals(tier) ? GOLD_RATE : STANDARD_RATE;
        return amount.multiply(rate);
    }
}
