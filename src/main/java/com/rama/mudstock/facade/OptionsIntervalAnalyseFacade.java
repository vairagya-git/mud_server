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

/**
 * Processes options_interval_analyse entries in CREATE_CONTRACT state,
 * creates contracts from Massive snapshots, marks interval entry ACTIVE once created,
 * and marks matching option contracts COMPLETED when interval status is CLOSE.
 */
@Service
public class OptionsIntervalAnalyseFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionsIntervalAnalyseFacade.class);

    private final OptionToAnalyseRepository optionToAnalyseRepository;
    private final MassiveRestOptionSnapshotService massiveOptionSnapshotService;
    private final OptionSnapshotParser optionSnapshotParser;
    private final OptionContractRepository optionContractRepository;

    public OptionsIntervalAnalyseFacade(OptionToAnalyseRepository optionToAnalyseRepository,
                                        MassiveRestOptionSnapshotService massiveOptionSnapshotService,
                                        OptionSnapshotParser optionSnapshotParser,
                                        OptionContractRepository optionContractRepository) {
        this.optionToAnalyseRepository = optionToAnalyseRepository;
        this.massiveOptionSnapshotService = massiveOptionSnapshotService;
        this.optionSnapshotParser = optionSnapshotParser;
        this.optionContractRepository = optionContractRepository;
    }

    public int analyseDaily() {
        List<Map<String, Object>> entries = optionToAnalyseRepository.listCreateContractWithTicker();
        int processedContracts = 0;

        for (Map<String, Object> entry : entries) {
            try {
                EntryProcessingResult result = processEntry(entry);
                processedContracts += result.processedContracts();

                if (result.createdContracts()) {
                    Long entryId = toLong(entry.get("id"));
                    if (entryId != null) {
                        optionToAnalyseRepository.updateStatusById(entryId, OptionToAnalyseRepository.STATUS_ACTIVE);
                    }
                }
            } catch (Exception ex) {
                log.error("OptionsIntervalAnalyseFacade: failed processing options_interval_analyse entry {}", entry, ex);
            }
        }

        List<Map<String, Object>> closeEntries = optionToAnalyseRepository.listCloseWithTicker();
        for (Map<String, Object> entry : closeEntries) {
            try {
                completeContractsForClosedInterval(entry);
            } catch (Exception ex) {
                log.error("OptionsIntervalAnalyseFacade: failed completing contracts for CLOSE entry {}", entry, ex);
            }
        }

        return processedContracts;
    }

    private EntryProcessingResult processEntry(Map<String, Object> entry) throws Exception {
        Long stockId = toLong(entry.get("stock_id"));
        String ticker = toString(entry.get("ticker"));
        String requestedContractType = toString(entry.get("contract_type"));
        LocalDate expirationDate = toLocalDate(entry.get("expiration_date"));
        BigDecimal strikeFrom = toBigDecimal(entry.get("strike_from"));
        BigDecimal strikeTo = toBigDecimal(entry.get("strike_to"));
        BigDecimal interval = toBigDecimal(entry.get("interval"));

        if (stockId == null || ticker == null || requestedContractType == null || expirationDate == null
            || strikeFrom == null || strikeTo == null || interval == null) {
            log.warn("OptionsIntervalAnalyseFacade: skipping incomplete options_interval_analyse entry {}", entry);
            return new EntryProcessingResult(0, false);
        }

        if (interval.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("OptionsIntervalAnalyseFacade: skipping entry with non-positive interval {}", entry);
            return new EntryProcessingResult(0, false);
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

        return new EntryProcessingResult(processed, processed > 0);
    }

    private void completeContractsForClosedInterval(Map<String, Object> entry) {
        Long stockId = toLong(entry.get("stock_id"));
        String contractType = toString(entry.get("contract_type"));
        LocalDate expirationDate = toLocalDate(entry.get("expiration_date"));
        BigDecimal strikeFrom = toBigDecimal(entry.get("strike_from"));
        BigDecimal strikeTo = toBigDecimal(entry.get("strike_to"));

        if (stockId == null || contractType == null || expirationDate == null || strikeFrom == null || strikeTo == null) {
            log.warn("OptionsIntervalAnalyseFacade: skipping CLOSE entry with missing interval fields {}", entry);
            return;
        }

        int completed = optionContractRepository.markContractsCompletedForInterval(
            stockId,
            contractType,
            expirationDate,
            strikeFrom,
            strikeTo);

        if (completed > 0) {
            log.info("OptionsIntervalAnalyseFacade: marked {} option_contract row(s) COMPLETED for close interval id={}",
                completed,
                entry.get("id"));
        }
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

    private record EntryProcessingResult(int processedContracts, boolean createdContracts) {
    }
}