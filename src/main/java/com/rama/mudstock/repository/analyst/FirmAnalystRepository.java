package com.rama.mudstock.repository.analyst;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.model.analyst.FirmAnalyst;

@Repository
public interface FirmAnalystRepository extends JpaRepository<FirmAnalyst, Long> {

    java.util.Optional<FirmAnalyst> findByBenzingaAnalystId(String benzingaAnalystId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO firm_analyst (
                firm_id, benzinga_analyst_id, benzinga_firm_id, full_name, last_updated,
                overall_avg_return, overall_avg_return_percentile, overall_success_rate,
                smart_score, total_ratings, total_ratings_percentile
            ) VALUES (
                :firmId, :benzingaAnalystId, :benzingaFirmId, :fullName, :lastUpdated,
                :overallAvgReturn, :overallAvgReturnPercentile, :overallSuccessRate,
                :smartScore, :totalRatings, :totalRatingsPercentile
            )
            ON DUPLICATE KEY UPDATE
                firm_id                       = VALUES(firm_id),
                full_name                     = VALUES(full_name),
                last_updated                  = VALUES(last_updated),
                overall_avg_return            = VALUES(overall_avg_return),
                overall_avg_return_percentile = VALUES(overall_avg_return_percentile),
                overall_success_rate          = VALUES(overall_success_rate),
                smart_score                   = VALUES(smart_score),
                total_ratings                 = VALUES(total_ratings),
                total_ratings_percentile      = VALUES(total_ratings_percentile)
            """, nativeQuery = true)
    void upsert(@Param("firmId") Long firmId,
                @Param("benzingaAnalystId") String benzingaAnalystId,
                @Param("benzingaFirmId") String benzingaFirmId,
                @Param("fullName") String fullName,
                @Param("lastUpdated") java.time.LocalDate lastUpdated,
                @Param("overallAvgReturn") java.math.BigDecimal overallAvgReturn,
                @Param("overallAvgReturnPercentile") java.math.BigDecimal overallAvgReturnPercentile,
                @Param("overallSuccessRate") java.math.BigDecimal overallSuccessRate,
                @Param("smartScore") java.math.BigDecimal smartScore,
                @Param("totalRatings") java.math.BigDecimal totalRatings,
                @Param("totalRatingsPercentile") java.math.BigDecimal totalRatingsPercentile);
}
