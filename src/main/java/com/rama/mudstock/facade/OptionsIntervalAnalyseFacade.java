package com.rama.mudstock.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionContractStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionIntervalAnalyseStatusEnum;
import com.rama.mudstock.model.option.OptionsInternalAnalyseEntity;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionIntervalAnalyseRepository;
import com.rama.mudstock.service.MassiveRestOptionSnapshotService;
import com.rama.mudstock.service.OptionDataContractService;
import com.rama.mudstock.service.OptionSnapshotParser;

/**
 * Processes options_interval_analyse entries in CREATE_CONTRACT state,
 * creates contracts from Massive snapshots, marks interval entry ACTIVE once created,
 * and marks matching option contracts COMPLETED when interval status is CLOSE.
 */
@Service
public class OptionsIntervalAnalyseFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionsIntervalAnalyseFacade.class);

    private final OptionIntervalAnalyseRepository optionToAnalyseRepository;
    private final MassiveRestOptionSnapshotService massiveOptionSnapshotService;
    private final OptionSnapshotParser optionSnapshotParser;
    private final OptionContractRepository optionContractRepository;
    private final OptionDataContractService optionDataContractService;

    public OptionsIntervalAnalyseFacade(OptionIntervalAnalyseRepository optionToAnalyseRepository,
                                        MassiveRestOptionSnapshotService massiveOptionSnapshotService,
                                        OptionSnapshotParser optionSnapshotParser,
                                        OptionContractRepository optionContractRepository,
                                        OptionDataContractService optionDataContractService) {
        this.optionToAnalyseRepository = optionToAnalyseRepository;
        this.massiveOptionSnapshotService = massiveOptionSnapshotService;
        this.optionSnapshotParser = optionSnapshotParser;
        this.optionContractRepository = optionContractRepository;
        this.optionDataContractService = optionDataContractService;
    }

    public int completeExpiredActiveEntries() {
        return optionDataContractService.completeExpiredActiveEntries(OptionIntervalAnalyseStatusEnum.API_COMPLETED.name());
    }

    public int analyseDaily() {
        List<OptionsInternalAnalyseEntity> entries = optionToAnalyseRepository
            .getOptionsInternalAnalyseByStatus(OptionIntervalAnalyseStatusEnum.CREATE_CONTRACT.name());
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
                        optionToAnalyseRepository.updateStatusById(entryId, OptionIntervalAnalyseStatusEnum.PARTIALLY_COMPLETED.name());
                        log.info("OptionsIntervalAnalyseFacade: entry id={} status updated to {} (processedContracts={})",
                            entryId,
                            OptionIntervalAnalyseStatusEnum.PARTIALLY_COMPLETED.name(),
                            result.processedContracts());
                    } else if (result.createdContracts()) {
                        optionToAnalyseRepository.updateStatusById(entryId, OptionIntervalAnalyseStatusEnum.ACTIVE.name());
                        log.info("OptionsIntervalAnalyseFacade: entry id={} status updated to {} (processedContracts={})",
                            entryId,
                            OptionIntervalAnalyseStatusEnum.ACTIVE.name(),
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
            .getOptionsInternalAnalyseByStatus(OptionIntervalAnalyseStatusEnum.CLOSE.name());
        for (OptionsInternalAnalyseEntity entry : closeEntries) {
            try {
                if (entry.id() != null) {
                    int updatedContracts = optionContractRepository.markContractsStatusForInterval(
                        entry.id(),
                        OptionContractStatusEnum.COMPLETED.name());
                    if (updatedContracts > 0) {
                        log.info("OptionsIntervalAnalyseFacade: marked {} option_contract row(s) COMPLETED for CLOSE entry id={}",
                            updatedContracts,
                            entry.id());
                    }
                }
            } catch (Exception ex) {
                log.error("OptionsIntervalAnalyseFacade: failed completing contracts for CLOSE entry {}", entry, ex);
            }
        }

        return processedContracts;
    }

    public int closeInvalidStrikeIntervals() {
        List<OptionsInternalAnalyseEntity> entries = optionToAnalyseRepository
            .getOptionsInternalAnalyseByStatus(OptionIntervalAnalyseStatusEnum.ACTIVE.name());

        int closedCount = 0;
        for (OptionsInternalAnalyseEntity entry : entries) {
            Long entryId = entry.id();
            BigDecimal strikeFrom = entry.strikeFrom();
            BigDecimal strikeTo = entry.strikeTo();
            if (entryId == null || strikeFrom == null || strikeTo == null) {
                continue;
            }

            if (strikeFrom.compareTo(strikeTo) > 0) {
                optionToAnalyseRepository.updateStatusById(entryId, OptionIntervalAnalyseStatusEnum.CLOSE.name());
                closedCount++;
                log.info("OptionsIntervalAnalyseFacade: entry id={} moved -> CLOSE (strikeFrom={} > strikeTo={})",
                    entryId, strikeFrom, strikeTo);
            }
        }

        return closedCount;
    }

    private EntryProcessingResult processEntry(OptionsInternalAnalyseEntity entry) throws Exception {
        Long stockId = entry.stockId();
        String ticker = entry.ticker();
        String requestedContractType = entry.contractType();
        String source = entry.source();
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
                        source,
                        contract.exerciseStyle(),
                        contract.expirationDate(),
                        contract.strikePrice(),
                        contract.sharesPerContract(),
                        contract.contractTicker(),
                        entry.id());
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