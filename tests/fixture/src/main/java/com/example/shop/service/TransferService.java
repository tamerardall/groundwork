package com.example.shop.service;

import com.example.shop.domain.Account;
import com.example.shop.exception.InvalidRequestException;
import com.example.shop.exception.NotFoundException;
import com.example.shop.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        List<Account> accounts = accountRepository.findAllById(List.of(fromId, toId));
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRequestException("Amount must be positive");
        }
        Account source = pick(accounts, fromId);
        Account target = pick(accounts, toId);
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InvalidRequestException("Insufficient funds in account " + fromId);
        }
        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));
    }

    private static Account pick(List<Account> accounts, Long id) {
        return accounts.stream()
                .filter(account -> account.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Account", id));
    }
}
