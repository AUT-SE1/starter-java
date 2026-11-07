package edu.softwarelab.starter;

import edu.softwarelab.starter.entity.SettlementTransfer;
import edu.softwarelab.starter.repository.SettlementTransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementTransferRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private SettlementTransferRepository repository;

    @Test
    void shouldSaveAndLoadTransfer() {
        SettlementTransfer transfer = new SettlementTransfer();
        transfer.setIdempotencyKey("idem-" + UUID.randomUUID());
        transfer.setCorrelationId(UUID.randomUUID().toString());
        transfer.setSourceIban("IR000699000000000000000001");
        transfer.setDestinationIban("IR000120000000000000000002");
        transfer.setAmount(250000000L);
        transfer.setSettlementType("SATNA");
        transfer.setStatus("ACCEPTED");

        SettlementTransfer saved = repository.save(transfer);
        assertThat(saved.getId()).isNotNull();

        SettlementTransfer found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getAmount()).isEqualTo(250000000L);
        assertThat(found.getAttemptCount()).isZero();
    }

    @Test
    void shouldAllowRtgsTransferWithoutSourceIban() {
        SettlementTransfer transfer = new SettlementTransfer();
        transfer.setIdempotencyKey("idem-" + UUID.randomUUID());
        transfer.setCorrelationId("corr");
        transfer.setDestinationIban("IR000120000000000000000002");
        transfer.setAmount(1000L);
        transfer.setSettlementType("RTGS");
        transfer.setStatus("ACCEPTED");

        SettlementTransfer saved = repository.save(transfer);

        assertThat(repository.findById(saved.getId()).orElseThrow().getSourceIban()).isNull();
    }
}
