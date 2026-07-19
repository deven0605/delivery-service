package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {
    Optional<DeliveryPartner> findByPhone(String phone);
}
