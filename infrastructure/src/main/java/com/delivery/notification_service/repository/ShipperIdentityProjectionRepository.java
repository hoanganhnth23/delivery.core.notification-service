package com.delivery.notification_service.repository;

import com.delivery.notification_service.entity.ShipperIdentityProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipperIdentityProjectionRepository extends JpaRepository<ShipperIdentityProjection, Long> {
    Optional<ShipperIdentityProjection> findByShipperId(Long shipperId);
}
