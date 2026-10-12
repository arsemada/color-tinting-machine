package com.colortinting.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OperatorSettingsRepository extends JpaRepository<OperatorSettingsEntity, Short> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OperatorSettingsEntity s where s.id = 1")
    OperatorSettingsEntity lockSingleton();
}
