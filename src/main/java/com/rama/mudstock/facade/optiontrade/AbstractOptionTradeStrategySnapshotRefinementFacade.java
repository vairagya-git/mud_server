package com.rama.mudstock.facade.optiontrade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.rama.mudstock.model.option.OptionSnapshot;
import com.rama.mudstock.repository.option.OptionSnapshotFlatfileRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.repository.option.OptionStrategyRepository;
import com.rama.mudstock.repository.option.OptionTradeRepository;

public abstract class AbstractOptionTradeStrategySnapshotRefinementFacade {

    protected static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    protected final OptionTradeRepository optionTradeRepository;
    protected final OptionStrategyRepository optionStrategyRepository;
    protected final OptionSnapshotRepository optionSnapshotRepository;
    protected final OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository;

    protected AbstractOptionTradeStrategySnapshotRefinementFacade(OptionTradeRepository optionTradeRepository,
                                                                  OptionStrategyRepository optionStrategyRepository,
                                                                  OptionSnapshotRepository optionSnapshotRepository,
                                                                  OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository) {
        this.optionTradeRepository = optionTradeRepository;
        this.optionStrategyRepository = optionStrategyRepository;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.optionSnapshotFlatfileRepository = optionSnapshotFlatfileRepository;
    }

    public abstract int enrichTradeStrategySnapshot(OptionTradeRepository.TradeMode tradeMode,
                                                    Integer optionSnapshotInterval,
                                                    String lastFetchedSnapshotTime,
                                                    String lastFetchedFlatFileTime,
                                                    String lastFetchedManualEntryTime);

    protected ResolvedLegSnapshot resolveLegSnapshot(OptionStrategyRepository.StrategyLegSummaryRow leg,
                                                     Timestamp targetTime,
                                                     int intervalMinutes) {
        if (leg.optionContractId() == null || leg.quantity() == null || leg.quantity() <= 0) {
            return null;
        }

        OptionSnapshot optionSnapshot = optionSnapshotRepository.findNearestSnapshotByContractAndOptionTime(leg.optionContractId(), targetTime);
        if (!isWithinInterval(targetTime, snapshotQuoteTime(optionSnapshot), intervalMinutes)) {
            optionSnapshot = null;
        }

        OptionSnapshotFlatfileRepository.FlatFileLookupRow flatFile = optionSnapshotFlatfileRepository
            .findNearestFlatFileByContractAndTime(leg.optionContractId(), targetTime);
        if (flatFile != null && !isWithinInterval(targetTime, flatFile.localTime(), intervalMinutes)) {
            flatFile = null;
        }

        Long optionSnapshotId = optionSnapshot != null
            ? optionSnapshot.getId()
            : (flatFile != null ? flatFile.nearOptionSnapshotId() : null);

        if (optionSnapshotId == null) {
            return null;
        }

        BigDecimal marketPrice = resolveMarketPrice(optionSnapshot, flatFile);
        if (marketPrice == null) {
            return null;
        }

        Long optionFlatFileId = flatFile == null ? null : flatFile.id();
        return new ResolvedLegSnapshot(leg, optionSnapshotId, optionFlatFileId, marketPrice, optionSnapshot);
    }

    protected BigDecimal resolveMarketPrice(OptionSnapshot optionSnapshot,
                                            OptionSnapshotFlatfileRepository.FlatFileLookupRow flatFile) {
        if (optionSnapshot != null) {
            if (optionSnapshot.getMidpoint() != null) {
                return optionSnapshot.getMidpoint();
            }
            if (optionSnapshot.getLastTradePrice() != null) {
                return optionSnapshot.getLastTradePrice();
            }
            if (optionSnapshot.getBid() != null && optionSnapshot.getAsk() != null) {
                return optionSnapshot.getBid().add(optionSnapshot.getAsk()).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            }
        }

        if (flatFile != null && flatFile.optClose() != null) {
            return flatFile.optClose();
        }

        return null;
    }

    protected Timestamp snapshotQuoteTime(OptionSnapshot optionSnapshot) {
        if (optionSnapshot == null) {
            return null;
        }
        return optionSnapshot.getOptionQuoteTime() != null ? optionSnapshot.getOptionQuoteTime() : optionSnapshot.getSnapshotTime();
    }

    protected boolean isWithinInterval(Timestamp targetTime, Timestamp sourceTime, int intervalMinutes) {
        if (targetTime == null || sourceTime == null) {
            return false;
        }
        long minutes = Math.abs(Duration.between(targetTime.toInstant(), sourceTime.toInstant()).toMinutes());
        return minutes <= intervalMinutes;
    }

    protected Timestamp alignToIntervalBoundary(int intervalMinutes) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        int minute = now.getMinute();
        int floorMinute = minute - (minute % intervalMinutes);
        LocalDateTime aligned = now.withMinute(floorMinute);
        return Timestamp.valueOf(aligned);
    }

    protected int requireInterval(Integer optionSnapshotInterval) {
        if (optionSnapshotInterval == null || optionSnapshotInterval <= 0) {
            throw new IllegalArgumentException("optionSnapshotInterval must be > 0");
        }
        return optionSnapshotInterval;
    }

    protected BigDecimal averageNonNull(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        BigDecimal sum = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 6, RoundingMode.HALF_UP);
    }

    protected BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    protected record ResolvedLegSnapshot(OptionStrategyRepository.StrategyLegSummaryRow leg,
                                         Long optionSnapshotId,
                                         Long optionFlatFileId,
                                         BigDecimal marketPrice,
                                         OptionSnapshot optionSnapshot) {
    }
}
