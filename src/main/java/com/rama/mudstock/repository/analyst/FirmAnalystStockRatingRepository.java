package com.rama.mudstock.repository.analyst;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.model.analyst.FirmAnalystStockRating;

@Repository
public interface FirmAnalystStockRatingRepository extends JpaRepository<FirmAnalystStockRating, Long> {

    java.util.Optional<FirmAnalystStockRating> findByFirmAnalystIdAndFirmIdAndStockIdAndDate(
            Long firmAnalystId, Long firmId, Long stockId, java.time.LocalDate date);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO firm_analyst_stock_rating (
                firm_analyst_id, firm_id, stock_id,
                rating_action, price_target_action, rating, previous_rating,
                price_target, previous_price_target, price_percent_change,
                adjusted_price_target, previous_adjusted_price_target,
                importance, last_updated, date,
                benzinga_calendar_url, benzinga_news_url
            ) VALUES (
                :firmAnalystId, :firmId, :stockId,
                :ratingAction, :priceTargetAction, :rating, :previousRating,
                :priceTarget, :previousPriceTarget, :pricePercentChange,
                :adjustedPriceTarget, :previousAdjustedPriceTarget,
                :importance, :lastUpdated, :date,
                :benzingaCalendarUrl, :benzingaNewsUrl
            )
            ON DUPLICATE KEY UPDATE
                rating_action                   = VALUES(rating_action),
                price_target_action             = VALUES(price_target_action),
                rating                          = VALUES(rating),
                previous_rating                 = VALUES(previous_rating),
                price_target                    = VALUES(price_target),
                previous_price_target           = VALUES(previous_price_target),
                price_percent_change            = VALUES(price_percent_change),
                adjusted_price_target           = VALUES(adjusted_price_target),
                previous_adjusted_price_target  = VALUES(previous_adjusted_price_target),
                importance                      = VALUES(importance),
                last_updated                    = VALUES(last_updated),
                benzinga_calendar_url           = VALUES(benzinga_calendar_url),
                benzinga_news_url               = VALUES(benzinga_news_url)
            """, nativeQuery = true)
    void upsert(@Param("firmAnalystId") Long firmAnalystId,
                @Param("firmId") Long firmId,
                @Param("stockId") Long stockId,
                @Param("ratingAction") String ratingAction,
                @Param("priceTargetAction") String priceTargetAction,
                @Param("rating") String rating,
                @Param("previousRating") String previousRating,
                @Param("priceTarget") java.math.BigDecimal priceTarget,
                @Param("previousPriceTarget") java.math.BigDecimal previousPriceTarget,
                @Param("pricePercentChange") java.math.BigDecimal pricePercentChange,
                @Param("adjustedPriceTarget") java.math.BigDecimal adjustedPriceTarget,
                @Param("previousAdjustedPriceTarget") java.math.BigDecimal previousAdjustedPriceTarget,
                @Param("importance") Integer importance,
                @Param("lastUpdated") java.time.LocalDate lastUpdated,
                @Param("date") java.time.LocalDate date,
                @Param("benzingaCalendarUrl") String benzingaCalendarUrl,
                @Param("benzingaNewsUrl") String benzingaNewsUrl);
}
