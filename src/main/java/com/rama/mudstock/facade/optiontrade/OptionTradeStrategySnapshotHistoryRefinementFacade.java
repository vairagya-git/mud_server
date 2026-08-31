package com.rama.mudstock.facade.optiontrade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.option.OptionSnapshot;
import com.rama.mudstock.repository.option.OptionSnapshotFlatfileRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.repository.option.OptionStrategyRepository;
import com.rama.mudstock.repository.option.OptionTradeRepository;
import com.rama.mudstock.util.AppUtil;

@Service
public class OptionTradeStrategySnapshotHistoryRefinementFacade extends AbstractOptionTradeStrategySnapshotRefinementFacade {

    private final Logger log = LoggerFactory.getLogger(OptionTradeStrategySnapshotHistoryRefinementFacade.class);

    public OptionTradeStrategySnapshotHistoryRefinementFacade(OptionTradeRepository optionTradeRepository,
                                                              OptionStrategyRepository optionStrategyRepository,
                                                              OptionSnapshotRepository optionSnapshotRepository,
                                                              OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository) {
        super(optionTradeRepository, optionStrategyRepository, optionSnapshotRepository, optionSnapshotFlatfileRepository);
    }

    @Override
    public int enrichTradeStrategySnapshot(OptionTradeRepository.TradeMode tradeMode,
                                           Integer optionSnapshotInterval,
                                           String lastFetchedSnapshotTime,
                                           String lastFetchedFlatFileTime,
                                           String lastFetchedManualEntryTime) {
        if (tradeMode == null) {
            throw new IllegalArgumentException("tradeMode is required");
        }

        int intervalMinutes = requireInterval(optionSnapshotInterval);

        List<Long> openTradeIds = optionTradeRepository
            .listOpenTradesByModeWithTicker(OptionTradeRepository.Status.OPEN)
            .stream().map(OptionTradeRepository.TradeRow::id).toList();

        if (openTradeIds.isEmpty()) {
            log.info("enrichTradeStrategySnapshot: no OPEN trades found for tradeMode={}", tradeMode);
            return 0;
        }

        if (tradeMode == OptionTradeRepository.TradeMode.HISTORY) {
            log.info("enrichTradeStrategySnapshot: OPEN HISTORY trades count={}", openTradeIds.size());
        }

        Timestamp snapshotReferenceTime = alignToIntervalBoundary(intervalMinutes);
        long intervalSeconds = intervalMinutes * 60L;

        List<OptionStrategyRepository.StrategyLegSummaryRow> strategyLegRows = optionStrategyRepository
            .listOpenStrategyLegsByTradeIds(openTradeIds, OptionStrategyRepository.Status.OPEN);
        if (strategyLegRows.isEmpty()) {
            log.info("enrichTradeStrategySnapshot: no OPEN option_strategy rows found for tradeMode={}", tradeMode);
            return 0;
        }

        Map<Long, List<OptionStrategyRepository.StrategyLegSummaryRow>> legsByStrategyId = strategyLegRows
            .stream()
            .collect(Collectors.groupingBy(OptionStrategyRepository.StrategyLegSummaryRow::optionStrategyId));

        List<Long> strategyIds = legsByStrategyId.keySet().stream().filter(Objects::nonNull).toList();
        if (strategyIds.isEmpty()) {
            log.info("enrichTradeStrategySnapshot: no OPEN option_strategy rows found for tradeMode={}", tradeMode);
            return 0;
        }

        int strategySnapshotsInserted = 0;
        int legSnapshotsInserted = 0;
        int strategiesSkipped = 0;

        for (Long strategyId : strategyIds) {
            if (strategyId == null) {
                continue;
            }

            List<OptionStrategyRepository.StrategyLegSummaryRow> legs = legsByStrategyId.getOrDefault(strategyId, List.of());
            if (legs.isEmpty()) {
                strategiesSkipped++;
                continue;
            }

            List<ResolvedLegSnapshot> resolvedLegs = resolveLegSnapshotsForStrategy(strategyId, legs, intervalMinutes, intervalSeconds);

            if (resolvedLegs.size() != legs.size()) {
                String allContracts = legs.stream()
                    .map(leg -> Objects.toString(leg.contractTicker(), "") + "(" + leg.id() + ")")
                    .collect(Collectors.joining(", "));
                String pickedContracts = resolvedLegs.stream()
                    .map(resolved -> Objects.toString(resolved.leg().contractTicker(), "") + "(" + resolved.leg().id() + ")")
                    .collect(Collectors.joining(", "));
                log.info("enrichTradeStrategySnapshot: strategy skipped strategyId={}, reason=not-all-legs-resolved, totalLegs={}, pickedLegs={}, contracts=[{}], picked=[{}]",
                    strategyId,
                    legs.size(),
                    resolvedLegs.size(),
                    allContracts,
                    pickedContracts);
                strategiesSkipped++;
                continue;
            }

            StrategySnapshotMetrics metrics = calculateStrategySnapshotMetrics(resolvedLegs);
            if (metrics == null) {
                strategiesSkipped++;
                continue;
            }

            Long optionStrategySnapshotId = optionStrategyRepository.insertStrategySnapshotAndReturnId(
                strategyId,
                snapshotReferenceTime,
                metrics.underlyingPrice(),
                metrics.entryCost(),
                metrics.currentMarketValue(),
                metrics.unrealizedPnl(),
                metrics.unrealizedPnlPct(),
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                metrics.unrealizedPnl(),
                metrics.netDelta(),
                metrics.netGamma(),
                metrics.netTheta(),
                metrics.netVega(),
                metrics.averageIv(),
                metrics.totalOpenInterest(),
                metrics.totalDayVolume());

            if (optionStrategySnapshotId == null) {
                strategiesSkipped++;
                continue;
            }

            strategySnapshotsInserted++;
            legSnapshotsInserted += insertLegSnapshots(optionStrategySnapshotId, resolvedLegs);
        }

        log.info("enrichTradeStrategySnapshot: tradeMode={}, optionSnapshotInterval={}, lastFetchedSnapshotTime={}, lastFetchedFlatFileTime={}, lastFetchedManualEntryTime={}, strategySnapshotInserted={}, strategyLegSnapshotInserted={}, strategySkipped={}, snapshotTime={}",
            tradeMode,
            intervalMinutes,
            lastFetchedSnapshotTime,
            lastFetchedFlatFileTime,
            lastFetchedManualEntryTime,
            strategySnapshotsInserted,
            legSnapshotsInserted,
            strategiesSkipped,
            snapshotReferenceTime);
        return strategySnapshotsInserted;
    }

    // Resolves the next eligible snapshot for each leg using unix-time interval logic.
    private List<ResolvedLegSnapshot> resolveLegSnapshotsForStrategy(Long strategyId,
                                                                     List<OptionStrategyRepository.StrategyLegSummaryRow> legs,
                                                                     int intervalMinutes,
                                                                     long intervalSeconds) {
        List<ResolvedLegSnapshot> resolvedLegs = new ArrayList<>();
        for (OptionStrategyRepository.StrategyLegSummaryRow leg : legs) {
            if (leg.lastSnapshotUnixTime() == null || leg.optionContractId() == null || leg.quantity() == null || leg.quantity() <= 0) {
                log.info("enrichTradeStrategySnapshot: leg skipped strategyId={}, legId={}, contractTicker={}, contractId={}, reason=missing-leg-anchor-or-invalid-quantity, lastSnapshotUnixTime={}, quantity={}",
                    strategyId,
                    leg.id(),
                    leg.contractTicker(),
                    leg.optionContractId(),
                    leg.lastSnapshotUnixTime(),
                    leg.quantity());
                resolvedLegs.clear();
                break;
            }

            long targetUnixTime = AppUtil.addMinutesToEpoch(leg.lastSnapshotUnixTime(), intervalMinutes);
            OptionSnapshot optionSnapshot = optionSnapshotRepository.findNearestSnapshotByContractAndUnixTime(
                leg.optionContractId(),
                targetUnixTime,
                leg.lastSnapshotUnixTime());

            if (optionSnapshot == null || optionSnapshot.getUnixTime() == null) {
                log.info("enrichTradeStrategySnapshot: leg skipped strategyId={}, legId={}, contractTicker={}, contractId={}, reason=no-nearest-snapshot, targetUnixTime={}, lastSnapshotUnixTime={}",
                    strategyId,
                    leg.id(),
                    leg.contractTicker(),
                    leg.optionContractId(),
                    targetUnixTime,
                    leg.lastSnapshotUnixTime());
                resolvedLegs.clear();
                break;
            }

            long unixDistance = Math.abs(optionSnapshot.getUnixTime() - targetUnixTime);
            long maxDistanceInEpochUnit = AppUtil.secondsToEpochUnit(intervalSeconds, optionSnapshot.getUnixTime());
            if (unixDistance > maxDistanceInEpochUnit) {
                log.info("enrichTradeStrategySnapshot: leg skipped strategyId={}, legId={}, contractTicker={}, contractId={}, reason=snapshot-outside-interval, targetUnixTime={}, pickedUnixTime={}, allowedSeconds={}, distanceEpoch={}, allowedEpochDistance={}",
                    strategyId,
                    leg.id(),
                    leg.contractTicker(),
                    leg.optionContractId(),
                    targetUnixTime,
                    optionSnapshot.getUnixTime(),
                    intervalSeconds,
                    unixDistance,
                    maxDistanceInEpochUnit);
                resolvedLegs.clear();
                break;
            }

            BigDecimal marketPrice = resolveMarketPrice(optionSnapshot, null);
            if (marketPrice == null) {
                log.info("enrichTradeStrategySnapshot: leg skipped strategyId={}, legId={}, contractTicker={}, contractId={}, reason=no-market-price, snapshotId={}, snapshotUnixTime={}",
                    strategyId,
                    leg.id(),
                    leg.contractTicker(),
                    leg.optionContractId(),
                    optionSnapshot.getId(),
                    optionSnapshot.getUnixTime());
                resolvedLegs.clear();
                break;
            }

            ResolvedLegSnapshot resolved = new ResolvedLegSnapshot(
                leg,
                optionSnapshot.getId(),
                null,
                marketPrice,
                optionSnapshot);
            resolvedLegs.add(resolved);
            log.info("enrichTradeStrategySnapshot: leg picked strategyId={}, legId={}, contractTicker={}, contractId={}, snapshotId={}, snapshotUnixTime={}, targetUnixTime={}, marketPrice={}",
                strategyId,
                leg.id(),
                leg.contractTicker(),
                leg.optionContractId(),
                optionSnapshot.getId(),
                optionSnapshot.getUnixTime(),
                targetUnixTime,
                marketPrice);
        }
        return resolvedLegs;
    }

    // Aggregates strategy-level valuation and greek metrics from resolved leg snapshots.
    private StrategySnapshotMetrics calculateStrategySnapshotMetrics(List<ResolvedLegSnapshot> resolvedLegs) {
        BigDecimal entryCost = BigDecimal.ZERO;
        BigDecimal currentMarketValue = BigDecimal.ZERO;
        BigDecimal netDelta = BigDecimal.ZERO;
        BigDecimal netGamma = BigDecimal.ZERO;
        BigDecimal netTheta = BigDecimal.ZERO;
        BigDecimal netVega = BigDecimal.ZERO;
        BigDecimal ivSum = BigDecimal.ZERO;
        int ivCount = 0;
        long totalOpenInterest = 0;
        long totalDayVolume = 0;

        List<BigDecimal> underlyingPrices = resolvedLegs.stream()
            .map(ResolvedLegSnapshot::optionSnapshot)
            .filter(Objects::nonNull)
            .map(OptionSnapshot::getUnderlyingPrice)
            .filter(Objects::nonNull)
            .toList();

        if (underlyingPrices.isEmpty()) {
            return null;
        }

        for (ResolvedLegSnapshot resolved : resolvedLegs) {
            OptionStrategyRepository.StrategyLegSummaryRow leg = resolved.leg();
            BigDecimal quantityMultiplier = BigDecimal.valueOf((long) leg.quantity()).multiply(HUNDRED);
            BigDecimal sign = "SHORT".equalsIgnoreCase(leg.positionSide()) ? BigDecimal.valueOf(-1) : BigDecimal.ONE;

            BigDecimal currentLegValue = resolved.marketPrice().multiply(quantityMultiplier).multiply(sign).setScale(4, RoundingMode.HALF_UP);
            BigDecimal entryPrice = leg.entryPrice() != null ? leg.entryPrice() : resolved.marketPrice();
            BigDecimal entryLegValue = entryPrice.multiply(quantityMultiplier).multiply(sign).setScale(4, RoundingMode.HALF_UP);

            entryCost = entryCost.add(entryLegValue);
            currentMarketValue = currentMarketValue.add(currentLegValue);

            OptionSnapshot snapshot = resolved.optionSnapshot();
            if (snapshot != null) {
                netDelta = netDelta.add(sign.multiply(nullSafe(snapshot.getDelta()).multiply(quantityMultiplier)));
                netGamma = netGamma.add(sign.multiply(nullSafe(snapshot.getGamma()).multiply(quantityMultiplier)));
                netTheta = netTheta.add(sign.multiply(nullSafe(snapshot.getTheta()).multiply(quantityMultiplier)));
                netVega = netVega.add(sign.multiply(nullSafe(snapshot.getVega()).multiply(quantityMultiplier)));

                if (snapshot.getImpliedVolatility() != null) {
                    ivSum = ivSum.add(snapshot.getImpliedVolatility());
                    ivCount++;
                }

                totalOpenInterest += snapshot.getOpenInterest() == null ? 0 : snapshot.getOpenInterest();
                totalDayVolume += snapshot.getDayVolume() == null ? 0 : snapshot.getDayVolume();
            }
        }

        BigDecimal unrealizedPnl = currentMarketValue.subtract(entryCost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal unrealizedPnlPct = entryCost.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
            : unrealizedPnl.divide(entryCost.abs(), 8, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal averageIv = ivCount == 0 ? null : ivSum.divide(BigDecimal.valueOf(ivCount), 6, RoundingMode.HALF_UP);
        BigDecimal underlyingPrice = averageNonNull(underlyingPrices);

        return new StrategySnapshotMetrics(
            underlyingPrice.setScale(4, RoundingMode.HALF_UP),
            entryCost.setScale(4, RoundingMode.HALF_UP),
            currentMarketValue.setScale(4, RoundingMode.HALF_UP),
            unrealizedPnl,
            unrealizedPnlPct,
            netDelta.setScale(8, RoundingMode.HALF_UP),
            netGamma.setScale(8, RoundingMode.HALF_UP),
            netTheta.setScale(8, RoundingMode.HALF_UP),
            netVega.setScale(8, RoundingMode.HALF_UP),
            averageIv,
            totalOpenInterest,
            totalDayVolume);
    }

    // Inserts leg-level snapshots and returns the total inserted count.
    private int insertLegSnapshots(Long optionStrategySnapshotId,
                                   List<ResolvedLegSnapshot> resolvedLegs) {
        int inserted = 0;
        for (ResolvedLegSnapshot resolved : resolvedLegs) {
            OptionStrategyRepository.StrategyLegSummaryRow leg = resolved.leg();
            BigDecimal quantityMultiplier = BigDecimal.valueOf((long) leg.quantity()).multiply(HUNDRED);
            BigDecimal sign = "SHORT".equalsIgnoreCase(leg.positionSide()) ? BigDecimal.valueOf(-1) : BigDecimal.ONE;

            BigDecimal currentLegValue = resolved.marketPrice().multiply(quantityMultiplier).multiply(sign).setScale(4, RoundingMode.HALF_UP);
            BigDecimal entryPrice = leg.entryPrice() != null ? leg.entryPrice() : resolved.marketPrice();
            BigDecimal entryLegValue = entryPrice.multiply(quantityMultiplier).multiply(sign).setScale(4, RoundingMode.HALF_UP);

            BigDecimal legUnrealizedPnl = currentLegValue.subtract(entryLegValue).setScale(4, RoundingMode.HALF_UP);
            BigDecimal legUnrealizedPnlPct = entryLegValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : legUnrealizedPnl.divide(entryLegValue.abs(), 8, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(4, RoundingMode.HALF_UP);

            inserted += optionStrategyRepository.insertStrategyLegSnapshot(
                optionStrategySnapshotId,
                leg.id(),
                resolved.optionSnapshotId(),
                resolved.optionFlatFileId(),
                currentLegValue,
                legUnrealizedPnl,
                legUnrealizedPnlPct);
        }
        return inserted;
    }

    private record StrategySnapshotMetrics(BigDecimal underlyingPrice,
                                           BigDecimal entryCost,
                                           BigDecimal currentMarketValue,
                                           BigDecimal unrealizedPnl,
                                           BigDecimal unrealizedPnlPct,
                                           BigDecimal netDelta,
                                           BigDecimal netGamma,
                                           BigDecimal netTheta,
                                           BigDecimal netVega,
                                           BigDecimal averageIv,
                                           Long totalOpenInterest,
                                           Long totalDayVolume) {
    }
}
