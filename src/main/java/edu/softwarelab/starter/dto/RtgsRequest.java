package edu.softwarelab.starter.dto;

import java.math.BigDecimal;

public record RtgsRequest(
        String transactionId,
        String sourceIban,
        String destinationIban,
        BigDecimal amount,
        String description
) {}