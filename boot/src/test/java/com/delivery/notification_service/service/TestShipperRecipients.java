package com.delivery.notification_service.service;

import com.delivery.notification_service.entity.ShipperIdentityProjection;
import com.delivery.notification_service.repository.ShipperIdentityProjectionRepository;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shipper profile 5 belongs to account (legacy 51, principal 52) in listener fixtures. */
public final class TestShipperRecipients {
    public static final long SHIPPER_ID = 5L;
    public static final long USER_ID = 51L;
    public static final long PRINCIPAL_ID = 52L;

    private TestShipperRecipients() {}

    public static ShipperRecipientResolver shipperFive() {
        ShipperIdentityProjectionRepository projections = mock(ShipperIdentityProjectionRepository.class);
        when(projections.findByShipperId(anyLong())).thenReturn(Optional.empty());
        ShipperIdentityProjection mapping = new ShipperIdentityProjection();
        mapping.setShipperId(SHIPPER_ID);
        mapping.setLegacyUserId(USER_ID);
        mapping.setPrincipalId(PRINCIPAL_ID);
        mapping.setMappingVersion(1L);
        when(projections.findByShipperId(SHIPPER_ID)).thenReturn(Optional.of(mapping));
        return new ShipperRecipientResolver(projections);
    }
}
