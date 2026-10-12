package com.colortinting.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MachineEventRepository extends JpaRepository<MachineEventEntity, UUID> {
    List<MachineEventEntity> findTop50ByOrderByOccurredAtDesc();
}
