package com.rama.mudstock.facade;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.service.MassiveRestOptionSnapshotService;
import com.rama.mudstock.service.OptionSnapshotParser;
import com.rama.mudstock.util.TypeConverstionUtil;

@Service
public class OptionSnapshotFetcherFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionSnapshotFetcherFacade.class);
    private final OptionContractRepository optionContractRepository;
    private final OptionSnapshotRepository optionSnapshotRepository;
    private final MassiveRestOptionSnapshotService massiveRestOptionSnapshotService;
    private final OptionSnapshotParser optionSnapshotParser;

    public OptionSnapshotFetcherFacade(OptionContractRepository optionContractRepository,
                                       OptionSnapshotRepository optionSnapshotRepository,
                                       MassiveRestOptionSnapshotService massiveRestOptionSnapshotService,
                                       OptionSnapshotParser optionSnapshotParser) {
        this.optionContractRepository = optionContractRepository;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.massiveRestOptionSnapshotService = massiveRestOptionSnapshotService;
        this.optionSnapshotParser = optionSnapshotParser;
    }

    public int fetchAndStoreSnapshots() {
        List<Map<String, Object>> contracts = optionContractRepository.getOptionContractsWithTickerByStatus(
            OptionContractRepository.STATUS_ACTIVE,
            true);
        int inserted = 0;

        for (Map<String, Object> contract : contracts) {
            try {
                inserted += processContract(contract);
            } catch (Exception ex) {
                log.error("OptionSnapshotFetcherFacade: failed processing contract {}", contract, ex);
            }
        }

        return inserted;
    }

    private int processContract(Map<String, Object> contract) throws Exception {
        Long optionContractId = TypeConverstionUtil.toLong(contract.get("id"));
        Long stockId = TypeConverstionUtil.toLong(contract.get("stock_id"));
        String stockTicker = toString(contract.get("ticker"));
        String contractTicker = toString(contract.get("contract_ticker"));
        BigDecimal strikePrice = TypeConverstionUtil.toBigDecimal(contract.get("strike_price"));
        LocalDate expirationDate = TypeConverstionUtil.toLocalDate(contract.get("expiration_date"));

        if (optionContractId == null || stockId == null || stockTicker == null || strikePrice == null || expirationDate == null) {
            log.warn("OptionSnapshotFetcherFacade: skipping incomplete active contract {}", contract);
            return 0;
        }

        String responseBody = massiveRestOptionSnapshotService.fetchOptionSnapshot(
            stockTicker,
            strikePrice.stripTrailingZeros().toPlainString(),
            expirationDate.toString());

        OptionSnapshotParser.OptionSnapshotData snapshot = optionSnapshotParser.parseSnapshotData(responseBody, contractTicker);
        if (snapshot.underlyingPrice() == null) {
            log.warn("OptionSnapshotFetcherFacade: skipping snapshot insert because underlying_price is missing for option_contract_id={}", optionContractId);
            return 0;
        }

        BigDecimal bid = TypeConverstionUtil.round(snapshot.bid(), 2);
        BigDecimal ask = TypeConverstionUtil.round(snapshot.ask(), 2);
        BigDecimal midpoint = TypeConverstionUtil.round(snapshot.midpoint(), 2);
        BigDecimal lastTradePrice = TypeConverstionUtil.round(snapshot.lastTradePrice(), 2);
        BigDecimal impliedVolatility = TypeConverstionUtil.toPercentAndRound(snapshot.impliedVolatility(), 2);
        BigDecimal delta = TypeConverstionUtil.round(snapshot.delta(), 3);
        BigDecimal gamma = TypeConverstionUtil.round(snapshot.gamma(), 3);
        BigDecimal theta = TypeConverstionUtil.round(snapshot.theta(), 3);
        BigDecimal vega = TypeConverstionUtil.round(snapshot.vega(), 3);

        try {
            optionSnapshotRepository.insert(
                optionContractId,
                stockId,
                Timestamp.from(Instant.now()),
                TypeConverstionUtil.toTimestampFromEpochNanos(snapshot.quoteLastUpdated()),
                TypeConverstionUtil.toTimestampFromEpochNanos(snapshot.tradeSipTimestamp()),
                TypeConverstionUtil.toTimestampFromEpochNanos(snapshot.underlyingLastUpdated()),
                snapshot.underlyingPrice(),
                snapshot.breakEvenPrice(),
                snapshot.changeToBreakEven(),
                bid,
                ask,
                midpoint,
                lastTradePrice,
                snapshot.bidSize(),
                snapshot.askSize(),
                snapshot.lastTradeSize(),
                impliedVolatility,
                delta,
                gamma,
                theta,
                vega,
                snapshot.openInterest(),
                snapshot.dayVolume(),
                snapshot.quoteTimeframe(),
                snapshot.underlyingTimeframe());
        } catch (DuplicateKeyException ex) {
            log.info(
                "OptionSnapshotFetcherFacade: skipping duplicate snapshot for option_contract_id={} contract_ticker={} quote_time={}",
                optionContractId,
                contractTicker,
                TypeConverstionUtil.toTimestampFromEpochNanos(snapshot.quoteLastUpdated()));
            return 0;
        }

        return 1;
    }

    private String toString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

}