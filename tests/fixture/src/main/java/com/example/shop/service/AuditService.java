package com.example.shop.service;

import com.example.shop.client.RiskClient;
import com.example.shop.domain.AuditEntry;
import com.example.shop.repository.AuditEntryRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;
    private final RiskClient riskClient;

    public AuditService(AuditEntryRepository auditEntryRepository, RiskClient riskClient) {
        this.auditEntryRepository = auditEntryRepository;
        this.riskClient = riskClient;
    }

    @Transactional
    public AuditEntry record(Long orderId, String action) {
        Instant recordedAt = Instant.now();
        String risk = riskClient.score(orderId);
        return auditEntryRepository.save(new AuditEntry(orderId, action, recordedAt, risk));
    }
}
