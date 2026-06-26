package com.rama.mudstock.repository.analyst;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.model.analyst.Firm;

@Repository
public interface FirmRepository extends JpaRepository<Firm, Long> {

    Optional<Firm> findByBenzingaFirmId(String benzingaFirmId);

    boolean existsByBenzingaFirmId(String benzingaFirmId);

    /**
     * Upsert a firm row: insert if the (benzinga_firm_id, name) pair does not exist,
     * otherwise update currency and last_updated.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO firm (benzinga_firm_id, name, currency, last_updated)
            VALUES (:benzingaFirmId, :name, :currency, :lastUpdated)
            ON DUPLICATE KEY UPDATE
                currency     = VALUES(currency),
                last_updated = VALUES(last_updated)
            """, nativeQuery = true)
    void upsert(@Param("benzingaFirmId") String benzingaFirmId,
                @Param("name") String name,
                @Param("currency") String currency,
                @Param("lastUpdated") java.time.LocalDate lastUpdated);
}