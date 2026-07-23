package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.CashRemittance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CashRemittanceRepository extends JpaRepository<CashRemittance, UUID> {
}
