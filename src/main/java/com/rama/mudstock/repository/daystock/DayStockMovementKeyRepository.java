package com.rama.mudstock.repository.daystock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.daystock.DayStockMovementKey;

@Repository
public interface DayStockMovementKeyRepository extends JpaRepository<DayStockMovementKey, Long> {
    java.util.Optional<DayStockMovementKey> findByCode(String code);
}
