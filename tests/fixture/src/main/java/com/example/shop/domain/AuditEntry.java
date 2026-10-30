package com.example.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class AuditEntry {

    @Id
    @GeneratedValue
    private Long id;
    private Long orderId;
    private String action;
    private Instant recordedAt;
    private String riskScore;

    protected AuditEntry() {
    }

    public AuditEntry(Long orderId, String action, Instant recordedAt, String riskScore) {
        this.orderId = orderId;
        this.action = action;
        this.recordedAt = recordedAt;
        this.riskScore = riskScore;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getAction() { return action; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRiskScore() { return riskScore; }
}
