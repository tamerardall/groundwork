package com.example.shop.service;

import com.example.shop.domain.Customer;
import com.example.shop.exception.InvalidRequestException;
import com.example.shop.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final CustomerRepository customerRepository;

    public RegistrationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer register(String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new InvalidRequestException("Email already registered");
        }
        if (email == null || !email.contains("@")) {
            throw new InvalidRequestException("Invalid email address");
        }
        return customerRepository.save(new Customer(email, "STANDARD"));
    }
}
