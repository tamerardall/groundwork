package com.example.shop.service;

import com.example.shop.client.MailClient;
import com.example.shop.domain.Customer;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WelcomeService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeService.class);

    private final CustomerRepository customerRepository;
    private final MailClient mailClient;

    public WelcomeService(CustomerRepository customerRepository, MailClient mailClient) {
        this.customerRepository = customerRepository;
        this.mailClient = mailClient;
    }

    @Transactional
    public void activate(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer", customerId));
        customer.setActive(true);
        try {
            mailClient.send(customer.getEmail(), "Welcome aboard!");
        } catch (RuntimeException e) {
            log.warn("Welcome mail to customer {} failed", customerId, e);
        }
    }
}
