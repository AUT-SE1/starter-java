package edu.softwarelab.starter.service;

import edu.softwarelab.starter.dto.ExecuteExternalTransfer;
import edu.softwarelab.starter.entity.SettlementStatus;
import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.exception.ValidationException;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ثبت پایدار تراکنش ورودی (وضعیت ACCEPTED). پردازش بعد از commit شروع می‌شود.
 * مصرف از Kafka در استوری جدا به همین متد وصل می‌شود.
 */
@Service
public class TransferIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TransferIngestionService.class);

    private final SettlementTransferRepository repository;
    private final ApplicationEventPublisher publisher;
    private final String defaultSettlementType;

    public TransferIngestionService(
            SettlementTransferRepository repository,
            ApplicationEventPublisher publisher,
            @Value("${settlement.default-type:RTGS}") String defaultSettlementType) {
        this.repository = repository;
        this.publisher = publisher;
        this.defaultSettlementType = defaultSettlementType;
    }

    /**
     * @return شناسه‌ی تراکنش (همان transaction_id پیام). تکراری‌ها دوباره ثبت نمی‌شوند.
     */
    @Transactional
    public UUID accept(ExecuteExternalTransfer command) {
        validate(command);

        UUID transactionId = command.getTransactionId();
        String idempotencyKey = command.getOperationId() != null
                ? command.getOperationId().toString()
                : transactionId.toString();

        if (repository.existsById(transactionId) || repository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("Duplicate transfer command ignored: transaction_id={}", transactionId);
            return transactionId;
        }

        SettlementTransfer transfer = new SettlementTransfer();
        transfer.setId(transactionId);
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setCorrelationId(command.getCorrelationId() != null ? command.getCorrelationId() : transactionId.toString());
        transfer.setSourceIban(null);
        transfer.setDestinationIban(command.getData().getDestIban());
        transfer.setAmount(command.getData().getAmount());
        transfer.setCurrency(command.getData().getCurrency() != null ? command.getData().getCurrency() : "IRR");
        transfer.setSettlementType(defaultSettlementType);
        transfer.setStatus(SettlementStatus.ACCEPTED);
        repository.save(transfer);

        publisher.publishEvent(new TransferAcceptedEvent(transactionId, defaultSettlementType));
        return transactionId;
    }

    private static void validate(ExecuteExternalTransfer c) {
        if (c == null) {
            throw new ValidationException("body", "Command is required");
        }
        if (c.getTransactionId() == null) {
            throw new ValidationException("transaction_id", "transaction_id is required");
        }
        if (c.getData() == null) {
            throw new ValidationException("data", "data is required");
        }
        if (c.getData().getDestIban() == null || c.getData().getDestIban().isBlank()) {
            throw new ValidationException("data.dest_iban", "dest_iban is required");
        }
        if (c.getData().getDestIban().length() > 26) {
            throw new ValidationException("data.dest_iban", "dest_iban must be at most 26 characters");
        }
        if (c.getData().getAmount() == null || c.getData().getAmount() <= 0) {
            throw new ValidationException("data.amount", "amount must be a positive integer");
        }
        if (c.getData().getCurrency() != null && !"IRR".equals(c.getData().getCurrency())) {
            throw new ValidationException("data.currency", "only IRR is supported");
        }
    }
}