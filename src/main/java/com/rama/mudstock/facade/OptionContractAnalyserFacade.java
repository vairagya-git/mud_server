package com.rama.mudstock.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionToAnalyseRepository;
import com.rama.mudstock.service.MassiveRestOptionSnapshotService;
import com.rama.mudstock.service.OptionSnapshotParser;

@Service
public class OptionContractAnalyserFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionContractAnalyserFacade.class);

    private final OptionToAnalyseRepository optionToAnalyseRepository;
    private final MassiveRestOptionSnapshotService massiveOptionSnapshotService;
    private final OptionSnapshotParser optionSnapshotParser;
    private final OptionContractRepository optionContractRepository;

    public OptionContractAnalyserFacade(OptionToAnalyseRepository optionToAnalyseRepository,
                                        MassiveRestOptionSnapshotService massiveOptionSnapshotService,
                                        OptionSnapshotParser optionSnapshotParser,
                                        OptionContractRepository optionContractRepository) {
        this.optionToAnalyseRepository = optionToAnalyseRepository;
        this.massiveOptionSnapshotService = massiveOptionSnapshotService;
        this.optionSnapshotParser = optionSnapshotParser;
        this.optionContractRepository = optionContractRepository;
    }

    public int analyseDaily() {
        List<Map<String, Object>> entries = optionToAnalyseRepository.listActiveWithTicker();
        int processedContracts = 0;

        for (Map<String, Object> entry : entries) {
            try {
                processedContracts += processEntry(entry);
            } catch (Exception ex) {
                log.error("OptionContractAnalyserFacade: failed processing option_to_analyse entry {}", entry, ex);
            }
        }

        return processedContracts;
    }

    private int processEntry(Map<String, Object> entry) throws Exception {
        Long stockId = toLong(entry.get("stock_id"));
        String ticker = toString(entry.get("ticker"));
        String requestedContractType = toString(entry.get("contract_type"));
        LocalDate expirationDate = toLocalDate(entry.get("expiration_date"));
        BigDecimal strikeFrom = toBigDecimal(entry.get("strike_from"));
        BigDecimal strikeTo = toBigDecimal(entry.get("strike_to"));
        BigDecimal interval = toBigDecimal(entry.get("interval"));

        if (stockId == null || ticker == null || requestedContractType == null || expirationDate == null
            || strikeFrom == null || strikeTo == null || interval == null) {
            log.warn("OptionContractAnalyserFacade: skipping incomplete option_to_analyse entry {}", entry);
            return 0;
        }

        if (interval.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("OptionContractAnalyserFacade: skipping entry with non-positive interval {}", entry);
            return 0;
        }

        int processed = 0;
        for (BigDecimal strikePrice = strikeFrom;
             strikePrice.compareTo(strikeTo) <= 0;
             strikePrice = strikePrice.add(interval)) {
            String responseBody = massiveOptionSnapshotService.fetchOptionSnapshot(
                ticker,
                strikePrice.stripTrailingZeros().toPlainString(),
                expirationDate.toString());

            List<OptionSnapshotParser.OptionContractData> contracts = optionSnapshotParser.parseContracts(responseBody);
            for (OptionSnapshotParser.OptionContractData contract : contracts) {
                if (!shouldPersist(requestedContractType, contract.contractType())) {
                    continue;
                }

                if (optionContractRepository.existsByUniqueKey(
                    stockId,
                    contract.contractType(),
                    contract.expirationDate(),
                    contract.contractTicker())) {
                    continue;
                }

                optionContractRepository.upsert(
                    stockId,
                    contract.contractType(),
                    contract.exerciseStyle(),
                    contract.expirationDate(),
                    contract.strikePrice(),
                    contract.sharesPerContract(),
                    contract.contractTicker());
                processed++;
            }
        }

        return processed;
    }

    private boolean shouldPersist(String requestedContractType, String contractType) {
        String normalizedRequested = requestedContractType == null ? "" : requestedContractType.trim().toUpperCase();
        String normalizedActual = contractType == null ? "" : contractType.trim().toUpperCase();
        if ("BOTH".equals(normalizedRequested)) {
            return "CALL".equals(normalizedActual) || "PUT".equals(normalizedActual);
        }
        return normalizedRequested.equals(normalizedActual);
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}