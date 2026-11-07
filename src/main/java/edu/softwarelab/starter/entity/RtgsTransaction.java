package edu.softwarelab.starter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "rtgs_transactions")
public class RtgsTransaction {

    @Id
    private String id;

    private String sourceIban;
    private String destinationIban;
    private BigDecimal amount;
    private String description;
    private RtgsStatus status;
    private String failureReason;

    // سازنده پیش‌فرض برای JPA (الزامی)
    public RtgsTransaction() {}

    public RtgsTransaction(String id, String sourceIban, String destinationIban, BigDecimal amount, String description) {
        this.id = id;
        this.sourceIban = sourceIban;
        this.destinationIban = destinationIban;
        this.amount = amount;
        this.description = description;
        this.status = RtgsStatus.PENDING;
    }

    public String getId() { return id; }
    public String getSourceIban() { return sourceIban; }
    public String getDestinationIban() { return destinationIban; }
    public BigDecimal getAmount() { return amount; }
    public RtgsStatus getStatus() { return status; }
    public void setStatus(RtgsStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}