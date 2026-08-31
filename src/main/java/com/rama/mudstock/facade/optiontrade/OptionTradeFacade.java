package com.rama.mudstock.facade.optiontrade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.repository.option.OptionStrategyRepository;
import com.rama.mudstock.repository.option.OptionTradeRepository;
import com.rama.mudstock.model.option.OptionContract;
import com.rama.mudstock.model.option.OptionSnapshot;
import com.rama.mudstock.service.app.ApplicationFilterService;

@Service
public class OptionTradeFacade {

    private static final DateTimeFormatter OPTION_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd:MM:yyyy HH:mm");
    private static final int SNAPSHOT_ROWS_LIMIT = 20;

    private final ApplicationFilterService applicationFilterService;
    private final OptionStrategyRepository optionStrategyRepository;
    private final OptionContractRepository optionContractRepository;
    private final OptionSnapshotRepository optionSnapshotRepository;
    private final OptionTradeRepository optionTradeRepository;

    public OptionTradeFacade(ApplicationFilterService applicationFilterService,
                            OptionStrategyRepository optionStrategyRepository,
                            OptionContractRepository optionContractRepository,
                            OptionSnapshotRepository optionSnapshotRepository,
                            OptionTradeRepository optionTradeRepository) {
        this.applicationFilterService = applicationFilterService;
        this.optionStrategyRepository = optionStrategyRepository;
        this.optionContractRepository = optionContractRepository;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.optionTradeRepository = optionTradeRepository;
    }

    public OptionTradeFilterData loadFilterData(String contractStatus) {
        ApplicationFilterService.OptionTradeFilters filters = applicationFilterService.optionTradeFilters(contractStatus);

        return new OptionTradeFilterData(
            filters.strategyDefinitions(),
            filters.strategyDefinitionLegs(),
            filters.tickers(),
            filters.expirationDates(),
            filters.contracts());
    }

    public List<OpenTradeOption> listOpenTradeOptions(Long stockId) {
        if (stockId == null) {
            return List.of();
        }

        List<OptionTradeRepository.OpenTradeRow> rows = optionTradeRepository.listOpenTradesByStockId(stockId);
        return rows.stream()
            .map(row -> new OpenTradeOption(
                row.id(),
                Objects.toString(row.tradeName(), ""),
                Objects.toString(row.tradeMode(), "")))
            .toList();
    }

    public List<OpenTradeOption> listOpenTradeOptionsByTicker(String ticker) {
        String normalizedTicker = ticker == null ? "" : ticker.trim().toUpperCase();
        if (normalizedTicker.isBlank()) {
            return List.of();
        }

        Long stockId = optionContractRepository.getOptionContractExpirationForStock(normalizedTicker)
            .stream()
            .map(OptionContract::getStockId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        return listOpenTradeOptions(stockId);
    }

    public List<ContractOption> listContractsForTickerAndExpiration(String ticker,
                                                                    LocalDate expirationDate) {
        String normalizedTicker = ticker == null ? "" : ticker.trim().toUpperCase();
        if (normalizedTicker.isBlank() || expirationDate == null) {
            return List.of();
        }

        Long stockId = optionContractRepository.getOptionContractExpirationForStock(normalizedTicker)
            .stream()
            .map(OptionContract::getStockId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (stockId == null) {
            return List.of();
        }

        List<OptionContract> strikeRows = optionContractRepository
            .getOptionContractStrikeForStockAndExpiration(stockId, expirationDate);
        if (strikeRows.isEmpty()) {
            return List.of();
        }

        List<ContractOption> options = new ArrayList<>();
        for (OptionContract strikeRow : strikeRows) {
            BigDecimal strikePrice = strikeRow.getStrikePrice();
            if (strikePrice == null) {
                continue;
            }

            List<OptionContract> contracts = optionContractRepository
                .getOptionContractsForStockExpirationStrike(stockId, expirationDate, strikePrice);
            for (OptionContract contract : contracts) {
                options.add(new ContractOption(
                    stockId,
                    normalizedTicker,
                    Objects.toString(contract.getContractTicker(), ""),
                    Objects.toString(contract.getContractType(), ""),
                    expirationDate,
                    strikePrice));
            }
        }

        return options;
    }

    public OpenTradeOption createOpenTrade(Long stockId,
                                           String tradeName,
                                           boolean withHistoricData) {
        if (stockId == null) {
            throw new IllegalArgumentException("stock_id is required");
        }

        String normalizedTradeName = tradeName == null ? "" : tradeName.trim();
        if (normalizedTradeName.isBlank()) {
            throw new IllegalArgumentException("trade_name is required");
        }
        String resolvedTradeMode = OptionTradeRepository.TradeMode.LIVE.name();

        Long tradeId = optionTradeRepository.insertOpenTrade(stockId, normalizedTradeName, resolvedTradeMode, withHistoricData, LocalDateTime.now());
        return new OpenTradeOption(tradeId, normalizedTradeName, resolvedTradeMode);
    }

    public List<SnapshotQuoteOption> listSnapshotQuoteOptions(String contractTicker) {
        String normalizedContractTicker = contractTicker == null ? "" : contractTicker.trim().toUpperCase();
        if (normalizedContractTicker.isBlank()) {
            return List.of();
        }

        OptionContract selectedContract = optionContractRepository.listContractsByTickers(List.of(normalizedContractTicker))
            .stream()
            .filter(contract -> normalizedContractTicker.equals(Objects.toString(contract.getContractTicker(), "").trim().toUpperCase()))
            .findFirst()
            .orElse(null);

        if (selectedContract == null || selectedContract.getId() == null) {
            return List.of();
        }

        return optionSnapshotRepository.listOptionSnapshotsByContractId(selectedContract.getId())
            .stream()
            .filter(Objects::nonNull)
            .limit(SNAPSHOT_ROWS_LIMIT)
            .map(this::toSnapshotQuoteOption)
            .toList();
    }

    public TradeSummaryData loadTradeSummaryData() {
        List<OptionTradeRepository.TradeRow> aliveTrades = optionTradeRepository
            .listOpenTradesByModeWithTicker(OptionTradeRepository.Status.OPEN);
        if (aliveTrades.isEmpty()) {
            return new TradeSummaryData(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<Long> tradeIds = aliveTrades.stream()
            .map(OptionTradeRepository.TradeRow::id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        List<OptionStrategyRepository.StrategySummaryRow> optionStrategies = optionStrategyRepository.listStrategiesByTradeIds(tradeIds);
        List<Long> optionStrategyIds = optionStrategies.stream()
            .map(OptionStrategyRepository.StrategySummaryRow::id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        List<OptionStrategyRepository.StrategyLegSummaryRow> optionStrategyLegs = optionStrategyIds.isEmpty()
            ? List.of()
            : optionStrategyRepository.listStrategyLegsByStrategyIds(optionStrategyIds);
        List<OptionStrategyRepository.StrategySnapshotSummaryRow> optionStrategySnapshots = optionStrategyIds.isEmpty()
            ? List.of()
            : optionStrategyRepository.listStrategySnapshotsByStrategyIds(optionStrategyIds);

        List<Long> optionStrategySnapshotIds = optionStrategySnapshots.stream()
            .map(OptionStrategyRepository.StrategySnapshotSummaryRow::id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        List<OptionStrategyRepository.StrategyLegSnapshotSummaryRow> optionStrategySnapshotLegs = optionStrategySnapshotIds.isEmpty()
            ? List.of()
            : optionStrategyRepository.listStrategyLegSnapshotsBySnapshotIds(optionStrategySnapshotIds);

        return new TradeSummaryData(
            aliveTrades,
            optionStrategies,
            optionStrategyLegs,
            optionStrategySnapshots,
            optionStrategySnapshotLegs);
    }

    @Transactional
    public StrategySnapshotCreateResult createStrategySnapshot(Long strategyDefinitionId,
                                                               Long stockId,
                                                               Long optionTradeId,
                                                               String optionTime,
                                                               List<SelectedLegInput> selectedLegs) {
        if (strategyDefinitionId == null) {
            throw new IllegalArgumentException("strategy_definition_id is required");
        }
        if (stockId == null) {
            throw new IllegalArgumentException("stock_id is required");
        }
        if (optionTradeId == null) {
            throw new IllegalArgumentException("option_trade_id is required");
        }
        if (selectedLegs == null || selectedLegs.isEmpty()) {
            throw new IllegalArgumentException("selected legs are required");
        }

        Timestamp requestedOptionTime = parseOptionTime(optionTime);

        String tradeMode = optionTradeRepository.findOpenTradeModeByIdAndStockId(optionTradeId, stockId);
        if (tradeMode == null) {
            throw new IllegalArgumentException("Selected trade is not OPEN for this stock");
        }
        tradeMode = tradeMode.trim().toUpperCase();

        List<OptionStrategyRepository.StrategyDefinitionLegRow> strategyLegDefinitions = optionStrategyRepository
            .listActiveStrategyDefinitionLegsByDefinitionId(strategyDefinitionId);
        if (strategyLegDefinitions.isEmpty()) {
            throw new IllegalArgumentException("No active strategy legs found for selected strategy definition");
        }

        Map<Long, SelectedLegInput> selectedLegMap = selectedLegs.stream()
            .filter(Objects::nonNull)
            .filter(leg -> leg.optionStrategyDefinitionLegId() != null && leg.contractTicker() != null && !leg.contractTicker().isBlank())
            .collect(Collectors.toMap(
                SelectedLegInput::optionStrategyDefinitionLegId,
                leg -> new SelectedLegInput(leg.optionStrategyDefinitionLegId(), leg.contractTicker().trim().toUpperCase()),
                (left, right) -> right,
                LinkedHashMap::new));

        if (selectedLegMap.isEmpty()) {
            throw new IllegalArgumentException("At least one leg contract must be selected");
        }

        List<String> requestedContractTickers = selectedLegMap.values().stream()
            .map(SelectedLegInput::contractTicker)
            .distinct()
            .toList();

        Map<String, OptionContract> contractsByTicker = optionContractRepository
            .listContractsByTickers(requestedContractTickers)
            .stream()
            .collect(Collectors.toMap(
            row -> Objects.toString(row.getContractTicker(), "").trim().toUpperCase(),
            row -> row,
                (left, right) -> left,
                LinkedHashMap::new));

        List<ResolvedLeg> resolvedLegs = new ArrayList<>();

        for (OptionStrategyRepository.StrategyDefinitionLegRow legDefinition : strategyLegDefinitions) {
            Long strategyDefinitionLegId = legDefinition.id();
            if (strategyDefinitionLegId == null) {
                continue;
            }

            SelectedLegInput selectedLeg = selectedLegMap.get(strategyDefinitionLegId);
            if (selectedLeg == null) {
                throw new IllegalArgumentException("Contract selection missing for strategy definition leg id " + strategyDefinitionLegId);
            }

            OptionContract contractRow = contractsByTicker.get(selectedLeg.contractTicker());
            if (contractRow == null) {
                throw new IllegalArgumentException("Selected contract not found: " + selectedLeg.contractTicker());
            }

            Long contractStockId = contractRow.getStockId();
            if (!stockId.equals(contractStockId)) {
                throw new IllegalArgumentException("Selected contract does not belong to selected ticker stock");
            }

            String expectedContractType = Objects.toString(legDefinition.contractType(), "").trim().toUpperCase();
            String actualContractType = Objects.toString(contractRow.getContractType(), "").trim().toUpperCase();
            if (!expectedContractType.isBlank() && !expectedContractType.equals(actualContractType)) {
                throw new IllegalArgumentException("Strategy definition leg id " + strategyDefinitionLegId + " requires " + expectedContractType + " contract");
            }

            Long optionContractId = contractRow.getId();
            OptionSnapshot nearestSnapshot = optionSnapshotRepository
                .findNearestSnapshotByContractAndOptionTime(optionContractId, requestedOptionTime);
            if (nearestSnapshot == null) {
                throw new IllegalArgumentException("No option_snapshot found near Option Time for contract " + selectedLeg.contractTicker());
            }

            BigDecimal entryPrice = resolveEntryPrice(nearestSnapshot);
            if (entryPrice == null) {
                throw new IllegalArgumentException("No usable price found in option_snapshot for contract " + selectedLeg.contractTicker());
            }

            Integer quantity = legDefinition.quantity();
            if (quantity == null || quantity <= 0) {
                quantity = 1;
            }

            Integer sharesPerContract = contractRow.getSharesPerContract();
            if (sharesPerContract == null || sharesPerContract <= 0) {
                sharesPerContract = 100;
            }

            String positionSide = Objects.toString(legDefinition.positionSide(), "LONG").trim().toUpperCase();
            Long optionSnapshotId = nearestSnapshot.getId();

            resolvedLegs.add(new ResolvedLeg(
                legDefinition.legOrder(),
                optionContractId,
                optionSnapshotId,
                positionSide,
                quantity,
                sharesPerContract,
                entryPrice,
                nearestSnapshot));
        }

        resolvedLegs.sort(Comparator.comparing(ResolvedLeg::strategyLegNumber));

        BigDecimal underlyingPrice = averageNonNull(resolvedLegs.stream()
            .map(leg -> leg.snapshot().getUnderlyingPrice())
            .toList());
        if (underlyingPrice == null) {
            throw new IllegalArgumentException("No underlying price available from nearest snapshots");
        }

        BigDecimal entryCost = BigDecimal.ZERO;
        BigDecimal currentMarketValue = BigDecimal.ZERO;
        BigDecimal netDelta = BigDecimal.ZERO;
        BigDecimal netGamma = BigDecimal.ZERO;
        BigDecimal netTheta = BigDecimal.ZERO;
        BigDecimal netVega = BigDecimal.ZERO;
        BigDecimal ivSum = BigDecimal.ZERO;
        int ivCount = 0;
        long totalOpenInterest = 0L;
        long totalDayVolume = 0L;

        for (ResolvedLeg leg : resolvedLegs) {
            BigDecimal multiplier = BigDecimal.valueOf((long) leg.quantity() * leg.sharesPerContract());
            BigDecimal marketAbs = leg.entryPrice().multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
            BigDecimal signedMarketValue = "SHORT".equals(leg.positionSide()) ? marketAbs.negate() : marketAbs;

            entryCost = entryCost.add(signedMarketValue);
            currentMarketValue = currentMarketValue.add(signedMarketValue);

            BigDecimal sign = "SHORT".equals(leg.positionSide()) ? BigDecimal.valueOf(-1) : BigDecimal.ONE;
            netDelta = netDelta.add(sign.multiply(nullSafe(leg.snapshot().getDelta()).multiply(multiplier)));
            netGamma = netGamma.add(sign.multiply(nullSafe(leg.snapshot().getGamma()).multiply(multiplier)));
            netTheta = netTheta.add(sign.multiply(nullSafe(leg.snapshot().getTheta()).multiply(multiplier)));
            netVega = netVega.add(sign.multiply(nullSafe(leg.snapshot().getVega()).multiply(multiplier)));

            BigDecimal iv = leg.snapshot().getImpliedVolatility();
            if (iv != null) {
                ivSum = ivSum.add(iv);
                ivCount++;
            }

            Integer openInterest = leg.snapshot().getOpenInterest();
            Integer dayVolume = leg.snapshot().getDayVolume();
            totalOpenInterest += (openInterest == null ? 0 : openInterest);
            totalDayVolume += (dayVolume == null ? 0 : dayVolume);
        }

        BigDecimal averageIv = ivCount == 0 ? null : ivSum.divide(BigDecimal.valueOf(ivCount), 6, RoundingMode.HALF_UP);
        BigDecimal unrealizedPnl = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal unrealizedPnlPct = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal realizedPnl = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalPnl = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        Long optionStrategyId = optionStrategyRepository.insertStrategyAndReturnId(
            strategyDefinitionId,
            stockId,
            optionTradeId,
            OptionStrategyRepository.Type.NEW.name(),
            OptionStrategyRepository.Status.OPEN.name(),
            requestedOptionTime,
            underlyingPrice.setScale(6, RoundingMode.HALF_UP));

        if (optionStrategyId == null) {
            throw new IllegalArgumentException("Failed to create option_strategy");
        }

        List<InsertedLeg> insertedLegs = new ArrayList<>();
        for (ResolvedLeg leg : resolvedLegs) {
            Long optionStrategyLegId = optionStrategyRepository.insertStrategyLegAndReturnId(
                optionStrategyId,
                leg.optionContractId(),
                leg.strategyLegNumber(),
                leg.positionSide(),
                leg.quantity(),
                leg.optionSnapshotId(),
                leg.entryPrice().setScale(4, RoundingMode.HALF_UP));

            insertedLegs.add(new InsertedLeg(optionStrategyLegId, leg));
        }

        Long optionStrategySnapshotId = optionStrategyRepository.insertStrategySnapshotAndReturnId(
            optionStrategyId,
            requestedOptionTime,
            underlyingPrice.setScale(4, RoundingMode.HALF_UP),
            entryCost.setScale(4, RoundingMode.HALF_UP),
            currentMarketValue.setScale(4, RoundingMode.HALF_UP),
            unrealizedPnl,
            unrealizedPnlPct,
            realizedPnl,
            totalPnl,
            netDelta.setScale(8, RoundingMode.HALF_UP),
            netGamma.setScale(8, RoundingMode.HALF_UP),
            netTheta.setScale(8, RoundingMode.HALF_UP),
            netVega.setScale(8, RoundingMode.HALF_UP),
            averageIv,
            totalOpenInterest,
            totalDayVolume);

        for (InsertedLeg insertedLeg : insertedLegs) {
            ResolvedLeg leg = insertedLeg.resolvedLeg();
            BigDecimal multiplier = BigDecimal.valueOf((long) leg.quantity() * leg.sharesPerContract());
            BigDecimal marketAbs = leg.entryPrice().multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
            BigDecimal signedMarketValue = "SHORT".equals(leg.positionSide()) ? marketAbs.negate() : marketAbs;

            optionStrategyRepository.insertStrategyLegSnapshot(
                optionStrategySnapshotId,
                insertedLeg.optionStrategyLegId(),
                leg.optionSnapshotId(),
                null,
                signedMarketValue,
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        }

        return new StrategySnapshotCreateResult(
            optionStrategyId,
            optionStrategySnapshotId,
            requestedOptionTime.toLocalDateTime().format(OPTION_TIME_FORMATTER));
    }

    private Timestamp parseOptionTime(String optionTime) {
        if (optionTime == null || optionTime.isBlank()) {
            throw new IllegalArgumentException("Option Time is required in format DD:MM:YYYY HH:MM");
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(optionTime.trim(), OPTION_TIME_FORMATTER);
            return Timestamp.valueOf(parsed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid Option Time format. Expected DD:MM:YYYY HH:MM");
        }
    }

    private BigDecimal resolveEntryPrice(OptionSnapshot snapshot) {
        BigDecimal midpoint = snapshot.getMidpoint();
        if (midpoint != null) {
            return midpoint;
        }

        BigDecimal lastTradePrice = snapshot.getLastTradePrice();
        if (lastTradePrice != null) {
            return lastTradePrice;
        }

        BigDecimal bid = snapshot.getBid();
        BigDecimal ask = snapshot.getAsk();
        if (bid != null && ask != null) {
            return bid.add(ask).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        }

        return null;
    }

    private BigDecimal averageNonNull(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }

        BigDecimal sum = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private SnapshotQuoteOption toSnapshotQuoteOption(OptionSnapshot snapshot) {
        Timestamp quoteTime = snapshot.getOptionQuoteTime() != null ? snapshot.getOptionQuoteTime() : snapshot.getSnapshotTime();
        String optionQuoteTime = quoteTime == null
            ? ""
            : quoteTime.toLocalDateTime().format(OPTION_TIME_FORMATTER);

        BigDecimal bid = snapshot.getBid();
        BigDecimal ask = snapshot.getAsk();
        BigDecimal midpoint = snapshot.getMidpoint();
        if (midpoint == null && bid != null && ask != null) {
            midpoint = bid.add(ask).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        }

        String bidText = formatPrice2(bid);
        String askText = formatPrice2(ask);
        String midpointText = formatPrice2(midpoint);

        String displayValue = optionQuoteTime
            + " B" + bidText
            + "-A" + askText
            + "-M" + midpointText;

        return new SnapshotQuoteOption(optionQuoteTime, bidText, askText, midpointText, displayValue);
    }

    private String formatPrice2(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public record OptionTradeFilterData(List<Map<String, Object>> strategyDefinitions,
                                        List<Map<String, Object>> strategyDefinitionLegs,
                                        List<String> tickers,
                                        List<LocalDate> expirationDates,
                                        Object contracts) {
    }

    public record ContractOption(Long stockId,
                                 String ticker,
                                 String contractTicker,
                                 String contractType,
                                 LocalDate expirationDate,
                                 BigDecimal strikePrice) {
    }

    public record OpenTradeOption(Long id,
                                  String tradeName,
                                  String tradeMode) {
    }

    public record SelectedLegInput(Long optionStrategyDefinitionLegId,
                                   String contractTicker) {
    }

    public record StrategySnapshotCreateResult(Long optionStrategyId,
                                               Long optionStrategySnapshotId,
                                               String resolvedOptionTime) {
    }

    public record SnapshotQuoteOption(String optionQuoteTime,
                                      String bid,
                                      String ask,
                                      String midpoint,
                                      String displayValue) {
    }

    public record TradeSummaryData(List<OptionTradeRepository.TradeRow> aliveTrades,
                                   List<OptionStrategyRepository.StrategySummaryRow> optionStrategies,
                                   List<OptionStrategyRepository.StrategyLegSummaryRow> optionStrategyLegs,
                                   List<OptionStrategyRepository.StrategySnapshotSummaryRow> optionStrategySnapshots,
                                   List<OptionStrategyRepository.StrategyLegSnapshotSummaryRow> optionStrategySnapshotLegs) {
    }

    private record ResolvedLeg(Integer strategyLegNumber,
                               Long optionContractId,
                               Long optionSnapshotId,
                               String positionSide,
                               Integer quantity,
                               Integer sharesPerContract,
                               BigDecimal entryPrice,
                               OptionSnapshot snapshot) {
    }

    private record InsertedLeg(Long optionStrategyLegId,
                               ResolvedLeg resolvedLeg) {
    }
}
