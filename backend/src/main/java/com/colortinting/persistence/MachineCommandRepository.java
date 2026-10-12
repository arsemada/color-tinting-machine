package com.colortinting.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MachineCommandRepository extends JpaRepository<MachineCommandEntity, UUID> {
    Optional<MachineCommandEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByReconciliationRequiredTrueAndReconciliationResolvedFalse();

    List<MachineCommandEntity> findByReconciliationRequiredTrueAndReconciliationResolvedFalse();
}
