package com.rama.mudstock.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.rama.mudstock.model.option.OptionsInternalAnalyseEntity;
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
        List<OptionsInternalAnalyseEntity> entries = optionToAnalyseRepository
            .getOptionsInternalAnalyseByStatus(OptionToAnalyseRepository.STATUS_CREATE_CONTRACT);
        int processedContracts = 0;

        for (OptionsInternalAnalyseEntity entry : entries) {
            try {
                log.info("OptionsIntervalAnalyseFacade: processing CREATE_CONTRACT entry id={}, ticker={}, contractType={}, expirationDate={}, strikeFrom={}, strikeTo={}, interval={}",
                    entry.id(),
                    entry.ticker(),
                    entry.contractType(),
                    entry.expirationDate(),
                    entry.strikeFrom(),
                    entry.strikeTo(),
                    entry.interval());
                EntryProcessingResult result = processEntry(entry);
                processedContracts += result.processedContracts();

                Long entryId = entry.id();
                if (entryId != null) {
                    if (result.hadNotFound()) {
                        optionToAnalyseRepository.updateStatusById(entryId, OptionToAnalyseRepository.STATUS_PARTIALLY_COMPLETED);
                        log.info("OptionsIntervalAnalyseFacade: entry id={} status updated to {} (processedContracts={})",
                            entryId,
                            OptionToAnalyseRepository.STATUS_PARTIALLY_COMPLETED,
                            result.processedContracts());
                    } else if (result.createdContracts()) {
                        optionToAnalyseRepository.updateStatusById(entryId, OptionToAnalyseRepository.STATUS_ACTIVE);
                        log.info("OptionsIntervalAnalyseFacade: entry id={} status updated to {} (processedContracts={})",
                            entryId,
                            OptionToAnalyseRepository.STATUS_ACTIVE,
                            result.processedContracts());
                    } else {
                        log.info("OptionsIntervalAnalyseFacade: entry id={} no contracts created and no 404 observed; status unchanged",
                            entryId);
                    }
                }
            } catch (Exception ex) {
                log.error("OptionsIntervalAnalyseFacade: failed processing options_interval_analyse entry {}", entry, ex);
            }
        }

        List<OptionsInternalAnalyseEntity> closeEntries = optionToAnalyseRepository
            .getOptionsInternalAnalyseByStatus(OptionToAnalyseRepository.STATUS_CLOSE);
        for (OptionsInternalAnalyseEntity entry : closeEntries) {
            try {
                completeContractsForClosedInterval(entry);
            } catch (Exception ex) {
                log.error("OptionsIntervalAnalyseFacade: failed completing contracts for CLOSE entry {}", entry, ex);
            }
        }

        return processedContracts;
    }

    private EntryProcessingResult processEntry(OptionsInternalAnalyseEntity entry) throws Exception {
        Long stockId = entry.stockId();
        String ticker = entry.ticker();
        String requestedContractType = entry.contractType();
        LocalDate expirationDate = entry.expirationDate();
        BigDecimal strikeFrom = entry.strikeFrom();
        BigDecimal strikeTo = entry.strikeTo();
        BigDecimal interval = entry.interval();

        if (stockId == null || ticker == null || requestedContractType == null || expirationDate == null
            || strikeFrom == null || strikeTo == null || interval == null) {
            log.warn("OptionsIntervalAnalyseFacade: skipping incomplete options_interval_analyse entry {}", entry);
            return new EntryProcessingResult(0, false, false);
        }

        if (interval.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("OptionsIntervalAnalyseFacade: skipping entry with non-positive interval {}", entry);
            return new EntryProcessingResult(0, false, false);
        }

        int processed = 0;
        boolean hadNotFound = false;
        for (BigDecimal strikePrice = strikeFrom;
             strikePrice.compareTo(strikeTo) <= 0;
             strikePrice = strikePrice.add(interval)) {
            try {
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
            } catch (HttpClientErrorException.NotFound notFound) {
                hadNotFound = true;
                log.warn("OptionsIntervalAnalyseFacade: snapshot 404 for ticker={}, strike={}, expirationDate={}",
                    ticker,
                    strikePrice.stripTrailingZeros().toPlainString(),
                    expirationDate);
            }
        }

        return new EntryProcessingResult(processed, processed > 0, hadNotFound);
    }

    private void completeContractsForClosedInterval(OptionsInternalAnalyseEntity entry) {
        Long stockId = entry.stockId();
        String contractType = entry.contractType();
        LocalDate expirationDate = entry.expirationDate();
        BigDecimal strikeFrom = entry.strikeFrom();
        BigDecimal strikeTo = entry.strikeTo();

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
                entry.id());
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

    private record EntryProcessingResult(int processedContracts, boolean createdContracts, boolean hadNotFound) {
    }
}