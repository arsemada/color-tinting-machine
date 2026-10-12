package com.colortinting.persistence;

import com.colortinting.domain.ModelEnums.BatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<BatchEntity, UUID>, JpaSpecificationExecutor<BatchEntity> {
    Optional<BatchEntity> findFirstByMachineIdAndStateIn(String machineId, Collection<BatchState> states);

    List<BatchEntity> findByMachineIdAndStateIn(String machineId, Collection<BatchState> states);
}
