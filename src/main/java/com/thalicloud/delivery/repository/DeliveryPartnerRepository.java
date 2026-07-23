package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.enums.DutyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {
    Optional<DeliveryPartner> findByPhone(String phone);

    // Candidate pool for dispatchOrder — no geo/zone matching engine exists yet,
    // so the service picks the first of these with no active assignment.
    List<DeliveryPartner> findByDutyStatus(DutyStatus dutyStatus);
}
