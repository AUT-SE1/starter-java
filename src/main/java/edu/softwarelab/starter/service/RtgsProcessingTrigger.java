package edu.softwarelab.starter.service;

import edu.softwarelab.starter.entity.SettlementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** پردازش RTGS را فقط بعد از commit موفق ثبت تراکنش شروع می‌کند. */
@Component
public class RtgsProcessingTrigger {

    private static final Logger log = LoggerFactory.getLogger(RtgsProcessingTrigger.class);

    private final SettlementProcessingService processingService;

    public RtgsProcessingTrigger(SettlementProcessingService processingService) {
        this.processingService = processingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferAccepted(TransferAcceptedEvent event) {
        if (!SettlementType.RTGS.equals(event.settlementType())) {
            return; // PAYA و بقیه در استوری‌های خودشان پردازش می‌شوند
        }
        try {
            processingService.process(event.transferId());
        } catch (RuntimeException ex) {
            // ثبت تراکنش قبلاً commit شده؛ پردازش با retryPending قابل جبران است.
            log.error("RTGS processing failed for transfer {}", event.transferId(), ex);
        }
    }
}
