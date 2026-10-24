package com.delivery.notification_service.repository;

import com.delivery.notification_service.entity.ShipperIdentityInboxReceipt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipperIdentityInboxReceiptRepository extends JpaRepository<ShipperIdentityInboxReceipt, UUID> {}
