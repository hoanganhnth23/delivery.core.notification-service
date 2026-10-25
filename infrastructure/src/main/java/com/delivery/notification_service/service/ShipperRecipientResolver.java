package com.delivery.notification_service.service;

import com.delivery.notification_service.entity.ShipperIdentityProjection;
import com.delivery.notification_service.repository.ShipperIdentityProjectionRepository;
import org.springframework.stereotype.Component;

/**
 * Maps a shipper profile id from Delivery/Match events to the shipper's account inbox.
 * A missing mapping is retryable (the projection is eventually consistent); it must
 * never fall back to the shipper id, which is a different id space.
 */
@Component
public class ShipperRecipientResolver {
    private final ShipperIdentityProjectionRepository projections;

    public ShipperRecipientResolver(ShipperIdentityProjectionRepository projections) {
        this.projections = projections;
    }

    public Recipient require(Long shipperId) {
        ShipperIdentityProjection mapping = projections.findByShipperId(shipperId)
                .orElseThrow(() -> new ShipperIdentityNotReadyException(shipperId));
        return new Recipient(mapping.getLegacyUserId(), mapping.getPrincipalId());
    }

    public record Recipient(Long legacyUserId, Long principalId) {}

    /** Retryable: the listener's retry topics redeliver until the projection arrives. */
    public static class ShipperIdentityNotReadyException extends IllegalStateException {
        public ShipperIdentityNotReadyException(Long shipperId) {
            super("Shipper identity projection is not ready for shipper " + shipperId);
        }
    }
}
