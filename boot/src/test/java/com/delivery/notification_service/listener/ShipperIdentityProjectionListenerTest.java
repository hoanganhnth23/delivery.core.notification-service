package com.delivery.notification_service.listener;

import com.delivery.identity.contracts.ShipperIdentityUpserted;
import com.delivery.notification_service.entity.ShipperIdentityInboxReceipt;
import com.delivery.notification_service.entity.ShipperIdentityProjection;
import com.delivery.notification_service.repository.ShipperIdentityInboxReceiptRepository;
import com.delivery.notification_service.repository.ShipperIdentityProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipperIdentityProjectionListenerTest {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final ShipperIdentityProjectionRepository projections = mock(ShipperIdentityProjectionRepository.class);
    final ShipperIdentityInboxReceiptRepository receipts = mock(ShipperIdentityInboxReceiptRepository.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final Acknowledgment ack = mock(Acknowledgment.class);
    final ShipperIdentityProjectionListener listener =
            new ShipperIdentityProjectionListener(mapper, projections, receipts, transactions);
    final UUID eventId = UUID.randomUUID();

    String event(long version) throws Exception {
        return mapper.writeValueAsString(new ShipperIdentityUpserted(eventId, ShipperIdentityUpserted.TYPE, 1,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, 10L, 100L, 7L, version));
    }

    ShipperIdentityProjection existing(long version) {
        var p = new ShipperIdentityProjection(); p.setPrincipalId(10L); p.setLegacyUserId(100L);
        p.setShipperId(5L); p.setMappingVersion(version);
        when(projections.findById(10L)).thenReturn(Optional.of(p)); return p;
    }

    @Test void firstMappingIsPersistedAcknowledgedAfterCommitAndExactReplayIsNoOp() throws Exception {
        String raw = event(1); listener.upsert(raw, ack);
        var projection = ArgumentCaptor.forClass(ShipperIdentityProjection.class);
        var receipt = ArgumentCaptor.forClass(ShipperIdentityInboxReceipt.class);
        var order = inOrder(projections, receipts, transactions, ack);
        order.verify(projections).save(projection.capture()); order.verify(receipts).save(receipt.capture());
        order.verify(transactions).commit(any()); order.verify(ack).acknowledge();
        assertThat(projection.getValue().getShipperId()).isEqualTo(7L);
        assertThat(projection.getValue().getLegacyUserId()).isEqualTo(100L);
        assertThat(projection.getValue().getPrincipalId()).isEqualTo(10L);
        assertThat(receipt.getValue().getPayloadFingerprint()).hasSize(64);
        when(receipts.findById(eventId)).thenReturn(Optional.of(receipt.getValue()));
        clearInvocations(projections, receipts); listener.upsert(raw, ack);
        verifyNoInteractions(projections); verify(receipts, never()).save(any());
        assertThatThrownBy(() -> listener.upsert(event(2), ack)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Conflicting shipper identity event reuse");
    }

    @Test void oldVersionRecordsReceiptWithoutRegressingMapping() throws Exception {
        var p = existing(3); listener.upsert(event(2), ack);
        assertThat(p.getShipperId()).isEqualTo(5L); assertThat(p.getMappingVersion()).isEqualTo(3L);
        verify(projections, never()).save(any()); verify(receipts).save(any()); verify(ack).acknowledge();
    }

    @Test void versionGapFailsWithoutMutationOrAcknowledgement() throws Exception {
        existing(1);
        assertThatThrownBy(() -> listener.upsert(event(3), ack)).hasMessage("Shipper identity mapping version gap");
        verify(projections, never()).save(any()); verify(receipts, never()).save(any());
        verify(transactions).rollback(any()); verify(ack, never()).acknowledge();
    }

    @Test void invalidVersionAndTypeArePoisonBeforePersistence() throws Exception {
        assertThatThrownBy(() -> listener.upsert(event(0), ack)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.upsert(event(1).replace(ShipperIdentityUpserted.TYPE, "wrong"), ack))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(projections, receipts, ack);
    }
}
